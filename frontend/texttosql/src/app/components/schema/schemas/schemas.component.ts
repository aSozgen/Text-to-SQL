import { Component, inject, signal, WritableSignal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

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

  databases: WritableSignal<DatabaseDto[]> = signal([]);
  isLoading = signal(false);
  expandedDbIds = signal<Set<string>>(new Set());
  expandedTableIds = signal<Set<string>>(new Set());

  tablesMap = signal<Map<string, TableDto[]>>(new Map());
  columnsMap = signal<Map<string, ColumnDto[]>>(new Map());

  loadingTables = signal<Set<string>>(new Set());
  loadingColumns = signal<Set<string>>(new Set());

  searchQuery = signal('');
  page = 0; size = 10; sort = 'createdAt'; direction = 'desc';

  showModal = signal(false);
  modalMode = signal<ModalMode>('IMPORT');

  selectedDbId: string | null = null;
  selectedTableId: string | null = null;
  selectedColumnId: string | null = null;

  formData = { name: '', description: '', dataType: 'VARCHAR', jsonContent: '' };

  constructor() {
    this.loadSchemas();
  }

  // --- READ ---
  async loadSchemas() {
    this.isLoading.set(true);
    if (this.isGuest()) {
      const mockDbs: DatabaseDto[] = [
        { databaseId: 'mock-1', name: 'Demo_Store', description: 'Read-only demo data', createdAt: '25.01.2026' },
        { databaseId: 'mock-2', name: 'Analytics_DB', description: 'Log data', createdAt: '20.02.2026' }
      ];
      setTimeout(() => { this.databases.set(mockDbs); this.isLoading.set(false); }, 500);
      return;
    }

    try {
      const response = await this.api.invoke(getDatabases, { page: this.page, size: this.size, sort: this.sort, direction: this.direction });
      this.databases.set(response.content || []);
    } catch (e) { console.error(e); } finally { this.isLoading.set(false); }
  }

  async toggleDatabase(dbId: string | undefined) {
    if (!dbId) return;
    const expanded = new Set(this.expandedDbIds());
    if (expanded.has(dbId)) expanded.delete(dbId);
    else { expanded.add(dbId); if (!this.tablesMap().has(dbId)) await this.loadTablesForDb(dbId); }
    this.expandedDbIds.set(expanded);
  }

  async loadTablesForDb(dbId: string) {
    if (this.isGuest()) {
      const newMap = new Map(this.tablesMap());
      newMap.set(dbId, [{ tableId: 't1', name: 'users', description: 'User table' }]);
      this.tablesMap.set(newMap);
      return;
    }
    const loading = new Set(this.loadingTables()); loading.add(dbId); this.loadingTables.set(loading);
    try {
      const res = await this.api.invoke(getTables, { databaseId: dbId, page: 0, size: 100 });
      const newMap = new Map(this.tablesMap()); newMap.set(dbId, res.content || []); this.tablesMap.set(newMap);
    } catch (e) { console.error(e); } finally { const l = new Set(this.loadingTables()); l.delete(dbId); this.loadingTables.set(l); }
  }

  async toggleTable(dbId: string | undefined, tableId: string | undefined) {
    if (!dbId || !tableId) return;
    const expanded = new Set(this.expandedTableIds());
    if (expanded.has(tableId)) expanded.delete(tableId);
    else { expanded.add(tableId); if (!this.columnsMap().has(tableId)) await this.loadColumnsForTable(dbId, tableId); }
    this.expandedTableIds.set(expanded);
  }

  async loadColumnsForTable(dbId: string, tableId: string) {
    if (this.isGuest()) {
      const newMap = new Map(this.columnsMap());
      newMap.set(tableId, [{ columnId: 'c1', name: 'id', dataType: 'INTEGER', primaryKey: true }]);
      this.columnsMap.set(newMap);
      return;
    }
    const loading = new Set(this.loadingColumns()); loading.add(tableId); this.loadingColumns.set(loading);
    try {
      const res = await this.api.invoke(getColumns, { databaseId: dbId, tableId: tableId, page: 0, size: 100 });
      const newMap = new Map(this.columnsMap()); newMap.set(tableId, res.content || []); this.columnsMap.set(newMap);
    } catch (e) { console.error(e); } finally { const l = new Set(this.loadingColumns()); l.delete(tableId); this.loadingColumns.set(l); }
  }

  // --- ACTIONS ---
  checkGuest() { if (this.isGuest()) { return true; } return false; }

  // MODAL OPENERS
  openImportModal() { if (this.checkGuest()) return; this.modalMode.set('IMPORT'); this.resetForm(); this.showModal.set(true); }

  openCreateDatabase() { if (this.checkGuest()) return; this.modalMode.set('CREATE_DB'); this.resetForm(); this.showModal.set(true); }

  openEditDatabase(db: DatabaseDto) {
    if (this.checkGuest()) return;
    this.modalMode.set('EDIT_DB');
    this.selectedDbId = db.databaseId || null;
    this.formData = { name: db.name, description: db.description || '', dataType: '', jsonContent: '' };
    this.showModal.set(true);
  }

  openCreateTable(dbId: string) { if (this.checkGuest()) return; this.modalMode.set('CREATE_TABLE'); this.selectedDbId = dbId; this.resetForm(); this.showModal.set(true); }

  openEditTable(table: TableDto, dbId: string) {
    if (this.checkGuest()) return;
    this.modalMode.set('EDIT_TABLE');
    this.selectedDbId = dbId;
    this.selectedTableId = table.tableId || null;
    this.formData = { name: table.name, description: table.description || '', dataType: '', jsonContent: '' };
    this.showModal.set(true);
  }

  openCreateColumn(dbId: string, tableId: string) { if (this.checkGuest()) return; this.modalMode.set('CREATE_COLUMN'); this.selectedDbId = dbId; this.selectedTableId = tableId; this.resetForm(); this.formData.dataType = 'VARCHAR'; this.showModal.set(true); }

  openEditColumn(col: ColumnDto, dbId: string, tableId: string) {
    if (this.checkGuest()) return;
    this.modalMode.set('EDIT_COLUMN');
    this.selectedDbId = dbId; this.selectedTableId = tableId;
    this.selectedColumnId = col.columnId || null;
    this.formData = { name: col.name, description: '', dataType: col.dataType, jsonContent: '' };
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); this.resetForm(); }
  resetForm() { this.formData = { name: '', description: '', dataType: 'VARCHAR', jsonContent: '' }; this.selectedDbId = null; this.selectedTableId = null; this.selectedColumnId = null; }

  // --- SUBMIT LOGIC ---
  async submitModal() {
    if (this.checkGuest()) return;
    this.isLoading.set(true);
    const mode = this.modalMode();

    try {
      if (mode === 'IMPORT') {
        const importBody = {
          name: this.formData.name,
          description: this.formData.description,
          jsonContent: JSON.parse(this.formData.jsonContent)
        };
        await this.api.invoke(importSchema, { body: importBody });
        await this.loadSchemas();
      }

      // DATABASE OPS
      else if (mode === 'CREATE_DB') {
        const dbBody: DatabaseDto = { name: this.formData.name, description: this.formData.description };
        await this.api.invoke(createDatabase, { body: dbBody });
        await this.loadSchemas();
      }
      else if (mode === 'EDIT_DB' && this.selectedDbId) {
        const dbBody: DatabaseDto = { name: this.formData.name, description: this.formData.description };
        await this.api.invoke(updateDatabase, { databaseId: this.selectedDbId, body: dbBody });
        await this.loadSchemas();
      }

      // TABLE OPS (CORRECTED)
      else if (mode === 'CREATE_TABLE' && this.selectedDbId) {
        // Correct structure: { databaseId, body: { ... } }
        await this.api.invoke(createTable, {
          databaseId: this.selectedDbId,
          body: { name: this.formData.name, description: this.formData.description }
        });
        await this.loadTablesForDb(this.selectedDbId);
      }
      else if (mode === 'EDIT_TABLE' && this.selectedDbId && this.selectedTableId) {
        await this.api.invoke(updateTable, {
          databaseId: this.selectedDbId,
          tableId: this.selectedTableId,
          body: { name: this.formData.name, description: this.formData.description }
        });
        await this.loadTablesForDb(this.selectedDbId);
      }

      // COLUMN OPS
      else if (mode === 'CREATE_COLUMN' && this.selectedDbId && this.selectedTableId) {
        await this.api.invoke(createColumn, {
          databaseId: this.selectedDbId,
          tableId: this.selectedTableId,
          body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: false }
        });
        await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }
      else if (mode === 'EDIT_COLUMN' && this.selectedDbId && this.selectedTableId && this.selectedColumnId) {
        const existingCol = this.columnsMap().get(this.selectedTableId)?.find(c => c.columnId === this.selectedColumnId);
        await this.api.invoke(updateColumn, {
          databaseId: this.selectedDbId,
          tableId: this.selectedTableId,
          columnId: this.selectedColumnId,
          body: { name: this.formData.name, dataType: this.formData.dataType, primaryKey: existingCol?.primaryKey || false }
        });
        await this.loadColumnsForTable(this.selectedDbId, this.selectedTableId);
      }

      this.closeModal();
    } catch (e) {
      console.error("Operation Failed:", e);
      alert("Action failed. Please check console.");
    } finally {
      this.isLoading.set(false);
    }
  }

  // --- DELETE & OTHER ---
  async togglePrimaryKey(dbId: string, tableId: string, column: ColumnDto) {
    if (this.checkGuest()) return;
    if (column.primaryKey) return;
    if(!confirm(`Set '${column.name}' as Primary Key?`)) return;

    try {
      await this.api.invoke(updateColumn, {
        databaseId: dbId, tableId: tableId, columnId: column.columnId!,
        body: { name: column.name, dataType: column.dataType, primaryKey: true }
      });
      await this.loadColumnsForTable(dbId, tableId);
    } catch (e) { alert("Failed to set Primary Key"); }
  }

  async deleteItem(type: 'DB'|'TABLE'|'COLUMN', id: string, pid?: string, gpid?: string) {
    if (this.checkGuest() || !confirm("Are you sure?")) return;
    try {
      if (type === 'DB') { await this.api.invoke(deleteDatabase, { databaseId: id }); await this.loadSchemas(); }
      else if (type === 'TABLE' && pid) { await this.api.invoke(deleteTable, { databaseId: pid, tableId: id }); await this.loadTablesForDb(pid); }
      else if (type === 'COLUMN' && pid && gpid) { await this.api.invoke(deleteColumn, { databaseId: gpid, tableId: pid, columnId: id }); await this.loadColumnsForTable(gpid, pid); }
    } catch (e) { alert("Delete failed"); }
  }

  async onSearch() { console.log("Searching:", this.searchQuery()); }
}
