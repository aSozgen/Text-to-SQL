package com.texttosql.backend.service;

import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.NotResourceOwnerException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<DatabaseDto> getDatabases() {
        final UserEntity currentUser = getCurrentUserEntity();
        List<DatabaseEntity> databaseEntities = databaseRepository.findByUserIdOrderByCreatedAtDesc(currentUser);
        databaseEntities.removeIf(databaseEntity -> !securityUtil.isResourceOwner(databaseEntity.getUserId().getUserId()));

        return databaseEntities.stream()
                .map(entity -> new DatabaseDto(entity.getDatabaseId(),entity.getName(), entity.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DatabaseDto getDatabase(UUID databaseId) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId);
        checkResourceOwner(entity);

        return new DatabaseDto(entity.getDatabaseId(), entity.getName(), entity.getDescription());
    }


    public DatabaseDto createDatabase(DatabaseDto databaseDTO) {
        final UserEntity currentUser = getCurrentUserEntity();

        if (databaseRepository.existsByNameIgnoreCaseAndUserId(databaseDTO.getName(), currentUser)) {
            throw new DuplicatedResourceException("There is already a Database with the name '" + databaseDTO.getName() + "'");
        }

        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setUserId(currentUser);
        databaseEntity.setName(databaseDTO.getName());
        databaseEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedEntity = databaseRepository.save(databaseEntity);
        databaseDTO.setDatabaseId(savedEntity.getDatabaseId());
        return databaseDTO;
    }

    public DatabaseDto updateDatabase(UUID databaseId, DatabaseDto databaseDTO) {
        final UserEntity currentUser = getCurrentUserEntity();
        DatabaseEntity oldEntity = getCurrentDatabaseEntity(databaseId);
        checkResourceOwner(oldEntity);

        if (databaseRepository.existsByNameIgnoreCaseAndUserId(databaseDTO.getName(), currentUser)
                && !oldEntity.getName().equalsIgnoreCase(databaseDTO.getName())) {
            throw new DuplicatedResourceException("There is already a Database with the name '" + databaseDTO.getName() + "'");
        }

        oldEntity.setName(databaseDTO.getName());
        oldEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedEntity = databaseRepository.save(oldEntity);
        databaseDTO.setDatabaseId(savedEntity.getDatabaseId());
        return databaseDTO;
    }

    @Transactional
    public void deleteDatabase(UUID databaseId) {
        DatabaseEntity databaseEntity = getCurrentDatabaseEntity(databaseId);
        checkResourceOwner(databaseEntity);

        databaseRepository.delete(databaseEntity);
    }

    private UserEntity getCurrentUserEntity() {
        return securityUtil.getCurrentUserEntity()
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public DatabaseEntity getCurrentDatabaseEntity(UUID databaseId) {
        return databaseRepository.findByDatabaseId(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found"));
    }

    private void checkResourceOwner(DatabaseEntity databaseEntity) {
        if (!securityUtil.isResourceOwner(databaseEntity.getUserId().getUserId())) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
