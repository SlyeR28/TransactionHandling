package com.rishabh.transactions.service;

import com.rishabh.transactions.entity.Account;
import com.rishabh.transactions.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final OrderService orderService;

     @Transactional(rollbackFor = Exception.class)
     public String AccountDebit(Long id , Long amount) throws Exception {
         String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
         System.out.println("Current Transaction Name of method Account Debit is: " +transactionName);

         Account account = accountRepository.findById(id).orElseThrow(() ->
                 new RuntimeException("Account not found by Id " + id));

         if(account.getBalance() < amount){
             throw new RuntimeException("Insufficient balance");
         }

         System.out.println("Current Amount in " +account.getName() + " is " +account.getBalance());

         Long remainingBalance =  account.getBalance() - amount;

         System.out.println(" Remaining Amount from  " +account.getName() + " is " +remainingBalance);

         account.setBalance(remainingBalance);

         accountRepository.save(account);
         orderService.saveOrderRecord(id , account.getName() , amount , "Debited");
         try{
             int i = 10/0;

         } catch (Exception e) {
             throw new Exception(e);
         }
         return "Amount Debited From " +account.getName() + " by " +amount;
     }

}
