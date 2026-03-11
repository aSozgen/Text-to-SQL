package com.texttosql.backend.repository;

import com.texttosql.backend.entity.PasswordResetTokenEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
    Optional<PasswordResetTokenEntity> findByUserAndUsedFalse(UserEntity user);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
