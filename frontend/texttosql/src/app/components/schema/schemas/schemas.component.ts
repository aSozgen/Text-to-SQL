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
  getDatabases, getTables, getColumns,
  deleteDatabase, deleteTable, deleteColumn,
  updateDatabase, updateTable, updateColumn,
  createDatabase, createTable, createColumn,
  importSchema
} from '../../../api/functions';

type ModalMode = 'IMPORT' | 'CREATE_DB' | 'EDIT_DB' | 'CREATE_TABLE' | 'EDIT_TABLE' | 'CREATE_COLUMN' | 'EDIT_COLUMN';
type DeleteType = 'DB' | 'TABLE' | 'COLUMN';

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

  // Data
  databases: WritableSignal<DatabaseDto[]> = signal([]);
  tablesMap = signal<Map<string, TableDto[]>>(new Map());
  columnsMap = signal<Map<string, ColumnDto[]>>(new Map());

  // UI States
  isLoading = signal(false);
  expandedDbIds = signal<Set<string>>(new Set());
  expandedTableIds = signal<Set<string>>(new Set());
  loadingTables = signal<Set<string>>(new Set());
  loadingColumns = signal<Set<string>>(new Set());

  // Search & Pagination
  searchQuery = signal('');
  page = 0; size = 10; sort = 'createdAt'; direction = 'desc';

  // Modals
  showModal = signal(false);
  showDeleteModal = signal(false);
  showErrorModal = signal(false);
  errorMessage = signal('');
  modalMode = signal<ModalMode>('IMPORT');

  // Selected Items
  selectedDbId: string | null = null;
  selectedTableId: string | null = null;
  selectedColumnId: string | null = null;

  deleteTarget: { type: DeleteType, id: string, pid?: string, gpid?: string, name?: string } | null = null;

  formData = {
    name: '',
    description: '',
    dataType: 'varchar',
    primaryKey: false,
    jsonContent: ''
  };

  dataTypes = ['integer', 'varchar', 'boolean', 'date', 'text', 'timestamp', 'double', 'float', 'json', 'uuid', 'blob'];

  constructor() {
    effect(() => {
      const guest = this.isGuest();
      untracked(() => { this.loadSchemas(); });
    });
  }

  // --- LOCAL STORAGE HELPERS ---
  private saveToLocal() {
    if (!this.isGuest()) return;
    const data = {
      dbs: this.databases(),
      tables: Array.from(this.tablesMap().entries()),
      columns: Array.from(this.columnsMap().entries())
    };
    localStorage.setItem('guest_schema_data', JSON.stringify(data));
  }

  private loadFromLocal() {
    const raw = localStorage.getItem('guest_schema_data');
    if (raw) {
      const data = JSON.parse(raw);
      this.databases.set(data.dbs || []);
      this.tablesMap.set(new Map(data.tables));
      this.columnsMap.set(new Map(data.columns));
      return true;
    }
    return false;
  }

  // --- ERROR HANDLING HELPER (GÜNCELLENDİ) ---
  private getErrorMessage(err: any): string {

    // 1. Backend JSON Response kontrolü
    // Beklenen format: { error: "Conflict", message: "...", ... }
    // Angular HttpClient bunu genelde 'err.error' objesi olarak parse eder.
    if (err && err.error && typeof err.error === 'object') {
      const backendError = err.error.error;     // "Conflict"
      const backendMsg = err.error.message;     // "There is already a Column..."

      if (backendError && backendMsg) {
        return `${backendError}: ${backendMsg}`;
      }
      if (backendMsg) {
        return backendMsg;
      }
    }

    // 2. Eğer JSON string olarak geldiyse (Nadir durum)
    if (err && typeof err.error === 'string') {
      try {
        const parsed = JSON.parse(err.error);
        if (parsed.error && parsed.message) return `${parsed.error}: ${parsed.message}`;
        if (parsed.message) return parsed.message;
      } catch (e) {
        // String parse edilemediyse ama kısaysa göster
        if (err.error.length < 150) return err.error;
      }
    }

    // 3. Fallback: Status Code'a göre genel mesajlar
    if (err instanceof HttpErrorResponse) {
      switch (err.status) {
        case 409: return "Conflict: Operation could not be completed due to a conflict.";
        case 400: return "Bad Request: Please check your input.";
        case 401: return "Unauthorized: Session expired.";
        case 403: return "Forbidden: You do not have permission.";
        case 404: return "Not Found: Resource does not exist.";
        case 500: return "Server Error: Something went wrong internally.";
        case 0: return "Connection Error: Could not reach the server.";
      }
    }

    return "An unexpected error occurred.";
  }

  // --- READ OPERATIONS ---
  async loadSchemas() {
    this.isLoading.set(true);
    if (this.isGuest()) {
      if (!this.loadFromLocal()) {
        const mockDbs = [{ databaseId: 'mock-1', name: 'Demo_Store', description: 'Local Demo', createdAt: new Date().toLocaleDateString() }];
        this.databases.set(mockDbs);
        this.saveToLocal();
      }
      this.isLoading.set(false);
      return;
    }

    try {
      const params: any = { page: this.page, size: this.size, sort: this.sort, direction: this.direction };
      if (this.searchQuery()) params['search'] = this.searchQuery();
      const response = await this.api.invoke(getDatabases, params);
      this.databases.set(response.content || []);
    } catch (e) {
      console.error(e);
    } finally {
      this.isLoading.set(false);
    }
  }

  async toggleDatabase(dbId: string | undefined) {
    if (!dbId) return;
    const expanded = new Set(this.expandedDbIds());
    if (expanded.has(dbId)) expanded.delete(dbId);
    else {
      expanded.add(dbId);
      if (!this.tablesMap().has(dbId)) await this.loadTablesForDb(dbId);
    }
    this.expandedDbIds.set(expanded);
  }

  async loadTablesForDb(dbId: string) {
    if (this.isGuest()) {
      const currentMap = this.tablesMap();
      if (!currentMap.has(dbId)) {
        currentMap.set(dbId, []);
        this.tablesMap.set(new Map(currentMap));
        this.saveToLocal();
      }
      return;
    }
    const loading = new Set(this.loadingTables()); loading.add(dbId); this.loadingTables.set(loading);
    try {
      const res = await this.api.invoke(getTables, { databaseId: dbId, page: 0, size: 100 });
      const newMap = new Map(this.tablesMap());
      newMap.set(dbId, res.content || []);
      this.tablesMap.set(newMap);
    } catch (e) { console.error(e); } finally {
      const l = new Set(this.loadingTables()); l.delete(dbId); this.loadingTables.set(l);
    }
  }

  async toggleTable(dbId: string | undefined, tableId: string | undefined) {
    if (!dbId || !tableId) return;
    const expanded = new Set(this.expandedTableIds());
    if (expanded.has(tableId)) expanded.delete(tableId);
    else {
      expanded.add(tableId);
      if (!this.columnsMap().has(tableId)) await this.loadColumnsForTable(dbId, tableId);
    }
    this.expandedTableIds.set(expanded);
  }

  async loadColumnsForTable(dbId: string, tableId: string) {
    if (this.isGuest()) {
      const currentMap = this.columnsMap();
      if (!currentMap.has(tableId)) {
        currentMap.set(tableId, []);
        this.columnsMap.set(new Map(currentMap));
        this.saveToLocal();
      }
      return;
    }
    const loading = new Set(this.loadingColumns()); loading.add(tableId); this.loadingColumns.set(loading);
    try {
      const res = await this.api.invoke(getColumns, { databaseId: dbId, tableId: tableId, page: 0, size: 100 });
      const newMap = new Map(this.columnsMap());
      newMap.set(tableId, res.content || []);
      this.columnsMap.set(newMap);
    } catch (e) { console.error(e); } finally {
      const l = new Set(this.loadingColumns()); l.delete(tableId); this.loadingColumns.set(l);
    }
  }

  hasExistingPrimaryKey(tableId: string, excludeColumnId?: string): boolean {
    const cols = this.columnsMap().get(tableId) || [];
    return cols.some(c => c.primaryKey && c.columnId !== excludeColumnId);
  }

  // --- MODAL ACTIONS ---
  checkGuest() { return this.isGuest(); }

  resetForm() {
    this.formData = { name: '', description: '', dataType: 'varchar', primaryKey: false, jsonContent: '' };
    this.selectedDbId = null; this.selectedTableId = null; this.selectedColumnId = null;
  }

  openImportModal() { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('IMPORT'); this.showModal.set(true); }
  openCreateDatabase() { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_DB'); this.showModal.set(true); }

  openEditDatabase(db: DatabaseDto) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('EDIT_DB');
    this.selectedDbId = db.databaseId || null;
    this.formData = { name: db.name, description: db.description || '', dataType: '', primaryKey: false, jsonContent: '' };
    this.showModal.set(true);
  }

  openCreateTable(dbId: string) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('CREATE_TABLE');
    this.selectedDbId = dbId;
    this.showModal.set(true);
  }

  openEditTable(table: TableDto, dbId: string) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('EDIT_TABLE');
    this.selectedDbId = dbId;
    this.selectedTableId = table.tableId || null;
    this.formData = { name: table.name, description: table.description || '', dataType: '', primaryKey: false, jsonContent: '' };
    this.showModal.set(true);
  }

  openCreateColumn(dbId: string, tableId: string) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('CREATE_COLUMN');
    this.selectedDbId = dbId;
    this.selectedTableId = tableId;
    this.formData.dataType = 'varchar';
    this.showModal.set(true);
  }

  openEditColumn(col: ColumnDto, dbId: string, tableId: string) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('EDIT_COLUMN');
    this.selectedDbId = dbId;
    this.selectedTableId = tableId;
    this.selectedColumnId = col.columnId || null;
    this.formData = {
      name: col.name,
      description: '',
      dataType: col.dataType?.toLowerCase() || 'varchar',
      primaryKey: !!col.primaryKey,
      jsonContent: ''
    };
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.resetForm(); }

  // --- ERROR MODAL ---
  openErrorModal(msg: string) {
    this.errorMessage.set(msg);
    this.showErrorModal.set(true);
  }

  closeErrorModal() {
    this.showErrorModal.set(false);
    this.errorMessage.set('');
  }

  // --- SUBMIT LOGIC ---
  async submitModal() {
    this.isLoading.set(true);
    const mode = this.modalMode();

    if (this.isGuest()) {
      const id = crypto.randomUUID();
      const now = new Date().toLocaleDateString();

      if (mode === 'CREATE_DB') {
        this.databases.update(dbs => [...dbs, { databaseId: id, name: this.formData.name, description: this.formData.description, createdAt: now }]);
      }
      else if (mode === 'EDIT_DB' && this.selectedDbId) {
        this.databases.update(dbs => dbs.map(d => d.databaseId === this.selectedDbId ? { ...d, name: this.formData.name, description: this.formData.description } : d));
      }
      else if (mode === 'CREATE_TABLE' && this.selectedDbId) {
        const newTable = { tableId: id, name: this.formData.name, description: this.formData.description, createdAt: now };
        const map = new Map(this.tablesMap());
        const tables = map.get(this.selectedDbId) || [];
        map.set(this.selectedDbId, [...tables, newTable]);
        this.tablesMap.set(map);
      }
      else if (mode === 'CREATE_COLUMN' && this.selectedDbId && this.selectedTableId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId)) {
          this.openErrorModal("This table already has a Primary Key. Please disable the existing one first.");
          this.isLoading.set(false); return;
        }
        const newCol = { columnId: id, name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey };
        const map = new Map(this.columnsMap());
        const cols = map.get(this.selectedTableId) || [];
        map.set(this.selectedTableId, [...cols, newCol]);
        this.columnsMap.set(map);
      }
      else if (mode === 'EDIT_COLUMN' && this.selectedTableId && this.selectedColumnId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId, this.selectedColumnId)) {
          this.openErrorModal("This table already has a Primary Key.");
          this.isLoading.set(false); return;
        }
        const map = new Map(this.columnsMap());
        const cols = map.get(this.selectedTableId)?.map(c => c.columnId === this.selectedColumnId ? { ...c, name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey } : c) || [];
        map.set(this.selectedTableId, cols);
        this.columnsMap.set(map);
      }

      this.saveToLocal();
      this.closeModal();
      this.isLoading.set(false);
      return;
    }

    try {
      if (mode === 'IMPORT') {
        await this.api.invoke(importSchema, { body: { name: this.formData.name, description: this.formData.description, jsonContent: JSON.parse(this.formData.jsonContent) } });
        await this.loadSchemas();
      }
      else if (mode === 'CREATE_DB') {
        await this.api.invoke(createDatabase, { body: { name: this.formData.name, description: this.formData.description } });
        await this.loadSchemas();
      }
      else if (mode === 'EDIT_DB' && this.selectedDbId) {
        await this.api.invoke(updateDatabase, { databaseId: this.selectedDbId, body: { name: this.formData.name, description: this.formData.description } });
        await this.loadSchemas();
      }
      else if (mode === 'CREATE_TABLE' && this.selectedDbId) {
        await this.api.invoke(createTable, { databaseId: this.selectedDbId, body: { name: this.formData.name, description: this.formData.description } });
        await this.loadTablesForDb(this.selectedDbId);
      }
      else if (mode === 'EDIT_TABLE' && this.selectedDbId && this.selectedTableId) {
        await this.api.invoke(updateTable, { databaseId: this.selectedDbId, tableId: this.selectedTableId, body: { name: this.formData.name, description: this.formData.description } });
        await this.loadTablesForDb(this.selectedDbId);
      }
      else if (mode === 'CREATE_COLUMN' && this.selectedDbId && this.selectedTableId) {
        // İstemci tarafı ön kontrol (Conflict'i önlemek için)
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId)) {
          this.openErrorModal("This table already has a Primary Key. Disable the existing one first.");
          this.isLoading.set(false); return;
        }
        await this.api.invoke(createColumn, {
          databaseId: this.selectedDbId, tableId: this.selectedTableId,
          body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey }
        });
        await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      else if (mode === 'EDIT_COLUMN' && this.selectedDbId && this.selectedTableId && this.selectedColumnId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId, this.selectedColumnId)) {
          this.openErrorModal("This table already has a Primary Key.");
          this.isLoading.set(false); return;
        }
        await this.api.invoke(updateColumn, {
          databaseId: this.selectedDbId, tableId: this.selectedTableId, columnId: this.selectedColumnId,
          body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey }
        });
        await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }

      this.closeModal();
    } catch (e: any) {
      console.error("Operation Failed:", e);
      const msg = this.getErrorMessage(e); // Backend mesajını çek
      this.openErrorModal(msg);
    } finally {
      this.isLoading.set(false);
    }
  }

  // --- DELETE MODAL ---
  openDeleteModal(type: DeleteType, id: string, name: string, pid?: string, gpid?: string) {
    this.deleteTarget = { type, id, pid, gpid, name };
    this.showDeleteModal.set(true);
  }

  closeDeleteModal() {
    this.showDeleteModal.set(false);
    this.deleteTarget = null;
  }

  async confirmDelete() {
    if (!this.deleteTarget) return;
    const { type, id, pid, gpid } = this.deleteTarget;
    this.isLoading.set(true);

    if (this.isGuest()) {
      if (type === 'DB') this.databases.update(dbs => dbs.filter(d => d.databaseId !== id));
      else if (type === 'TABLE' && pid) {
        const map = new Map(this.tablesMap());
        map.set(pid, map.get(pid)?.filter(t => t.tableId !== id) || []);
        this.tablesMap.set(map);
      } else if (type === 'COLUMN' && pid) {
        const map = new Map(this.columnsMap());
        map.set(pid, map.get(pid)?.filter(c => c.columnId !== id) || []);
        this.columnsMap.set(map);
      }
      this.saveToLocal();
      this.closeDeleteModal();
      this.isLoading.set(false);
      return;
    }

    try {
      if (type === 'DB') { await this.api.invoke(deleteDatabase, { databaseId: id }); await this.loadSchemas(); }
      else if (type === 'TABLE' && pid) { await this.api.invoke(deleteTable, { databaseId: pid, tableId: id }); await this.loadTablesForDb(pid); }
      else if (type === 'COLUMN' && pid && gpid) { await this.api.invoke(deleteColumn, { databaseId: gpid, tableId: pid, columnId: id }); await this.loadColumnsForTable(gpid, pid); }
      this.closeDeleteModal();
    } catch (e: any) {
      const msg = this.getErrorMessage(e);
      this.openErrorModal(msg);
    } finally {
      this.isLoading.set(false);
    }
  }

  async onSearch() { this.page = 0; await this.loadSchemas(); }
}
