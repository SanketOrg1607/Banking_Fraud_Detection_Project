package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // Create the transfer
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request
            )
    {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    // Get the transaction by the transaction id
    @GetMapping("/transactionId")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId
    )
    {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    // Get the transaction history by the account number
    @GetMapping("/accountNumber")
    public ResponseEntity<List<TransactionResponse>> transactionHistory(
            @PathVariable String accoTuntNumber
    )
    {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    /*
        -- Otp Verification endpoint IMP
     */
    @PostMapping("/transactionId/verify")
    public ResponseEntity<TransactionResponse> verifyOtp(
            @PathVariable String transactionId,
            @RequestParam String otp
    )
    {
        log.info("Otp verfication request - transaction: {}",transactionId);

        return ResponseEntity.ok(
                transactionService.verifyOTP(transactionId,otp)
        );
    }


}
