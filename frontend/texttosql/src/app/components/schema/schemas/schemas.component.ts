import { Component, inject, signal, WritableSignal, computed, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Api } from '../../../api/api';
import { AuthService } from '../../../core/auth.service';

// Models
import { DatabaseDto } from '../../../api/models/database-dto';
import { TableDto } from '../../../api/models/table-dto';
import { ColumnDto } from '../../../api/models/column-dto';

// Functions
import {
  getDatabases, getTables, getColumns, getDatabase, // getDatabase eklendi
  deleteDatabase, deleteTable, deleteColumn,
  updateDatabase, updateTable, updateColumn,
  createDatabase, createTable, createColumn,
  importSchema
} from '../../../api/functions';

// Search Import
import { searchSchema } from '../../../api/fn/4-search/search-schema';

type ModalMode = 'IMPORT' | 'CREATE_DB' | 'EDIT_DB' | 'CREATE_TABLE' | 'EDIT_TABLE' | 'CREATE_COLUMN' | 'EDIT_COLUMN';
type DeleteType = 'DB' | 'TABLE' | 'COLUMN';

interface SchemaFormData {
  name: string;
  description: string;
  dataType: string;
  primaryKey: boolean;
  jsonContent: string;
}

interface PageCacheData {
  content: DatabaseDto[];
  totalElements: number;
}

@Component({
  selector: 'app-schemas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schemas.component.html',
  styleUrl: './schemas.component.scss'
})
export class SchemasComponent {
  private api = inject(Api);
  private authService = inject(AuthService);

  isGuest = computed(() => !this.authService.currentUser());

  // Data Signals
  databases: WritableSignal<DatabaseDto[]> = signal([]);
  tablesMap = signal<Map<string, TableDto[]>>(new Map());
  columnsMap = signal<Map<string, ColumnDto[]>>(new Map());

  // Cache
  private pageCache = new Map<string, PageCacheData>();

  // Fully Loaded Tracking (Search sonuçları ile tam listeyi karıştırmamak için)
  fullyLoadedDbIds = signal<Set<string>>(new Set());
  fullyLoadedTableIds = signal<Set<string>>(new Set());

  // UI States
  isLoading = signal(false);
  expandedDbIds = signal<Set<string>>(new Set());
  expandedTableIds = signal<Set<string>>(new Set());
  loadingTables = signal<Set<string>>(new Set());
  loadingColumns = signal<Set<string>>(new Set());

  // Highlight State
  highlightedId = signal<string | null>(null);

  // Pagination & Search
  searchQuery = signal('');
  page = signal(0);
  size = signal(5);
  sort = signal('createdAt');
  direction = signal<'asc' | 'desc'>('desc');
  totalElements = signal(0);

  // Modals & Forms
  showModal = signal(false);
  showDeleteModal = signal(false);
  showErrorModal = signal(false);
  errorMessage = signal('');
  modalMode = signal<ModalMode>('IMPORT');
  selectedDbId: string | null = null;
  selectedTableId: string | null = null;
  selectedColumnId: string | null = null;
  deleteTarget: { type: DeleteType, id: string, pid?: string, gpid?: string, name?: string } | null = null;

  formData: SchemaFormData = {
    name: '', description: '', dataType: 'varchar', primaryKey: false, jsonContent: ''
  };

  dataTypes = ['integer', 'varchar', 'boolean', 'date', 'text', 'timestamp', 'double', 'float', 'json', 'uuid', 'blob'];
  nameRegex = /^[a-zA-Z0-9_-]+$/;

  totalPages = computed(() => {
    const total = this.totalElements();
    const size = this.size();
    if (total === 0 || size === 0) return 0;
    return Math.ceil(total / size);
  });

  constructor() {
    effect(() => {
      const guest = this.isGuest();
      untracked(() => { this.loadSchemas(); });
    });
  }

  // --- CACHE & HELPERS ---
  private getPageCacheKey(): string {
    return `${this.page()}-${this.size()}-${this.sort()}-${this.direction()}-${this.searchQuery()}`;
  }

  private invalidateCache(type: 'DB' | 'TABLE' | 'COLUMN', parentId?: string) {
    if (type === 'DB') {
      this.pageCache.clear();
      this.fullyLoadedDbIds.set(new Set());
    } else if (type === 'TABLE' && parentId) {
      const map = new Map(this.tablesMap());
      map.delete(parentId);
      this.tablesMap.set(map);

      const loaded = new Set(this.fullyLoadedDbIds());
      loaded.delete(parentId);
      this.fullyLoadedDbIds.set(loaded);
    } else if (type === 'COLUMN' && parentId) {
      const map = new Map(this.columnsMap());
      map.delete(parentId);
      this.columnsMap.set(map);

      const loaded = new Set(this.fullyLoadedTableIds());
      loaded.delete(parentId);
      this.fullyLoadedTableIds.set(loaded);
    }
  }

  // --- READ OPERATIONS (SEARCH LOGIC FIXED) ---
  async loadSchemas() {
    this.isLoading.set(true);
    this.highlightedId.set(null);

    // 1. Guest Mode
    if (this.isGuest()) {
      if (!this.loadFromLocal()) this.saveToLocal();
      this.totalElements.set(this.databases().length);
      this.isLoading.set(false);
      return;
    }

    // 2. Cache Check (Search YOKSA)
    const cacheKey = this.getPageCacheKey();
    if ((!this.searchQuery() || this.searchQuery().trim() === '') && this.pageCache.has(cacheKey)) {
      const cached = this.pageCache.get(cacheKey)!;
      this.databases.set(cached.content);
      this.totalElements.set(cached.totalElements);
      this.isLoading.set(false);
      return;
    }

    try {
      const params = { page: this.page(), size: this.size(), sort: this.sort(), direction: this.direction() };

      // 3. SEARCH LOGIC
      if (this.searchQuery() && this.searchQuery().trim().length > 0) {

        const response = await this.api.invoke(searchSchema, { query: this.searchQuery(), ...params });

        // Gelen Ham Veriler
        const resultDbs = response.databases || [];
        const resultTables = response.tables || [];
        const resultCols = response.columns || [];

        // ---------------------------------------------------------
        // ADIM 1: Eksik Ebeveyn Veritabanlarını Bul ve Getir
        // ---------------------------------------------------------
        const parentDbIds = new Set<string>();

        // 1a. Mevcut DB'leri ekle
        resultDbs.forEach(d => parentDbIds.add(d.databaseId!));

        // 1b. Tablolardan DB ID topla
        resultTables.forEach(t => {
          if (t.databaseId) parentDbIds.add(t.databaseId);
        });

        // 1c. Kolonlardan DB ID topla (Dolaylı: Kolon -> Tablo -> DB)
        // Eğer backend kolon objesinde dbId dönmüyorsa, 'resultTables' içinde parent tabloyu aramalıyız.
        resultCols.forEach(c => {
          // Eğer Search Response içinde kolunun tablosu varsa, o tablonun dbId'sini al
          const parentTable = resultTables.find(t => t.tableId === c.tableId);
          if (parentTable && parentTable.databaseId) {
            parentDbIds.add(parentTable.databaseId);
          }
        });

        // 1d. Eksik DB'leri API'den çek (Eğer resultDbs içinde yoksa)
        // NOT: Paralel istek atmak performans için iyidir.
        const missingDbPromises: Promise<DatabaseDto>[] = [];
        const finalDbs = [...resultDbs];

        for (const dbId of parentDbIds) {
          const exists = finalDbs.find(d => d.databaseId === dbId);
          if (!exists) {
            // API'den tekil DB çek
            missingDbPromises.push(this.api.invoke(getDatabase, { databaseId: dbId }));
          }
        }

        if (missingDbPromises.length > 0) {
          const fetchedDbs = await Promise.all(missingDbPromises);
          finalDbs.push(...fetchedDbs);
        }

        // ---------------------------------------------------------
        // ADIM 2: Verileri State'e İşle (Hiyerarşi Kur)
        // ---------------------------------------------------------

        // Ana DB Listesini güncelle
        this.databases.set(finalDbs);
        this.totalElements.set(finalDbs.length);

        // Tabloları Map'e ekle ve DB'leri aç
        const tMap = new Map(this.tablesMap());
        const expandedDbs = new Set(this.expandedDbIds());

        // Arama sonucundaki tabloları ekle
        for (const table of resultTables) {
          if (table.databaseId) {
            const list = tMap.get(table.databaseId) || [];
            if (!list.find(t => t.tableId === table.tableId)) {
              list.push(table);
            }
            tMap.set(table.databaseId, list);
            expandedDbs.add(table.databaseId); // Parent DB'yi aç
          }
        }
        this.tablesMap.set(tMap);
        this.expandedDbIds.set(expandedDbs);

        // Kolonları Map'e ekle ve Tabloları aç
        const cMap = new Map(this.columnsMap());
        const expandedTables = new Set(this.expandedTableIds());

        for (const col of resultCols) {
          if (col.tableId) {
            const list = cMap.get(col.tableId) || [];
            if (!list.find(c => c.columnId === col.columnId)) {
              list.push(col);
            }
            cMap.set(col.tableId, list);
            expandedTables.add(col.tableId); // Parent Tabloyu aç

            // Eğer kolonun tablosu DB'de expand edilmediyse onu da bulup açmamız lazım
            // (Yukarıda resultTables döngüsü bunu büyük oranda çözer ama garanti olsun)
            const parentTable = resultTables.find(t => t.tableId === col.tableId);
            if (parentTable && parentTable.databaseId) {
              expandedDbs.add(parentTable.databaseId);
              this.expandedDbIds.set(expandedDbs);
            }
          }
        }
        this.columnsMap.set(cMap);
        this.expandedTableIds.set(expandedTables);

        // ---------------------------------------------------------
        // ADIM 3: Odaklan (Leaf Node Focus)
        // ---------------------------------------------------------
        // Angular DOM'u render etmesi için kısa süre tanı
        setTimeout(() => {
          this.focusOnFirstResult(resultCols, resultTables, resultDbs);
        }, 200);

      } else {
        // --- NORMAL LIST ---
        const response: any = await this.api.invoke(getDatabases, params);
        const content = response.content || (Array.isArray(response) ? response : []);
        let total = 0;
        if (response.page && typeof response.page.totalElements === 'number') total = response.page.totalElements;
        else if (typeof response.totalElements === 'number') total = response.totalElements;
        else total = content.length;

        this.databases.set(content);
        this.totalElements.set(total);
        this.pageCache.set(cacheKey, { content: content, totalElements: total });
      }

    } catch (e: any) {
      console.error("Load Error:", e);
    } finally {
      this.isLoading.set(false);
    }
  }

  private focusOnFirstResult(cols: ColumnDto[], tables: TableDto[], dbs: DatabaseDto[]) {
    let targetId = '';

    // Öncelik: Kolon > Tablo > DB
    if (cols.length > 0) targetId = 'col-' + cols[0].columnId;
    else if (tables.length > 0) targetId = 'table-' + tables[0].tableId;
    else if (dbs.length > 0) targetId = 'db-' + dbs[0].databaseId;

    if (targetId) {
      this.highlightedId.set(targetId);
      const el = document.getElementById(targetId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else {
        console.warn('Element not found in DOM:', targetId);
      }
    }
  }

  // --- TOGGLE & LOAD (FULLY LOADED LOGIC) ---

  async toggleDatabase(dbId: string | undefined) {
    if (!dbId) return;
    const expanded = new Set(this.expandedDbIds());

    if (expanded.has(dbId)) {
      expanded.delete(dbId);
    } else {
      expanded.add(dbId);
      // Eğer daha önce "Tam Liste" (Fully Loaded) çekilmediyse, çek.
      // Search sonucunda gelen veri "Kısmi" olabilir.
      const isFullyLoaded = this.fullyLoadedDbIds().has(dbId);
      if (!isFullyLoaded) {
        await this.loadTablesForDb(dbId);
      }
    }
    this.expandedDbIds.set(expanded);
  }

  async loadTablesForDb(dbId: string) {
    if (this.isGuest()) {
      const map = this.tablesMap(); if (!map.has(dbId)) { map.set(dbId, []); this.tablesMap.set(new Map(map)); } return;
    }
    const loading = new Set(this.loadingTables()); loading.add(dbId); this.loadingTables.set(loading);
    try {
      const res: any = await this.api.invoke(getTables, { databaseId: dbId });
      const tables = Array.isArray(res) ? res : (res.content || []);

      const newMap = new Map(this.tablesMap());
      newMap.set(dbId, tables); // Full listeyi set et (merge etme, override et)
      this.tablesMap.set(newMap);

      // İşaretle: Bu DB'nin tabloları tam yüklendi
      this.fullyLoadedDbIds.update(set => { set.add(dbId); return new Set(set); });

    } catch (e) { console.error(e); } finally { const l = new Set(this.loadingTables()); l.delete(dbId); this.loadingTables.set(l); }
  }

  async toggleTable(dbId: string | undefined, tableId: string | undefined) {
    if (!dbId || !tableId) return;
    const expanded = new Set(this.expandedTableIds());

    if (expanded.has(tableId)) {
      expanded.delete(tableId);
    } else {
      expanded.add(tableId);
      const isFullyLoaded = this.fullyLoadedTableIds().has(tableId);
      if (!isFullyLoaded) {
        await this.loadColumnsForTable(dbId, tableId);
      }
    }
    this.expandedTableIds.set(expanded);
  }

  async loadColumnsForTable(dbId: string, tableId: string) {
    if (this.isGuest()) {
      const map = this.columnsMap(); if (!map.has(tableId)) { map.set(tableId, []); this.columnsMap.set(new Map(map)); } return;
    }
    const loading = new Set(this.loadingColumns()); loading.add(tableId); this.loadingColumns.set(loading);
    try {
      const res: any = await this.api.invoke(getColumns, { databaseId: dbId, tableId: tableId });
      const columns = Array.isArray(res) ? res : (res.content || []);

      const newMap = new Map(this.columnsMap());
      newMap.set(tableId, columns);
      this.columnsMap.set(newMap);

      this.fullyLoadedTableIds.update(set => { set.add(tableId); return new Set(set); });

    } catch (e) { console.error(e); } finally { const l = new Set(this.loadingColumns()); l.delete(tableId); this.loadingColumns.set(l); }
  }

  // --- ACTIONS & UTILS (AYNI) ---
  hasExistingPrimaryKey(tableId: string, excludeColumnId?: string): boolean {
    const cols = this.columnsMap().get(tableId) || [];
    return cols.some(c => c.primaryKey && c.columnId !== excludeColumnId);
  }
  checkGuest() { return this.isGuest(); }
  resetForm() { this.formData = { name: '', description: '', dataType: 'varchar', primaryKey: false, jsonContent: '' }; this.selectedDbId = null; this.selectedTableId = null; this.selectedColumnId = null; }

  openImportModal() { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('IMPORT'); this.showModal.set(true); }
  openCreateDatabase() { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_DB'); this.showModal.set(true); }
  openEditDatabase(db: DatabaseDto) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('EDIT_DB'); this.selectedDbId = db.databaseId!; this.formData.name = db.name!; this.formData.description = db.description || ''; this.showModal.set(true); }
  openCreateTable(dbId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_TABLE'); this.selectedDbId = dbId; this.showModal.set(true); }
  openEditTable(table: TableDto, dbId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('EDIT_TABLE'); this.selectedDbId = dbId; this.selectedTableId = table.tableId!; this.formData.name = table.name!; this.formData.description = table.description || ''; this.showModal.set(true); }
  openCreateColumn(dbId: string, tableId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_COLUMN'); this.selectedDbId = dbId; this.selectedTableId = tableId; this.formData.dataType = 'varchar'; this.showModal.set(true); }
  openEditColumn(col: ColumnDto, dbId: string, tableId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('EDIT_COLUMN'); this.selectedDbId = dbId; this.selectedTableId = tableId; this.selectedColumnId = col.columnId!; this.formData.name = col.name!; this.formData.dataType = col.dataType!.toLowerCase(); this.formData.primaryKey = !!col.primaryKey; this.showModal.set(true); }

  closeModal() { this.showModal.set(false); this.resetForm(); }
  openErrorModal(msg: string) { this.errorMessage.set(msg); this.showErrorModal.set(true); }
  closeErrorModal() { this.showErrorModal.set(false); this.errorMessage.set(''); }
  openDeleteModal(type: DeleteType, id: string, name: string, pid?: string, gpid?: string) { this.deleteTarget = { type, id, pid, gpid, name }; this.showDeleteModal.set(true); }
  closeDeleteModal() { this.showDeleteModal.set(false); this.deleteTarget = null; }

  onSortChange(column: string) {
    if (this.sort() === column) this.direction.set(this.direction() === 'asc' ? 'desc' : 'asc');
    else { this.sort.set(column); this.direction.set('asc'); }
    this.loadSchemas();
  }
  getSortIcon(column: string): string {
    if (this.sort() !== column) return 'fas fa-sort text-muted opacity-25';
    return this.direction() === 'asc' ? 'fas fa-sort-up text-primary' : 'fas fa-sort-down text-primary';
  }
  onPageChange(newPage: number) { if (newPage >= 0 && newPage < this.totalPages()) { this.page.set(newPage); this.loadSchemas(); } }
  onSizeChange(newSize: number) { this.size.set(newSize); this.page.set(0); this.loadSchemas(); }
  async onSearch() { this.page.set(0); await this.loadSchemas(); }

  validateForm(mode: ModalMode): string | null {
    if (mode === 'IMPORT') {
      if (!this.formData.jsonContent || this.formData.jsonContent.trim().length === 0) return "JSON content cannot be empty.";
      try { JSON.parse(this.formData.jsonContent); } catch (e) { return "Invalid JSON format."; }
      return null;
    }
    const name = this.formData.name ? this.formData.name.trim() : '';
    const desc = this.formData.description ? this.formData.description.trim() : '';
    if (name.length === 0) return "Name cannot be empty.";
    if (!this.nameRegex.test(name)) return "Name contains invalid characters. Use only letters, numbers, '-' or '_'.";
    if (mode.includes('DB') || mode.includes('TABLE')) {
      if (name.length < 2 || name.length > 64) return "Name must be between 2 and 64 characters.";
      if (desc.length > 255) return "Description cannot exceed 255 characters.";
    }
    if (mode.includes('COLUMN')) {
      if (name.length < 1 || name.length > 50) return "Column name must be between 1 and 50 characters.";
    }
    return null;
  }

  async submitModal() {
    const error = this.validateForm(this.modalMode());
    if (error) { this.openErrorModal(error); return; }
    this.isLoading.set(true);
    const mode = this.modalMode();
    if (this.isGuest()) {
      const id = crypto.randomUUID(); const now = new Date().toLocaleDateString();
      if (mode === 'CREATE_DB') { this.databases.update(dbs => [...dbs, { databaseId: id, name: this.formData.name, description: this.formData.description, createdAt: now }]); this.totalElements.update(n => n + 1); }
      this.saveToLocal(); this.closeModal(); this.isLoading.set(false); return;
    }
    try {
      if (mode === 'IMPORT') { await this.api.invoke(importSchema, { body: { name: this.formData.name, description: this.formData.description, jsonContent: JSON.parse(this.formData.jsonContent) } }); this.invalidateCache('DB'); await this.loadSchemas(); }
      else if (mode === 'CREATE_DB') { await this.api.invoke(createDatabase, { body: { name: this.formData.name, description: this.formData.description } }); this.invalidateCache('DB'); await this.loadSchemas(); }
      else if (mode === 'EDIT_DB' && this.selectedDbId) { await this.api.invoke(updateDatabase, { databaseId: this.selectedDbId, body: { name: this.formData.name, description: this.formData.description } }); this.invalidateCache('DB'); await this.loadSchemas(); }
      else if (mode === 'CREATE_TABLE' && this.selectedDbId) { await this.api.invoke(createTable, { databaseId: this.selectedDbId, body: { name: this.formData.name, description: this.formData.description } }); this.invalidateCache('TABLE', this.selectedDbId); await this.loadTablesForDb(this.selectedDbId); }
      else if (mode === 'EDIT_TABLE' && this.selectedDbId && this.selectedTableId) { await this.api.invoke(updateTable, { databaseId: this.selectedDbId, tableId: this.selectedTableId, body: { name: this.formData.name, description: this.formData.description } }); this.invalidateCache('TABLE', this.selectedDbId); await this.loadTablesForDb(this.selectedDbId); }
      else if (mode === 'CREATE_COLUMN' && this.selectedDbId && this.selectedTableId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId)) { this.openErrorModal("This table already has a Primary Key."); this.isLoading.set(false); return; }
        await this.api.invoke(createColumn, { databaseId: this.selectedDbId, tableId: this.selectedTableId, body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey } });
        this.invalidateCache('COLUMN', this.selectedTableId); await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      else if (mode === 'EDIT_COLUMN' && this.selectedDbId && this.selectedTableId && this.selectedColumnId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId, this.selectedColumnId)) { this.openErrorModal("This table already has a Primary Key."); this.isLoading.set(false); return; }
        await this.api.invoke(updateColumn, { databaseId: this.selectedDbId, tableId: this.selectedTableId, columnId: this.selectedColumnId, body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey } });
        this.invalidateCache('COLUMN', this.selectedTableId); await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      this.closeModal();
    } catch (e: any) { console.error("Op Failed:", e); const msg = this.getErrorMessage(e); this.openErrorModal(msg); } finally { this.isLoading.set(false); }
  }

  async confirmDelete() {
    if (!this.deleteTarget) return;
    const { type, id, pid, gpid } = this.deleteTarget;
    this.isLoading.set(true);
    if (this.isGuest()) { this.saveToLocal(); this.closeDeleteModal(); this.isLoading.set(false); return; }
    try {
      if (type === 'DB') { await this.api.invoke(deleteDatabase, { databaseId: id }); this.invalidateCache('DB'); await this.loadSchemas(); }
      else if (type === 'TABLE' && pid) { await this.api.invoke(deleteTable, { databaseId: pid, tableId: id }); this.invalidateCache('TABLE', pid); await this.loadTablesForDb(pid); }
      else if (type === 'COLUMN' && pid && gpid) { await this.api.invoke(deleteColumn, { databaseId: gpid, tableId: pid, columnId: id }); this.invalidateCache('COLUMN', pid); await this.loadColumnsForTable(gpid, pid); }
      this.closeDeleteModal();
    } catch (e: any) { const msg = this.getErrorMessage(e); this.openErrorModal(msg); } finally { this.isLoading.set(false); }
  }

  private saveToLocal() { if (!this.isGuest()) return; const data = { dbs: this.databases(), tables: Array.from(this.tablesMap().entries()), columns: Array.from(this.columnsMap().entries()) }; localStorage.setItem('guest_schema_data_v2', JSON.stringify(data)); }
  private loadFromLocal() { const raw = localStorage.getItem('guest_schema_data_v2'); if (raw) { const data = JSON.parse(raw); this.databases.set(data.dbs || []); this.tablesMap.set(new Map(data.tables)); this.columnsMap.set(new Map(data.columns)); return true; } return false; }
  private getErrorMessage(err: any): string {
    if (err && err.error && typeof err.error === 'object') {
      const backendMsg = err.error.message;
      const backendErrorType = err.error.error;
      if (backendMsg) return backendErrorType ? `${backendErrorType}: ${backendMsg}` : backendMsg;
    }
    if (err instanceof HttpErrorResponse) {
      // if (err.status === 401) return "Unauthorized: Please log in.";
      // if (err.status === 403) return "Forbidden: You don't have permission.";
      // if (err.status === 404) return "Not Found: Resource does not exist.";
      // if (err.status === 409) return "Conflict: Data already exists.";
      // if (err.status === 500) return "Server Error: Something went wrong.";
      return `Error (${err.status}): ${err.statusText}`;
    }
    return "An unexpected error occurred.";
  }
}
