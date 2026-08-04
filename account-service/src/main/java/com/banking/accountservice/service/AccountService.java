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
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request)
    {
        log.info("Creating account for {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail()))
        {
            throw new RuntimeException("Account already exists for " + request.getEmail());
        }

        // Setting the account request values from the account
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
        account.setDailyTransactionLimit(
                request.getAccountType()== AccountType.SAVINGS
                ? new BigDecimal("100000") :new BigDecimal("500000")
        );

        Account savedAccount= accountRepository.save(account);
        log.info("Account created : {}",savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    /*
    * Get account by account number */
    public AccountResponse getAccount(
            String accountNumber
    )
    {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not found"));

        return mapToResponse(account);
    }

    /*
    * get account balance */
    public BigDecimal getBalance(
            String accountNumber
    )
    {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not found"));

        return account.getBalance();
    }
    /*
    * Block account - called by fraud detection service via kafka
    * */
    public void blockAccount(String accountNumber)
    {
        log.info("Blocking account : {}",accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked: {}",accountNumber);
    }

    /*
    * Deduct balance from sender account
    * Called by transaction service via kafka  */
    public void deductBalance(String accountNumber,BigDecimal amount)
    {
        log.info("Deducting balance {} from account : {}",amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));

        if(account.getStatus() != AccountStatus.ACTIVE)
        {
            throw new RuntimeException("Account is not active"+accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0)
        {
            throw new RuntimeException("Insufficient funds for account "+accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        log.info("Balance updated. New Balance: {} ",account.getBalance());
    }

    /*
     Credit balance called by transaction service via kafka
     */
    public void creditBalance(String accountNumber,BigDecimal amount)
    {
        log.info("Crediting {} to account: {}",amount,accountNumber);
    }

    // Generate 12 digit unique account number
    private String generateAccountNumber()
    {
        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d",number);

        }while(accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;

    }

    // mapToResponse creating a mapper to convert Account to AccountResponse
    private AccountResponse mapToResponse(Account account)
    {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }


}
