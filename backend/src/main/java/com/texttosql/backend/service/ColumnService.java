package com.texttosql.backend.service;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.entity.ColumnEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.NotResourceOwnerException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.ColumnRepository;
import com.texttosql.backend.util.SecurityUtil;
import jakarta.validation.Valid;
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
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<ColumnDto> getColumns(TableEntity tableEntity) {
        checkResourceOwner(tableEntity.getDatabaseId());

        List<ColumnEntity> columnEntities = columnRepository.findByTableIdOrderByCreatedAtDesc(tableEntity);
        return columnEntities.stream()
                .map(entity -> new ColumnDto(entity.getColumnId(), entity.getName(), entity.getDataType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ColumnDto getColumn(UUID columnId) {
        ColumnEntity columnEntity = getCurrentColumnEntity(columnId);
        checkResourceOwner(columnEntity.getTableId().getDatabaseId());

        return new ColumnDto(columnEntity.getColumnId(), columnEntity.getName(), columnEntity.getDataType());
    }


    public ColumnDto createColumn(TableEntity tableEntity, ColumnDto columnDto) {
        checkResourceOwner(tableEntity.getDatabaseId());

        if (columnRepository.existsByNameIgnoreCaseAndTableId(columnDto.getName(), tableEntity)) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        ColumnEntity columnEntity = new ColumnEntity();
        columnEntity.setTableId(tableEntity);
        columnEntity.setName(columnDto.getName());
        columnEntity.setDataType(columnDto.getDataType());

        ColumnEntity savedColumnEntity = columnRepository.save(columnEntity);
        columnDto.setColumnId(savedColumnEntity.getColumnId());
        return columnDto;
    }

    public ColumnDto updateColumn(TableEntity tableEntity, UUID columnId, ColumnDto columnDto) {
        checkResourceOwner(tableEntity.getDatabaseId());
        ColumnEntity oldEntity = getCurrentColumnEntity(columnId);
        checkResourceOwner(oldEntity.getTableId().getDatabaseId());

        if (columnRepository.existsByNameIgnoreCaseAndTableId(columnDto.getName(), tableEntity)
                && !oldEntity.getName().equalsIgnoreCase(columnDto.getName())) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        oldEntity.setName(columnDto.getName());
        oldEntity.setDataType(columnDto.getDataType());

        ColumnEntity updatedEntity = columnRepository.save(oldEntity);
        columnDto.setColumnId(updatedEntity.getColumnId());
        return columnDto;
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        ColumnEntity columnEntity = getCurrentColumnEntity(columnId);
        checkResourceOwner(columnEntity.getTableId().getDatabaseId());

        columnRepository.delete(columnEntity);
    }

    private ColumnEntity getCurrentColumnEntity(UUID columnId) {
        return columnRepository.findByColumnId(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
    }

    private void checkResourceOwner(DatabaseEntity databaseEntity) {
        if (!securityUtil.isResourceOwner(databaseEntity.getUserId().getUserId())) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
