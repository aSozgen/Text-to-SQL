package com.texttosql.backend.service;

import com.texttosql.backend.dto.DatabaseDTO;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatabaseService {
    private final DatabaseRepository databaseRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<DatabaseDTO> getDatabases() {
        final UserEntity currentUser = getCurrentUserEntity();
        List<DatabaseEntity> databaseEntities = databaseRepository.findByUserIdOrderByCreatedAtDesc(currentUser);

        return databaseEntities.stream()
                .map(entity -> new DatabaseDTO(entity.getDatabaseId(),entity.getName(), entity.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DatabaseEntity getDatabaseEntity(UUID databaseId) {
        return databaseRepository.findByDatabaseId(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found"));
    }

    @Transactional(readOnly = true)
    public DatabaseDTO getDatabase(UUID databaseId) {
        DatabaseEntity entity = getCurrentDatabaseEntity(databaseId);
        return new DatabaseDTO(entity.getDatabaseId(), entity.getName(), entity.getDescription());
    }


    public @Valid DatabaseDTO createDatabase(DatabaseDTO databaseDTO) {
        final UserEntity currentUser = getCurrentUserEntity();

        if (databaseRepository.existsByNameIgnoreCaseAndUserId(databaseDTO.getName(), currentUser)) {
            throw new DuplicatedResourceException("There is already a database with the name '" + databaseDTO.getName() + "'");
        }

        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setUserId(currentUser);
        databaseEntity.setName(databaseDTO.getName());
        databaseEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedEntity = databaseRepository.save(databaseEntity);
        databaseDTO.setDatabaseId(savedEntity.getDatabaseId());
        return databaseDTO;
    }

    public @Valid DatabaseDTO updateDatabase(UUID databaseId, DatabaseDTO databaseDTO) {
        final UserEntity currentUser = getCurrentUserEntity();
        DatabaseEntity oldEntity = getCurrentDatabaseEntity(databaseId);

        if (databaseRepository.existsByNameIgnoreCaseAndUserId(databaseDTO.getName(), currentUser)
                && !oldEntity.getName().equalsIgnoreCase(databaseDTO.getName())) {
            throw new DuplicatedResourceException("Database name already exists");
        }

        isResourceOwner(oldEntity.getUserId().getUserId());

        oldEntity.setName(databaseDTO.getName());
        oldEntity.setDescription(databaseDTO.getDescription());

        DatabaseEntity savedEntity = databaseRepository.save(oldEntity);
        databaseDTO.setDatabaseId(savedEntity.getDatabaseId());
        return databaseDTO;
    }

    @Transactional
    public void deleteDatabase(UUID databaseId) {
        DatabaseEntity databaseEntity = getCurrentDatabaseEntity(databaseId);
        isResourceOwner(databaseEntity.getUserId().getUserId());

        databaseRepository.delete(databaseEntity);
    }

    private UserEntity getCurrentUserEntity() {
        return securityUtil.getCurrentUserEntity()
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private DatabaseEntity getCurrentDatabaseEntity(UUID databaseId) {
        return databaseRepository.findByDatabaseId(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found"));
    }

    private void isResourceOwner(UUID uuid) {
        if (!securityUtil.isResourceOwner(uuid)) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
