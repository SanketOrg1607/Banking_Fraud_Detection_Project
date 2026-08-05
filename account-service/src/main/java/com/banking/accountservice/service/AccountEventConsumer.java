package com.banking.accountservice.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@KafkaListener(topics="transaction.completed")
public class AccountEventConsumer {

    private final AccountService accountService;

    /*
    * Consume transaction completed event from  kafka
    * credits receiver amount
    * @param payload
    * */
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload)
    {
        try{
            String receiverAccount = (String)payload.get("receiverAccountNumber");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            log.info("Creating account {} : amount {}",receiverAccount,amount);
            accountService.creditBalance(receiverAccount,amount);
        }
        catch(Exception e)
        {
            log.error("Error crediting account : {}",e.getMessage());
        }
    }

    /*
    * consume fraud detected event from kafka
    * Blocks hre flagged account
    * @param payload
    */
    public void consumerFraudDetected(@Payload Map <String,Object> payload)
    {
        try{
            String accountNumber= (String)payload.get("accountNumber");
            log.info("Fraud detected - blocking account : {}",accountNumber);

            accountService.blockAccount(accountNumber);
        }
        catch(Exception e){
            log.error("Error blocking account :{}",e.getMessage());
        }
    }
}
