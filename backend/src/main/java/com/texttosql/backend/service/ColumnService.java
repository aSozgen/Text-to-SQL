package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.entity.ColumnEntity;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.ColumnMapper;
import com.texttosql.backend.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ColumnService {
    private final ColumnRepository columnRepository;
    private final ColumnMapper columnMapper;
    private final SchemaVersionService versionService;

    @Transactional(readOnly = true)
    public List<ColumnDto> getColumns(TableEntity tableEntity) {
        List<ColumnEntity> entities = columnRepository.findByTableAndActiveTrueOrderByCreatedAtDesc(tableEntity);
        return columnMapper.toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public ColumnDto getColumn(TableEntity tableEntity, UUID columnId) {
        ColumnEntity entity = getCurrentColumnEntity(tableEntity, columnId);
        return columnMapper.toDto(entity);
    }

    @Transactional
    public ColumnDto createColumn(DatabaseEntity databaseEntity, TableEntity tableEntity, ColumnDto columnDto, boolean versionUsedInMessages) {
        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)) {
            throw new DuplicatedResourceException("There is already a Column with the same name.");
        }

        ColumnEntity columnEntity = new ColumnEntity();
        columnEntity.setTable(tableEntity);
        columnEntity.setName(columnDto.getName());
        columnEntity.setDataType(columnDto.getDataType());
        columnEntity.setPrimaryKey(columnDto.isPrimaryKey());
        columnEntity.setForeignTable(columnDto.getForeignTable());
        columnEntity.setForeignColumn(columnDto.getForeignColumn());

        ColumnEntity savedColumnEntity = columnRepository.save(columnEntity);
        columnDto.setColumnId(savedColumnEntity.getColumnId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion
        versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);

        return columnDto;
    }

    @Transactional
    public ColumnDto updateColumn(DatabaseEntity databaseEntity, TableEntity tableEntity, UUID columnId, ColumnDto columnDto, boolean versionUsedInMessages) {
        ColumnEntity oldEntity = getCurrentColumnEntity(tableEntity, columnId);

        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)
                && !oldEntity.getName().equalsIgnoreCase(columnDto.getName())) {
            throw new DuplicatedResourceException("There is already a Column with the same name.");
        }

        String oldName = oldEntity.getName();
        String oldDataType = oldEntity.getDataType();
        boolean oldPrimaryKey = oldEntity.isPrimaryKey();
        String oldForeignTable = oldEntity.getForeignTable();
        String oldForeignColumn = oldEntity.getForeignColumn();

        oldEntity.setName(columnDto.getName());
        oldEntity.setDataType(columnDto.getDataType());
        oldEntity.setPrimaryKey(columnDto.isPrimaryKey());
        oldEntity.setForeignTable(columnDto.getForeignTable());
        oldEntity.setForeignColumn(columnDto.getForeignColumn());

        columnRepository.save(oldEntity);
        columnDto.setColumnId(oldEntity.getColumnId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion iff Column name/datatype/primaryKey/foreigns has changed (Column description doesn't matter)
        if (!oldName.equalsIgnoreCase(columnDto.getName()) || !oldDataType.equalsIgnoreCase(columnDto.getDataType())
                || oldPrimaryKey != columnDto.isPrimaryKey() || !oldForeignTable.equalsIgnoreCase(columnDto.getForeignTable())
                || !oldForeignColumn.equalsIgnoreCase(columnDto.getForeignColumn())) {
            versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);
        }

        return columnDto;
    }

    @Transactional
    public void deleteColumn(DatabaseEntity databaseEntity, TableEntity tableEntity, UUID columnId, boolean versionUsedInMessages) {
        ColumnEntity columnEntity = getCurrentColumnEntity(tableEntity, columnId);

        columnEntity.setActive(false);
        columnRepository.save(columnEntity);

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion
        versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);
    }

    private ColumnEntity getCurrentColumnEntity(TableEntity tableEntity, UUID columnId) {
        return columnRepository.findByTableAndColumnIdAndActiveTrue(tableEntity, columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found."));
    }
}
