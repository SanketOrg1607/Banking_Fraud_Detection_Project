package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message="Account name is required")
    private String accountHolderName;

    @NotBlank(message="Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message="Phone is required")
    private String phone;

    @NotBlank(message = "Account type required")
    private AccountType accountType;

    @NotBlank(message = "Initial deposit is required")
    @Positive(message="initial deposit must be greater than zero")
    private BigDecimal initialDeposit;

}
