package com.example.Project01;

 // match your actual package name

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestLogRepository extends JpaRepository<RequestLogEntry, Long> {

    @Query("select count(r) from RequestLogEntry r where r.clientId = :clientId and r.requestedAtEpochMillis >= :sinceEpochMillis")
    long countRecentRequests(@Param("clientId") String clientId, @Param("sinceEpochMillis") long sinceEpochMillis);

    @Query("select min(r.requestedAtEpochMillis) from RequestLogEntry r where r.clientId = :clientId and r.requestedAtEpochMillis >= :sinceEpochMillis")
    Long findOldestTimestamp(@Param("clientId") String clientId, @Param("sinceEpochMillis") long sinceEpochMillis);
}
