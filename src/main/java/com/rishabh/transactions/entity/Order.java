package com.rishabh.transactions.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private String status;

    private Long accountId;

    private Long price;
}