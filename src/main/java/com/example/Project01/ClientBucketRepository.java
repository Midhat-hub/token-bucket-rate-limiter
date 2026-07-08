package com.example.Project01;



import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientBucketRepository extends JpaRepository<ClientBucket, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClientBucket c where c.clientId = :clientId")
    Optional<ClientBucket> findByIdForUpdate(@Param("clientId") String clientId);
}