package com.template.springboot.modules.session.repository;

import com.template.springboot.modules.session.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @Query("select s from UserSession s where s.userId = :userId and s.revokedAt is null order by s.lastUsedAt desc")
    List<UserSession> findActiveByUserId(@Param("userId") Long userId);

    @Query("select s from UserSession s where s.revokedAt is null order by s.lastUsedAt desc")
    List<UserSession> findAllActive();

    @Modifying
    @Query("""
            update UserSession s
               set s.revokedAt = :revokedAt,
                   s.revokedReason = :reason
             where s.userId = :userId
               and s.revokedAt is null
            """)
    int revokeAllByUserId(@Param("userId") Long userId,
                          @Param("revokedAt") Instant revokedAt,
                          @Param("reason") String reason);

    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :cutoff or (s.revokedAt is not null and s.revokedAt < :cutoff)")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
