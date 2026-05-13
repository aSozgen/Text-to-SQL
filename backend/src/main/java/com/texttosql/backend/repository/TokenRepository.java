package com.texttosql.backend.repository;

import com.texttosql.backend.entity.TokenEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, UUID> {
    Optional<TokenEntity> findByTokenAndType(String token, TokenType tokenType);
    Optional<TokenEntity> findByUserAndTypeAndUsedFalse(UserEntity user, TokenType tokenType);
    void deleteByExpiresAtBefore(LocalDateTime now);
    List<TokenEntity> findAllByUserAndTypeAndUsedFalse(UserEntity user, TokenType type);
}
