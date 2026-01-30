package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.TableDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.TableMapper;
import com.texttosql.backend.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class TableService {
    private final TableRepository tableRepository;
    private final TableMapper tableMapper;
    private final SchemaVersionService versionService;

    @Transactional(readOnly = true)
    public List<TableDto> getTables(DatabaseEntity databaseEntity) {
        List<TableEntity> entities = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(databaseEntity);
        return tableMapper.toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public TableDto getTable(DatabaseEntity databaseEntity, UUID tableId) {
        TableEntity entity = getCurrentTableEntity(databaseEntity, tableId);
        return tableMapper.toDto(entity);
    }

    @Transactional
    public TableDto createTable(DatabaseEntity databaseEntity, TableDto tableDTO, boolean versionUsedInMessages) {
        if (tableRepository.existsByNameIgnoreCaseAndDatabaseAndActiveTrue(tableDTO.getName(), databaseEntity)) {
            throw new DuplicatedResourceException("There is already a Table with the same name.");
        }

        TableEntity tableEntity = new TableEntity();
        tableEntity.setDatabase(databaseEntity);
        tableEntity.setName(tableDTO.getName());
        tableEntity.setDescription(tableDTO.getDescription());

        TableEntity savedTableEntity = tableRepository.save(tableEntity);
        tableDTO.setTableId(savedTableEntity.getTableId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion
        versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);

        return tableDTO;
    }

    @Transactional
    public TableDto updateTable(DatabaseEntity databaseEntity, UUID tableId, TableDto tableDTO, boolean versionUsedInMessages) {
        TableEntity oldEntity = getCurrentTableEntity(databaseEntity, tableId);

        if (tableRepository.existsByNameIgnoreCaseAndDatabaseAndActiveTrue(tableDTO.getName(), databaseEntity)
                && !oldEntity.getName().equalsIgnoreCase(tableDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Table with the same name.");
        }

        String oldName = oldEntity.getName();
        oldEntity.setName(tableDTO.getName());
        oldEntity.setDescription(tableDTO.getDescription());

        tableRepository.save(oldEntity);
        tableDTO.setTableId(oldEntity.getTableId());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion iff Table name has changed (Table description doesn't matter)
        if (!oldName.equalsIgnoreCase(tableDTO.getName())) {
            versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);
        }

        return tableDTO;
    }

    @Transactional
    public void deleteTable(DatabaseEntity databaseEntity, UUID tableId, boolean versionUsedInMessages) {
        TableEntity oldEntity = getCurrentTableEntity(databaseEntity, tableId);

        oldEntity.setActive(false);
        tableRepository.save(oldEntity);

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion
        versionService.createOrUpdateSchemaSnapshot(databaseEntity, versionUsedInMessages);
    }

    public TableEntity getCurrentTableEntity(DatabaseEntity database, UUID tableId) {
        return tableRepository.findByDatabaseAndTableIdAndActiveTrue(database, tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));
    }
}
