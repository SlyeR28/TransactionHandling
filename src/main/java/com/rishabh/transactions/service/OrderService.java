package com.rishabh.transactions.service;

import com.rishabh.transactions.entity.Order;
import com.rishabh.transactions.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrderService {


    private final OrderRepository orderRepository;


    @Transactional
    public void saveOrderRecord(Long accountId , String name , Long amount , String status){
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();

        System.out.println("Current Transaction Name of method saveOrderRecord is: " +transactionName);

        try{
            Order order = new Order();
            order.setProductName(name);
            order.setPrice(amount);
            order.setStatus(status);
            order.setAccountId(accountId);
            orderRepository.save(order);
//            int i = 10/0;
        } catch (RuntimeException e) {
            throw new RuntimeException("Exception while saving order record");
        }
    }



}
