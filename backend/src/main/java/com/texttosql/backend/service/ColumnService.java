package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.entity.ColumnEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.ColumnMapper;
import com.texttosql.backend.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ColumnService {
    private final ColumnRepository columnRepository;
    private final ColumnMapper columnMapper;
    private final SchemaVersionService versionService;

    @Transactional(readOnly = true)
    public Page<ColumnDto> getColumns(TableEntity tableEntity, int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<ColumnEntity> entities = columnRepository.findByTableAndActiveTrue(tableEntity, pageable);
        return entities.map(columnMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ColumnDto getColumn(TableEntity tableEntity, UUID columnId) {
        ColumnEntity entity = getCurrentColumnEntity(tableEntity, columnId);
        return columnMapper.toDto(entity);
    }

    @Transactional
    public ColumnDto createColumn(DatabaseEntity databaseEntity, TableEntity tableEntity, ColumnDto columnDto, boolean versionUsedInMessages) {
        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        ColumnEntity columnEntity = new ColumnEntity();
        columnEntity.setTable(tableEntity);
        columnEntity.setName(columnDto.getName());
        columnEntity.setDataType(columnDto.getDataType());

        ColumnEntity savedColumnEntity = columnRepository.save(columnEntity);
        columnDto.setColumnId(savedColumnEntity.getColumnId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update existing one
        // Else create a new SchemaVersion
        versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);

        return columnDto;
    }

    @Transactional
    public ColumnDto updateColumn(DatabaseEntity databaseEntity, TableEntity tableEntity, UUID columnId, ColumnDto columnDto, boolean versionUsedInMessages) {
        ColumnEntity oldEntity = getCurrentColumnEntity(tableEntity, columnId);

        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)
                && !oldEntity.getName().equalsIgnoreCase(columnDto.getName())) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        String oldName = oldEntity.getName();
        String oldDataType = oldEntity.getDataType();
        oldEntity.setName(columnDto.getName());
        oldEntity.setDataType(columnDto.getDataType());

        columnRepository.save(oldEntity);
        columnDto.setColumnId(oldEntity.getColumnId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion iff Column name/datatype has changed (Column description doesn't matter)
        if (!oldName.equalsIgnoreCase(columnDto.getName()) || !oldDataType.equalsIgnoreCase(columnDto.getDataType())) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
    }
}
