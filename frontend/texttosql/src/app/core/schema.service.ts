import { Injectable, inject, signal } from '@angular/core';
import { Api } from '../api/api';
import { DatabaseDto } from '../api/models/database-dto';
import { TableDto } from '../api/models/table-dto';
import { ColumnDto } from '../api/models/column-dto';
import { getDatabases, getTables, getColumns } from '../api/functions';

@Injectable({ providedIn: 'root' })
export class SchemaService {
  private readonly api = inject(Api);

  async loadDatabases(params: any): Promise<DatabaseDto[]> {
    const res = await this.api.invoke(getDatabases, params) as any;
    return res.content || [];
  }

  async loadTables(databaseId: string): Promise<TableDto[]> {
    const res = await this.api.invoke(getTables, { databaseId }) as any;
    return Array.isArray(res) ? res : (res.content || []);
  }

  async loadColumns(databaseId: string, tableId: string): Promise<ColumnDto[]> {
    const res = await this.api.invoke(getColumns, { databaseId, tableId }) as any;
    return Array.isArray(res) ? res : (res.content || []);
  }
}
