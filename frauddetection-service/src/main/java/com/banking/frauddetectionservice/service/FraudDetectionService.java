package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.model.FraudCheckResult;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor

public class FraudDetectionService {
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final RedisTemplate<String,String> redisTemplate;

    @Value("{fraud.max-transactions-per-minute")
    private int maxTransactionsPerMinute;

    @Value("{fraud.suspicious-amount-multiplier")
    private double suspiciousAmountMultiplier;

    private static final String VERFIFICATION_REQUIED_TOPIC = "verification.required";


    public void checkTransaction(Map<String,Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("SenderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        // Fetch the real balance from Account Service
        // We will get the balance using AccountServiceClient which is feign client

        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction: {} account : {} amount :{} balance:{}",
                transactionId, accountNumber, amount, senderBalance);

        FraudCheckResult result = performFraudChecks(accountNumber, amount, senderBalance);

        if (result.isFraud()) {
            log.info("Suspicious activity detected - account: {}" +
                            "reason: {} - requesting OTP verification",
                    accountNumber, result.getReason());

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERFIFICATION_REQUIED_TOPIC, transactionId, verificationEvent);
        } else {
            // transaction is clean
            Map<String,Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);
            transactionCleanEvent.put("reason",null);
        }
    }

    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance
    )
    {
        // Pattern 1 : Velocity check
        if(isVelocityExceeded(accountNumber))
        {
            return new FraudCheckResult(
                    true,"Too many transactions in one second"+
                    "Velocity limit exceeded"
                    );
        }

        // Pattern 2 : Amount Check
        if(isAmountSuspicious(accountNumber,amount))
        {
            return new FraudCheckResult(true,"Unusual transaction amount" +
                    " - exceeds 3x your average");
        }

        // Pattern  3 : Balance check
        if(senderBalance.compareTo(BigDecimal.ZERO) > 0
                && isBalanceCheckFailed(senderBalance,amount))
        {
            return new FraudCheckResult(true,"Transaction exceed 90% of account balance");
        }

        return new FraudCheckResult(false,null);
    }

    // Pattern 1 method impl
    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

//        The if (count == 1) check is strictly for brand new windows (count = 1). The 6th transaction has a count of 6, so it is strictly a "block and move on" operation! 🎯


        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        log.info("Velocity check - account : {} count: {}/{}",
                accountNumber, count, maxTransactionsPerMinute);

        return count != null && count > maxTransactionsPerMinute;
    }

    // Pattern : Amount check
    // here we are checking the initiated transaction amount 3X more than the average
    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount){
        String avgKey = "fraud:avg_amount"+ accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);

        if(avgStr == null)
        {
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            return false;
        }

        BigDecimal avgAmount  = new BigDecimal(avgStr);
//        BigDecimal threshold = avgAmount.multiply()

        return false;

    }




}


