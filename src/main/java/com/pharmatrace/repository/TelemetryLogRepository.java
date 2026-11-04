package com.pharmatrace.repository;

import com.pharmatrace.model.TelemetryLog;
import com.pharmatrace.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryLogRepository extends JpaRepository<TelemetryLog, Long> {

    List<TelemetryLog> findAllByOrderByTimestampDesc();

    List<TelemetryLog> findTop25ByOrderByTimestampDesc();

    Optional<TelemetryLog> findTop1ByOrderByTimestampDesc();

    List<TelemetryLog> findByBatchIdOrderByTimestampDesc(String batchId);

    long countByVerificationStatus(VerificationStatus status);
}
