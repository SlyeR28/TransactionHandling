package com.rishabh.transactions.repository;

import com.rishabh.transactions.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {
}