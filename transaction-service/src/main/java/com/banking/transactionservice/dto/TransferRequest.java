package com.banking.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    @NotBlank(message="Sender account number is required")
    private String senderAccountNumber;

    @NotBlank(message="Receiver account number is required")
    private String receiverAccountNumber;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
    private String description;
}
