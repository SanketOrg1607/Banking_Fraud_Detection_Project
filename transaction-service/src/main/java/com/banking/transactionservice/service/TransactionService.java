package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.entity.TransactionType;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final RedisTemplate<String,String> redisTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";


    /*
    * Saga Step 1 - Initiate the transaction
    * deduct the account balance from the sender account via feign client
    * saves the transaction as a PROCESSING
    * publish event to kafka for fraud check
    * @param request
    * @return =
    * */
    public TransactionResponse transfer(TransferRequest request)
    {
        log.info("SAGA START - Transfer: {} to {} with amount {}",
                  request.getSenderAccountNumber(),
                  request.getReceiverAccountNumber(),
                  request.getAmount()
                );

        // SAGA STEP 1 : Deduct from sender
        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),
                request.getAmount()
        );

        // saves the transaction as a PROCESSING
        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setReferenceNumber(UUID.randomUUID().toString());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING: {}",savedTransaction.getId());


        transactionRepository.save(savedTransaction);
        log.info("Transaction saved into db as Processing {}",savedTransaction.getId());

        // SAGA STEP - 2: Publish for fraud check
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()
        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
        log.info("SAGA STEP 2 - TransactionInitiatedEvent Published: {}",savedTransaction.getId());

        return mapToResponse(savedTransaction);
        // transfer method is completed
    }

    // Get transaction by transaction id
    public TransactionResponse getTransaction(String transactionId)
    {
        return mapToResponse( transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found"
                )));
    }

    // Get transaction history
    public List<TransactionResponse> getTransactionHistory(String accountNumber)
    {
        return transactionRepository.findSenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private  TransactionResponse mapToResponse(Transaction transaction)
    {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    public TransactionResponse verifyOtp(String transactionId,String otp)
    {
        log.info("OTP verification fo the transaction : {} ",transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"+transactionId));

        // getting stored otp  to check  given otp by user is correct or not
        String otpKey = "verification:otp" + transaction;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if(storedOtp == null)
        {
            // OTP EXPIRED
            log.warn("OTP expired for transaction: {}",transactionId);
            compensateTransaction(transaction,"OTP expired - transaction canceled amount refunded");
            return mapToResponse(transaction);
        }

        if(!storedOtp.equals(otp))
        {
            // BlOCK ACCOUNT AND REFUND
            log.warn("Wrong OTP - blocking account and refunding : {}",transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndCompesate(transaction,
                    "Wrong OTP entered - transaction cancelled, "+
                            "Account blocked for security"
                    );

            return  mapToResponse(transaction);
        }

        // OTP correct - completed transaction
        log.info("OTP verified - completing transaction: {}",transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return mapToResponse(transaction);
    }

    private void compensateTransaction(Transaction transaction,String reason)
    {
        log.warn("SAGA COMPENSATION - refunding: {} amount:{}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        // CREDIT MONEY BACK TO SENDER SYNCHRONOUSLY
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason +
                "- SAGA Compensation executed ,amount refunded at "+ LocalDateTime.now());

        transactionRepository.save(transaction);

        // PUBLISH refund event - NOTIFICATION service will alert user

    }



}
