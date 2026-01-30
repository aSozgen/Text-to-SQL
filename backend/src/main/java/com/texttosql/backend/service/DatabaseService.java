package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.DatabaseDto;
import com.texttosql.backend.entity.*;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.DatabaseMapper;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.ColumnRepository;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.repository.TableRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class DatabaseService {
    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final DatabaseMapper databaseMapper;
    private final UserMapper userMapper;
    private final SchemaVersionService versionService;

    @Transactional(readOnly = true)
    public Page<DatabaseDto> getDatabases(CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<DatabaseEntity> entities = databaseRepository.findByUserAndActiveTrue(userMapper.toEntity(userDetails), pageable);
        return entities.map(databaseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DatabaseDto getDatabase(UUID databaseId, CustomUserDetails userDetails) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));
        return databaseMapper.toDto(entity);
    }

    @Transactional
    public DatabaseDto createDatabase(DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        if (databaseRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(databaseDTO.getName(), userMapper.toEntity(userDetails))) {
            throw new DuplicatedResourceException("There is already a Database with the same name.");
        }

        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setUser(userMapper.toEntity(userDetails));
        databaseEntity.setName(databaseDTO.getName());
        databaseEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedDatabaseEntity = databaseRepository.save(databaseEntity);
        databaseDTO.setDatabaseId(savedDatabaseEntity.getDatabaseId());

        // Create an initial SchemaVersion
        SchemaVersionEntity schemaVersion = SchemaVersionEntity.builder()
                .database(savedDatabaseEntity)
                .build();
        versionService.createSchemaVersion(schemaVersion);

        return databaseDTO;
    }

    @Transactional
    public DatabaseDto updateDatabase(UUID databaseId, DatabaseDto databaseDTO, CustomUserDetails userDetails, boolean versionUsedInMessages) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));

        if (databaseRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(databaseDTO.getName(), userMapper.toEntity(userDetails))
                && !entity.getName().equalsIgnoreCase(databaseDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Database with the same name.");
        }

        String oldName = entity.getName();
        entity.setName(databaseDTO.getName());
        entity.setDescription(databaseDTO.getDescription());

        // If the current SchemaVersion is not used in any message, don't create a new SchemaVersion just update the existing one
        // Else create a new SchemaVersion iff Database name has changed (Database description doesn't matter)
        if (!oldName.equalsIgnoreCase(databaseDTO.getName())) {
            versionService.createOrUpdateSchemaSnapshot(entity, versionUsedInMessages);
        } else {
            databaseRepository.save(entity);
        }

        return databaseDTO;
    }

    @Transactional
    public void deleteDatabase(UUID databaseId, CustomUserDetails userDetails) {
        DatabaseEntity databaseEntity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));

        List<TableEntity> tables = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(databaseEntity);

        for (TableEntity table : tables) {
            List<ColumnEntity> columns = columnRepository.findByTableAndActiveTrueOrderByCreatedAtDesc(table);
            if (!columns.isEmpty()) {
                columns.forEach(c -> c.setActive(false));
                columnRepository.saveAll(columns);
            }

            table.setActive(false);
        }
        tableRepository.saveAll(tables);

        databaseEntity.setActive(false);
        databaseRepository.save(databaseEntity);
    }

    @Transactional(readOnly = true)
    public DatabaseEntity getCurrentDatabaseEntity(UUID databaseId, UserEntity user) {
        return databaseRepository.findByUserAndDatabaseIdAndActiveTrue(user, databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found."));
    }

}
