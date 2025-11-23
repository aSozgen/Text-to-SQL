package com.texttosql.backend.service;

import com.texttosql.backend.dto.TableDto;
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
    public List<TableDto> getTables(DatabaseEntity databaseEntity) {
        checkResourceOwner(databaseEntity);

        List<TableEntity> tableEntities = tableRepository.findByDatabaseIdOrderByCreatedAtDesc(databaseEntity);
        return tableEntities.stream()
                .map(entity -> new TableDto(entity.getTableId(), entity.getName(), entity.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TableDto getTable(UUID tableId) {
        TableEntity tableEntity = getCurrentTableEntity(tableId);
        checkResourceOwner(tableEntity.getDatabaseId());

        return new TableDto(tableEntity.getTableId(), tableEntity.getName(), tableEntity.getDescription());
    }


    public @Valid TableDto createTable(DatabaseEntity databaseEntity, TableDto tableDTO) {
        checkResourceOwner(databaseEntity);

        if (tableRepository.existsByNameIgnoreCaseAndDatabaseId(tableDTO.getName(), databaseEntity)) {
            throw new DuplicatedResourceException("There is already a Table with the name '" + tableDTO.getName() + "'");
        }

        TableEntity tableEntity = new TableEntity();
        tableEntity.setDatabaseId(databaseEntity);
        tableEntity.setName(tableDTO.getName());
        tableEntity.setDescription(tableDTO.getDescription());

        TableEntity savedTableEntity = tableRepository.save(tableEntity);
        tableDTO.setTableId(savedTableEntity.getTableId());
        return tableDTO;
    }

    public @Valid TableDto updateTable(DatabaseEntity databaseEntity, UUID tableId, TableDto tableDTO) {
        TableEntity oldEntity = getCurrentTableEntity(tableId);

        if (tableRepository.existsByNameIgnoreCaseAndDatabaseId(tableDTO.getName(), databaseEntity)
                && !oldEntity.getName().equalsIgnoreCase(tableDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Table with the name '" + tableDTO.getName() + "'");
        }

        checkResourceOwner(oldEntity.getDatabaseId());

        oldEntity.setName(tableDTO.getName());
        oldEntity.setDescription(tableDTO.getDescription());

        TableEntity savedEntity = tableRepository.save(oldEntity);
        tableDTO.setTableId(savedEntity.getTableId());
        return tableDTO;
    }

    @Transactional
    public void deleteTable(UUID tableId) {
        TableEntity tableEntity = getCurrentTableEntity(tableId);
        checkResourceOwner(tableEntity.getDatabaseId());

        tableRepository.delete(tableEntity);
    }

    public TableEntity getCurrentTableEntity(UUID tableId) {
        return tableRepository.findByTableId(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }

    private void checkResourceOwner(DatabaseEntity databaseEntity) {
        if (!securityUtil.isResourceOwner(databaseEntity.getUserId().getUserId())) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
