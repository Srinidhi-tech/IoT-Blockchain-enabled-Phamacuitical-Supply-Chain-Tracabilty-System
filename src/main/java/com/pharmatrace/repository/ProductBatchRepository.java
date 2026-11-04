package com.pharmatrace.repository;

import com.pharmatrace.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    Optional<ProductBatch> findByBatchId(String batchId);

    Optional<ProductBatch> findFirstByOrderByIdAsc();
}
