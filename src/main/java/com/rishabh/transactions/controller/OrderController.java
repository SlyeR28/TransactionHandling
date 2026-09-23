package com.rishabh.transactions.controller;

import com.rishabh.transactions.dto.OrderDto;
import com.rishabh.transactions.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {



    private  final AccountService accountService;

    @PostMapping("/debit")
    public ResponseEntity<String> accountDebit(@RequestBody OrderDto dto) throws Exception {
        System.out.println("Incoming DTO -> id=" + dto.getId() + ", amount=" + dto.getAmount());
        return ResponseEntity.ok(accountService.AccountDebit(dto.getId(), dto.getAmount()));
    }


}
