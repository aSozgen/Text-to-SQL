package com.texttosql.backend.service;

import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.DatabaseMapper;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
    private final DatabaseMapper databaseMapper;
    private final UserMapper  userMapper;

    @Transactional(readOnly = true)
    public List<DatabaseDto> getDatabases(CustomUserDetails userDetails) {
        List<DatabaseEntity> entities = databaseRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(userMapper.toEntity(userDetails));
        return databaseMapper.toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public DatabaseDto getDatabase(UUID databaseId, CustomUserDetails userDetails) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));
        return databaseMapper.toDto(entity);
    }

    public DatabaseDto createDatabase(DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        if (databaseRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(databaseDTO.getName(), userMapper.toEntity(userDetails))) {
            throw new DuplicatedResourceException("There is already a Database with the name '" + databaseDTO.getName() + "'");
        }

        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setUser(userMapper.toEntity(userDetails));
        databaseEntity.setName(databaseDTO.getName());
        databaseEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedEntity = databaseRepository.save(databaseEntity);
        databaseDTO.setDatabaseId(savedEntity.getDatabaseId());
        return databaseDTO;
    }

    public DatabaseDto updateDatabase(UUID databaseId, DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        DatabaseEntity oldEntity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));

        if (databaseRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(databaseDTO.getName(), userMapper.toEntity(userDetails))
                && !oldEntity.getName().equalsIgnoreCase(databaseDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Database with the name '" + databaseDTO.getName() + "'");
        }

        oldEntity.setName(databaseDTO.getName());
        oldEntity.setDescription(databaseDTO.getDescription());

        databaseRepository.save(oldEntity);
        databaseDTO.setDatabaseId(oldEntity.getDatabaseId());
        return databaseDTO;
    }

    @Transactional
    public void deleteDatabase(UUID databaseId, CustomUserDetails userDetails) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));

        entity.setActive(false);
        databaseRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public DatabaseEntity getCurrentDatabaseEntity(UUID databaseId, UserEntity user) {
        return databaseRepository.findByUserAndDatabaseIdAndActiveTrue(user, databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found"));
    }

}
