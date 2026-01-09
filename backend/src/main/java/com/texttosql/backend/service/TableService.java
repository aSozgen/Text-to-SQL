package com.texttosql.backend.service;

import com.texttosql.backend.dto.TableDto;
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

    @Transactional(readOnly = true)
    public List<TableDto> getTables(DatabaseEntity database) {
        List<TableEntity> entities = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(database);
        return tableMapper.toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public TableDto getTable(DatabaseEntity database, UUID tableId) {
        TableEntity entity = getCurrentTableEntity(database, tableId);
        return tableMapper.toDto(entity);
    }

    public TableDto createTable(DatabaseEntity databaseEntity, TableDto tableDTO) {
        if (tableRepository.existsByNameIgnoreCaseAndDatabaseAndActiveTrue(tableDTO.getName(), databaseEntity)) {
            throw new DuplicatedResourceException("There is already a Table with the name '" + tableDTO.getName() + "'");
        }

        TableEntity tableEntity = new TableEntity();
        tableEntity.setDatabase(databaseEntity);
        tableEntity.setName(tableDTO.getName());
        tableEntity.setDescription(tableDTO.getDescription());

        TableEntity savedTableEntity = tableRepository.save(tableEntity);
        tableDTO.setTableId(savedTableEntity.getTableId());

        return tableDTO;
    }

    public TableDto updateTable(DatabaseEntity database, UUID tableId, TableDto tableDTO) {
        TableEntity oldEntity = getCurrentTableEntity(database, tableId);

        if (tableRepository.existsByNameIgnoreCaseAndDatabaseAndActiveTrue(tableDTO.getName(), database)
                && !oldEntity.getName().equalsIgnoreCase(tableDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Table with the name '" + tableDTO.getName() + "'");
        }

        oldEntity.setName(tableDTO.getName());
        oldEntity.setDescription(tableDTO.getDescription());

        tableRepository.save(oldEntity);
        tableDTO.setTableId(oldEntity.getTableId());

        return tableDTO;
    }

    @Transactional
    public void deleteTable(DatabaseEntity database, UUID tableId) {
        TableEntity oldEntity = getCurrentTableEntity(database, tableId);

        oldEntity.setActive(false);
        tableRepository.save(oldEntity);
    }

    public TableEntity getCurrentTableEntity(DatabaseEntity database, UUID tableId) {
        return tableRepository.findByDatabaseAndTableIdAndActiveTrue(database, tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }
}
