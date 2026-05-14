package com.template.springboot.modules.auth.repository;

import com.template.springboot.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken t set t.consumedAt = :consumedAt " +
           "where t.userId = :userId and t.consumedAt is null")
    int invalidateAllForUser(@Param("userId") Long userId, @Param("consumedAt") Instant consumedAt);

    @Modifying
    @Query("delete from PasswordResetToken t " +
           "where t.expiresAt < :cutoff or (t.consumedAt is not null and t.consumedAt < :cutoff)")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
