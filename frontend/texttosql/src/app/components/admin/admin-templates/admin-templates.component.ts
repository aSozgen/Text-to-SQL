import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SchemasComponent } from '../../schemas/schemas.component';
import {getTemplates, createDatabase, updateDatabase, importSchema} from '../../../api/functions';
import { DatabaseDto } from '../../../api/models/database-dto';
import { TableDto } from '../../../api/models/table-dto';
import { ColumnDto } from '../../../api/models/column-dto';

@Component({
  selector: 'app-admin-templates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: '../../schemas/schemas.component.html', // Reuse HTML
  styleUrl: '../../schemas/schemas.component.scss' // Reuse styles
})
export class AdminTemplatesComponent extends SchemasComponent implements OnInit {

  ngOnInit(): void {
    // Override the base class ngOnInit to load templates instead
    this.loadTemplates();
  }

  // Override loadSchemas to only load templates
  override async loadSchemas(): Promise<void> {
    await this.loadTemplates();
  }

  private async loadTemplates(): Promise<void> {
    this.isLoading.set(true); 
    try {
      const response = await this.api.invoke(getTemplates);
      if (response.databases) {
        this.databases.set(response.databases);
        this.totalElements.set(response.databases.length);

        // Reset and populate maps
        const tMap = new Map<string, TableDto[]>();
        if (response.tables && response.tables.length > 0) {
            for (const table of response.tables) {
                if (table.databaseId) {
                    if (!tMap.has(table.databaseId)) tMap.set(table.databaseId, []);
                    tMap.get(table.databaseId)!.push(table);
                }
            }
        }
        this.tablesMap.set(tMap);

        const cMap = new Map<string, ColumnDto[]>();
        if (response.columns && response.columns.length > 0) {
            for (const col of response.columns) {
                if (col.tableId) {
                    if (!cMap.has(col.tableId)) cMap.set(col.tableId, []);
                    cMap.get(col.tableId)!.push(col);
                }
            }
        }
        this.columnsMap.set(cMap);
      }
    } catch (e: any) {
      this.errorMessage.set(this.errorHandler.message(e)); 
    } finally {
      this.isLoading.set(false);
    }
  }
  // Override the form submission logic for DB creation/editing to set isTemplate
  override async submitModal(): Promise<void> {
    if (this.modalMode() === 'CREATE_DB') {
      const error = this.validateForm(this.modalMode());
      if (error) { this.openErrorModal(error); return; }
      this.isLoading.set(true);

      try {
        const dto = {
            name: this.formData.name,
            description: this.formData.description,
            isTemplate: true // Force template flag
        };
        const newDb = await this.api.invoke(createDatabase, { body: dto, Authorization: '' }); // Fixed function reference
        this.databases.update(dbs => [newDb, ...dbs]);
        this.closeModal();
      } catch (e: any) {
        this.openErrorModal(this.errorHandler.message(e));
      } finally {
        this.isLoading.set(false);
      }
    } else if (this.modalMode() === 'EDIT_DB') {
      const error = this.validateForm(this.modalMode());
      if (error) { this.openErrorModal(error); return; }
      this.isLoading.set(true);

      try {
        const dto = {
            name: this.formData.name,
            description: this.formData.description,
            isTemplate: true // Keep template flag
        };
        const updatedDb = await this.api.invoke(updateDatabase, { // Fixed function reference
          databaseId: this.selectedDbId!, // Fixed property name
          body: dto,
          Authorization: ''
        });
        this.databases.update(dbs => dbs.map(d => d.databaseId === updatedDb.databaseId ? updatedDb : d));
        this.closeModal();
      } catch (e: any) {
        this.openErrorModal(this.errorHandler.message(e));
      } finally {
        this.isLoading.set(false);
      }
    } else if (this.modalMode() === 'IMPORT') {
      const error = this.validateForm(this.modalMode());
      if (error) { this.openErrorModal(error); return; }
      this.isLoading.set(true);

      try {
        await this.api.invoke(importSchema, {
            body: {
                name: this.formData.name,
                description: this.formData.description,
                jsonContent: JSON.parse(this.formData.jsonContent),
                isTemplate: true // Force template flag for admin import
            }
        });
        this.invalidateCache('DB');
        await this.loadSchemas();
        this.closeModal();
      } catch (e: any) {
        this.openErrorModal(this.errorHandler.message(e));
      } finally {
        this.isLoading.set(false);
      }
    } else {
      // For all other cases (Tables, Columns), use the standard logic
      super.submitModal();
    }
  }
}
