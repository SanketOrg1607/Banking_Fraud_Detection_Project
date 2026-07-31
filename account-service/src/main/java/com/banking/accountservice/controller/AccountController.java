package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountservice;

    // Create the account
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountservice.createAccount(request));
    }

    // Get the Account
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber)
    {
        return ResponseEntity.ok(accountservice.getAccount(accountNumber));
    }

    //  Get the balance
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<String> getBalance(
        @PathVariable String accountNumber)
    {
        return ResponseEntity.ok(accountservice.getBalance());
    }

    // Block the Account
    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber
    )
    {
        return ResponseEntity.ok("Your account is blocked");
    }

    /*
    * SAGA  Step 1 - DEDUCT Balance
    * Called by transaction service when the transfer is initiated
    */

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
            )
    {
        return ResponseEntity.ok("Amount deducted successfully");
    }

    /*
    * SAGA Step 4  - Compensating transaction endpoint
    * called by transaction service in two scenarios
    * 1.if the transaction is completed successfully then amount credits to receiver
    * 2.if the transaction detects the fraud it will refund the amount to the sender
    * */

    public ResponseEntity<String> creditAmount(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount)
    {
        return ResponseEntity.ok("Amount credited successfully");
    }




}
