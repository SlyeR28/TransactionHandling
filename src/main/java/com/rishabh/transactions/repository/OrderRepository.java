package com.rishabh.transactions.repository;

import com.rishabh.transactions.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}