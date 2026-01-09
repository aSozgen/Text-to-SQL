package com.texttosql.backend.service;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.entity.ColumnEntity;
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

    public ColumnDto createColumn(TableEntity tableEntity, ColumnDto columnDto) {
        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        ColumnEntity columnEntity = new ColumnEntity();
        columnEntity.setTable(tableEntity);
        columnEntity.setName(columnDto.getName());
        columnEntity.setDataType(columnDto.getDataType());

        ColumnEntity savedColumnEntity = columnRepository.save(columnEntity);
        columnDto.setColumnId(savedColumnEntity.getColumnId());

        return columnDto;
    }

    public ColumnDto updateColumn(TableEntity tableEntity, UUID columnId, ColumnDto columnDto) {
        ColumnEntity oldEntity = getCurrentColumnEntity(tableEntity, columnId);

        if (columnRepository.existsByNameIgnoreCaseAndTableAndActiveTrue(columnDto.getName(), tableEntity)
                && !oldEntity.getName().equalsIgnoreCase(columnDto.getName())) {
            throw new DuplicatedResourceException("There is already a Column with the name '" + columnDto.getName() + "'");
        }

        oldEntity.setName(columnDto.getName());
        oldEntity.setDataType(columnDto.getDataType());

        columnRepository.save(oldEntity);
        columnDto.setColumnId(oldEntity.getColumnId());

        return columnDto;
    }

    @Transactional
    public void deleteColumn(TableEntity tableEntity, UUID columnId) {
        ColumnEntity columnEntity = getCurrentColumnEntity(tableEntity, columnId);

        columnEntity.setActive(false);
        columnRepository.save(columnEntity);
    }

    private ColumnEntity getCurrentColumnEntity(TableEntity tableEntity, UUID columnId) {
        return columnRepository.findByTableAndColumnIdAndActiveTrue(tableEntity, columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
    }
}
