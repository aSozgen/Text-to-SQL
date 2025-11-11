package com.texttosql.backend.service;

import com.texttosql.backend.dto.TableDTO;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.NotResourceOwnerException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.TableRepository;
import com.texttosql.backend.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {
    private final TableRepository tableRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<TableDTO> getTables(DatabaseEntity databaseEntity) {
        List<TableEntity> tableEntities = tableRepository.findByDatabaseIdOrderByCreatedAtDesc(databaseEntity);
        return tableEntities.stream()
                .map(entity -> new TableDTO(entity.getTableId(), entity.getName(), entity.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TableDTO getTable(UUID tableId) {
        TableEntity tableEntity = getCurrentTableEntity(tableId);
        return new TableDTO(tableEntity.getTableId(), tableEntity.getName(), tableEntity.getDescription());
    }


    public @Valid TableDTO createTable(DatabaseEntity databaseEntity, TableDTO tableDTO) {
        if (tableRepository.existsByNameIgnoreCaseAndDatabaseId(tableDTO.getName(), databaseEntity)) {
            throw new DuplicatedResourceException("There is already a table with the name '" + tableDTO.getName() + "'");
        }

        TableEntity tableEntity = new TableEntity();
        tableEntity.setDatabaseId(databaseEntity);
        tableEntity.setName(tableDTO.getName());
        tableEntity.setDescription(tableDTO.getDescription());

        TableEntity savedTableEntity = tableRepository.save(tableEntity);
        tableDTO.setTableId(savedTableEntity.getTableId());
        return tableDTO;
    }

    public @Valid TableDTO updateTable(DatabaseEntity databaseEntity, UUID tableId, TableDTO tableDTO) {
        TableEntity oldEntity = getCurrentTableEntity(tableId);

        if (tableRepository.existsByNameIgnoreCaseAndDatabaseId(tableDTO.getName(), databaseEntity)
                && !oldEntity.getName().equalsIgnoreCase(tableDTO.getName())) {
            throw new DuplicatedResourceException("Table name already exists");
        }

        checkDatabaseOwner(oldEntity.getDatabaseId());

        oldEntity.setName(tableDTO.getName());
        oldEntity.setDescription(tableDTO.getDescription());

        TableEntity savedEntity = tableRepository.save(oldEntity);
        tableDTO.setTableId(savedEntity.getTableId());
        return tableDTO;
    }

    @Transactional
    public void deleteTable(UUID tableId) {
        TableEntity tableEntity = getCurrentTableEntity(tableId);
        checkDatabaseOwner(tableEntity.getDatabaseId());

        tableRepository.delete(tableEntity);
    }

    private TableEntity getCurrentTableEntity(UUID tableId) {
        return tableRepository.findByTableId(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }

    private void checkDatabaseOwner(DatabaseEntity database) {
        if (!securityUtil.isResourceOwner(database.getUserId().getUserId())) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
