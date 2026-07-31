package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest request)
    {
        log.info("Creating account for {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail()))
        {
            throw new RuntimeException("Account already exists for " + request.getEmail());
        }

        // Setting the account request values fro the account
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        // for the account number we have to generate account number
        account.setAccountNumber(generateAccountNumber());
        // set daily transaction limit
        account.setDailyTransactionLimit(request.getAccountType());
    }
}
