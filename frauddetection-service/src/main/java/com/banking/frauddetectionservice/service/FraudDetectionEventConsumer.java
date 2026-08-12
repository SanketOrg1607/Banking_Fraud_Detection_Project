package com.banking.frauddetectionservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    public void consumeTransactionInitiate(
            @Payload Map<String,Object> payload
    )
    {
        log.info("Received transaction for fraud check: {}",
                payload.get("transactionId"));

        try{
            fraudDetectionService.checkTransaction(payload);
        }catch(Exception e)
        {

        }
    }




}
