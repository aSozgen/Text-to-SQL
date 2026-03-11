package com.texttosql.backend.repository;

import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, UUID> {
    Optional<VerificationTokenEntity> findByToken(String token);
    Optional<VerificationTokenEntity> findByUserAndUsedFalse(UserEntity user);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
