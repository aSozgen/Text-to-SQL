import { Component, inject, signal, WritableSignal, computed, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../../api/api';
import { AuthService } from '../../core/auth.service';

import { DatabaseDto } from '../../api/models/database-dto';
import { TableDto } from '../../api/models/table-dto';
import { ColumnDto } from '../../api/models/column-dto';

import {
  getDatabase, getTable, importSchema,
  deleteDatabase, deleteTable, deleteColumn,
  updateDatabase, updateTable, updateColumn,
  createDatabase, createTable, createColumn
} from '../../api/functions';

// Search Function
import { searchSchema } from '../../api/fn/4-search/search-schema';
import {ErrorHandlerService} from '../../core/error.handler.service';
import {SchemaService} from '../../core/schema.service';

type ModalMode = 'IMPORT' | 'CREATE_DB' | 'EDIT_DB' | 'CREATE_TABLE' | 'EDIT_TABLE' | 'CREATE_COLUMN' | 'EDIT_COLUMN';
type DeleteType = 'DB' | 'TABLE' | 'COLUMN';
type SupportedDb = 'POSTGRESQL' | 'MYSQL' | 'SQLSERVER' | 'ORACLE';

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
  private readonly api = inject(Api);
  private readonly authService = inject(AuthService);
  private readonly schemaService = inject(SchemaService);
  private readonly errorHandler = inject(ErrorHandlerService);

  isGuest = computed(() => !this.authService.currentUser());
  isCopied = signal<boolean>(false);

  // Data Signals
  databases: WritableSignal<DatabaseDto[]> = signal([]);
  tablesMap = signal<Map<string, TableDto[]>>(new Map());
  columnsMap = signal<Map<string, ColumnDto[]>>(new Map());

  // Import Wizard State
  importStep = signal<1 | 2 | 3>(1);
  selectedDb = signal<SupportedDb>('POSTGRESQL');

  dbQueries: Record<SupportedDb, string> = {
    POSTGRESQL: `SELECT json_agg(json_build_object('table_name', table_name, 'column_name', column_name, 'data_type', data_type)) FROM information_schema.columns WHERE table_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast');`,
    MYSQL: `SELECT JSON_ARRAYAGG(JSON_OBJECT('table_name', TABLE_NAME, 'column_name', COLUMN_NAME, 'data_type', DATA_TYPE)) FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE();`,
    SQLSERVER: `SELECT TABLE_NAME as table_name, COLUMN_NAME as column_name, DATA_TYPE as data_type FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA NOT IN ('information_schema', 'sys') FOR JSON PATH;`,
    ORACLE: `SELECT JSON_ARRAYAGG(JSON_OBJECT('table_name' VALUE table_name, 'column_name' VALUE column_name, 'data_type' VALUE data_type)) FROM all_tab_columns WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_USER')`
  };

  currentQuery = computed(() => this.dbQueries[this.selectedDb()]);

  private pageCache = new Map<string, PageCacheData>();
  fullyLoadedDbIds = signal<Set<string>>(new Set());
  fullyLoadedTableIds = signal<Set<string>>(new Set());

  isLoading = signal<boolean>(false);
  expandedDbIds = signal<Set<string>>(new Set());
  expandedTableIds = signal<Set<string>>(new Set());
  loadingTables = signal<Set<string>>(new Set());
  loadingColumns = signal<Set<string>>(new Set());

  highlightedId = signal<string | null>(null);

  searchQuery = signal<string>('');
  page = signal<number>(0);
  size = signal<number>(5);
  sort = signal<string>('createdAt');
  direction = signal<'asc' | 'desc'>('desc');
  totalElements = signal<number>(0);

  showModal = signal<boolean>(false);
  showDeleteModal = signal<boolean>(false);
  showErrorModal = signal<boolean>(false);
  errorMessage = signal<string>('');
  modalMode = signal<ModalMode>('IMPORT');

  selectedDbId: string | null = null;
  selectedTableId: string | null = null;
  selectedColumnId: string | null = null;
  deleteTarget: { type: DeleteType, id: string, pid?: string, gpid?: string, name?: string } | null = null;

  formData: SchemaFormData = { name: '', description: '', dataType: 'varchar', primaryKey: false, jsonContent: '' };
  dataTypes = [
    'integer',
    'int',
    'smallint',
    'bigint',
    'tinyint',
    'mediumint',
    'decimal',
    'numeric',
    'float',
    'double',
    'real',
    'double precision',
    'money',
    'varchar',
    'char',
    'text',
    'varchar2',
    'nvarchar',
    'nchar',
    'ntext',
    'character',
    'character varying',
    'longtext',
    'mediumtext',
    'tinytext',
    'clob',
    'nclob',
    'boolean',
    'bool',
    'bit',
    'date',
    'time',
    'datetime',
    'timestamp',
    'timestamp with time zone',
    'timestamp without time zone',
    'timestamptz',
    'year',
    'interval',
    'blob',
    'binary',
    'varbinary',
    'bytea',
    'raw',
    'long raw',
    'image',
    'json',
    'jsonb',
    'xml',
    'array',
    'hstore',
    'uuid',
    'guid',
    'serial',
    'bigserial',
    'smallserial',
    'auto_increment',
    'enum',
    'set',
    'point',
    'line',
    'polygon',
    'geometry',
    'geography',
    'inet',
    'cidr',
    'macaddr',
    'number',
    'long',
    'rowid',
    'urowid',
    'bfile',
    'any',
    'sql_variant',
    'cursor',
    'table'
  ];
  nameRegex = /^[a-zA-Z0-9_-]+$/;

  totalPages = computed(() => {
    const total = this.totalElements();
    const size = this.size();
    return (total === 0 || size === 0) ? 0 : Math.ceil(total / size);
  });

  constructor() {
    effect(() => {
      const guest = this.isGuest();
      untracked(() => { this.loadSchemas(); });
    });
  }

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

  async loadSchemas() {
    this.isLoading.set(true);
    this.highlightedId.set(null);

    if (this.isGuest()) {
      if (!this.loadFromLocal()) this.saveToLocal();
      this.totalElements.set(this.databases().length);
      this.isLoading.set(false);
      return;
    }

    const cacheKey = this.getPageCacheKey();
    if ((!this.searchQuery() || !this.searchQuery().trim()) && this.pageCache.has(cacheKey)) {
      const cached = this.pageCache.get(cacheKey)!;
      this.databases.set(cached.content);
      this.totalElements.set(cached.totalElements);
      this.isLoading.set(false);
      return;
    }

    try {
      const params = { page: this.page(), size: this.size(), sort: this.sort(), direction: this.direction() };

      if (this.searchQuery() && this.searchQuery().trim().length > 0) {
        const res = await this.api.invoke(searchSchema, { query: this.searchQuery(), ...params });

        const resDbs = res.databases || [];
        const resTables = res.tables || [];
        const resCols = res.columns || [];

        const requiredDbIds = new Set<string>();

        resDbs.forEach(d => { if(d.databaseId) requiredDbIds.add(d.databaseId); });
        resTables.forEach(t => { if (t.databaseId) requiredDbIds.add(t.databaseId); });
        resCols.forEach(c => { if (c.databaseId) requiredDbIds.add(c.databaseId); });

        const finalDbs = [...resDbs];
        const missingDbPromises: Promise<DatabaseDto>[] = [];

        requiredDbIds.forEach(id => {
          if (!finalDbs.some(d => d.databaseId === id)) {
            missingDbPromises.push(this.api.invoke(getDatabase, { databaseId: id }));
          }
        });

        if (missingDbPromises.length > 0) {
          try {
            const fetchedDbs = await Promise.all(missingDbPromises);
            finalDbs.push(...fetchedDbs);
          } catch (e) {
            // Silent catch: If parent DB metadata fails, just skip adding them.
            // User sees the search result but maybe not the parent context.
          }
        }

        const finalTables = [...resTables];
        const missingTablePromises: Promise<TableDto>[] = [];
        const tablesToFetch = new Map<string, string>();

        resCols.forEach(c => {
          if (c.tableId && c.databaseId) {
            if (!finalTables.some(t => t.tableId === c.tableId)) {
              tablesToFetch.set(c.tableId, c.databaseId);
            }
          }
        });

        tablesToFetch.forEach((dbId, tableId) => {
          missingTablePromises.push(this.api.invoke(getTable, { databaseId: dbId, tableId: tableId }));
        });

        if (missingTablePromises.length > 0) {
          try {
            const fetchedTables = await Promise.all(missingTablePromises);
            finalTables.push(...fetchedTables);
          } catch (e) {
            // Silent catch: Similar to DBs, avoid interrupting user flow for metadata.
          }
        }

        this.databases.set(finalDbs);
        this.totalElements.set(finalDbs.length);

        const tMap = new Map(this.tablesMap());
        const expandedDbs = new Set(this.expandedDbIds());

        finalTables.forEach(table => {
          if (table.databaseId) {
            const list = tMap.get(table.databaseId) || [];
            if (!list.some(t => t.tableId === table.tableId)) {
              list.push(table);
            }
            tMap.set(table.databaseId, list);
            expandedDbs.add(table.databaseId);
          }
        });
        this.tablesMap.set(tMap);
        this.expandedDbIds.set(expandedDbs);

        const cMap = new Map(this.columnsMap());
        const expandedTables = new Set(this.expandedTableIds());

        resCols.forEach(col => {
          if (col.tableId) {
            const list = cMap.get(col.tableId) || [];
            if (!list.some(c => c.columnId === col.columnId)) {
              list.push(col);
            }
            cMap.set(col.tableId, list);
            expandedTables.add(col.tableId);
          }
        });
        this.columnsMap.set(cMap);
        this.expandedTableIds.set(expandedTables);

        setTimeout(() => {
          this.focusOnFirstResult(resCols, resTables, resDbs);
        }, 300);

      } else {
        const response: any = await this.schemaService.loadDatabases(params);
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
      this.openErrorModal(this.errorHandler.message(e));
    } finally {
      this.isLoading.set(false);
    }
  }
  private focusOnFirstResult(cols: ColumnDto[], tables: TableDto[], dbs: DatabaseDto[]) {
    let targetId = '';
    if (cols.length > 0) targetId = 'col-' + cols[0].columnId;
    else if (tables.length > 0) targetId = 'table-' + tables[0].tableId;
    else if (dbs.length > 0) targetId = 'db-' + dbs[0].databaseId;

    if (targetId) {
      this.highlightedId.set(targetId);
      const el = document.getElementById(targetId);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }


  async toggleDatabase(dbId: string | undefined) {
    if (!dbId) return;
    const expanded = new Set(this.expandedDbIds());
    if (expanded.has(dbId)) {
      expanded.delete(dbId);
    } else {
      expanded.add(dbId);
      if (!this.fullyLoadedDbIds().has(dbId)) {
        await this.loadTablesForDb(dbId);
      }
    }
    this.expandedDbIds.set(expanded);
  }

  async loadTablesForDb(dbId: string) {
    if (this.isGuest()) {
      const map = this.tablesMap();
      if (!map.has(dbId)) { map.set(dbId, []); this.tablesMap.set(new Map(map)); }
      return;
    }
    const loading = new Set(this.loadingTables());
    loading.add(dbId);
    this.loadingTables.set(loading);

    try {
      const tables: TableDto[] = await this.schemaService.loadTables(dbId);

      const newMap = new Map(this.tablesMap());
      newMap.set(dbId, tables);
      this.tablesMap.set(newMap);

      this.fullyLoadedDbIds.update(set => { set.add(dbId); return new Set(set); });
    } catch (e) { console.error(e); }
    finally {
      const l = new Set(this.loadingTables());
      l.delete(dbId);
      this.loadingTables.set(l);
    }
  }

  async toggleTable(dbId: string | undefined, tableId: string | undefined) {
    if (!dbId || !tableId) return;
    const expanded = new Set(this.expandedTableIds());
    if (expanded.has(tableId)) {
      expanded.delete(tableId);
    } else {
      expanded.add(tableId);
      if (!this.fullyLoadedTableIds().has(tableId)) {
        await this.loadColumnsForTable(dbId, tableId);
      }
    }
    this.expandedTableIds.set(expanded);
  }

  async loadColumnsForTable(dbId: string, tableId: string) {
    if (this.isGuest()) {
      const map = this.columnsMap();
      if (!map.has(tableId)) { map.set(tableId, []); this.columnsMap.set(new Map(map)); }
      return;
    }
    const loading = new Set(this.loadingColumns());
    loading.add(tableId);
    this.loadingColumns.set(loading);

    try {
      const columns = await this.schemaService.loadColumns(dbId, tableId);

      const newMap = new Map(this.columnsMap());
      newMap.set(tableId, columns);
      this.columnsMap.set(newMap);

      this.fullyLoadedTableIds.update(set => { set.add(tableId); return new Set(set); });
    } catch (e) { console.error(e); }
    finally {
      const l = new Set(this.loadingColumns());
      l.delete(tableId);
      this.loadingColumns.set(l);
    }
  }

  // --- ACTIONS & UTILS ---
  hasExistingPrimaryKey(tableId: string, excludeColumnId?: string): boolean {
    const cols = this.columnsMap().get(tableId) || [];
    // Using 'isPrimaryKey' based on DTO update
    return cols.some(c => c.primaryKey && c.columnId !== excludeColumnId);
  }

  copyQuery() {
    navigator.clipboard.writeText(this.currentQuery()).then(() => {
      this.isCopied.set(true);
      setTimeout(() => { this.isCopied.set(false); }, 2000);
    });
  }

  checkGuest() { return this.isGuest(); }
  resetForm() { this.formData = { name: '', description: '', dataType: 'varchar', primaryKey: false, jsonContent: '' }; this.selectedDbId = null; this.selectedTableId = null; this.selectedColumnId = null; }

  openImportModal() { if (this.checkGuest()) return; this.resetForm(); this.importStep.set(1); this.modalMode.set('IMPORT'); this.showModal.set(true); }
  openCreateDatabase() { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_DB'); this.showModal.set(true); }
  openEditDatabase(db: DatabaseDto) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('EDIT_DB'); this.selectedDbId = db.databaseId!; this.formData.name = db.name!; this.formData.description = db.description || ''; this.showModal.set(true); }
  openCreateTable(dbId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_TABLE'); this.selectedDbId = dbId; this.showModal.set(true); }
  openEditTable(table: TableDto, dbId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('EDIT_TABLE'); this.selectedDbId = dbId; this.selectedTableId = table.tableId!; this.formData.name = table.name!; this.formData.description = table.description || ''; this.showModal.set(true); }
  openCreateColumn(dbId: string, tableId: string) { if (this.checkGuest()) return; this.resetForm(); this.modalMode.set('CREATE_COLUMN'); this.selectedDbId = dbId; this.selectedTableId = tableId; this.formData.dataType = 'varchar'; this.showModal.set(true); }
  openEditColumn(col: ColumnDto, dbId: string, tableId: string) {
    if (this.checkGuest()) return;
    this.resetForm();
    this.modalMode.set('EDIT_COLUMN');
    this.selectedDbId = dbId;
    this.selectedTableId = tableId;
    this.selectedColumnId = col.columnId!;
    this.formData.name = col.name!;
    this.formData.dataType = col.dataType!.toLowerCase();
    this.formData.primaryKey = !!col.primaryKey;
    this.showModal.set(true);
  }

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
        await this.api.invoke(createColumn, { databaseId: this.selectedDbId, tableId: this.selectedTableId, body: { name: this.formData.name, dataType: this.formData.dataType, isPrimaryKey: this.formData.primaryKey } });
        this.invalidateCache('COLUMN', this.selectedTableId); await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      else if (mode === 'EDIT_COLUMN' && this.selectedDbId && this.selectedTableId && this.selectedColumnId) {
        if (this.formData.primaryKey && this.hasExistingPrimaryKey(this.selectedTableId, this.selectedColumnId)) { this.openErrorModal("This table already has a Primary Key."); this.isLoading.set(false); return; }
        await this.api.invoke(updateColumn, { databaseId: this.selectedDbId, tableId: this.selectedTableId, columnId: this.selectedColumnId, body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: this.formData.primaryKey } });
        this.invalidateCache('COLUMN', this.selectedTableId); await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      this.closeModal();
    } catch (e: any) { console.error("Op Failed:", e); const msg = this.errorHandler.message(e); this.openErrorModal(msg); } finally { this.isLoading.set(false); }
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
    } catch (e: any) { const msg = this.errorHandler.message(e); this.openErrorModal(msg); } finally { this.isLoading.set(false); }
  }

  private saveToLocal() { if (!this.isGuest()) return; const data = { dbs: this.databases(), tables: Array.from(this.tablesMap().entries()), columns: Array.from(this.columnsMap().entries()) }; localStorage.setItem('guest_schema_data_v2', JSON.stringify(data)); }
  private loadFromLocal() { const raw = localStorage.getItem('guest_schema_data_v2'); if (raw) { const data = JSON.parse(raw); this.databases.set(data.dbs || []); this.tablesMap.set(new Map(data.tables)); this.columnsMap.set(new Map(data.columns)); return true; } return false; }
}
