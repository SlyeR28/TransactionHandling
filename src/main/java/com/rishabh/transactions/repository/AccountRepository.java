package com.rishabh.transactions.repository;

import com.rishabh.transactions.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}