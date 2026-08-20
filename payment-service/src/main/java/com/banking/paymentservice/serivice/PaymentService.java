package com.banking.paymentservice.serivice;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value(("${razorpay.key-secrete}"))
    private String keySecrete;



    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed ";

    /**
     * Create Razorpay payment order
     * FLOW
     * 1.Create order in razorpay
     * 2.Save Payment record to db
     * 3.Return order details to frontend
     * 4.Frontend shows checkout page
     * 5.User pays
     * 6.Razorpay calls webhook - after user pays web captured status and events
     * @param request
     */
    public PaymentOrderResponse createPaymentOrder(
            CreatePaymentRequest request
    ) throws RazorpayException
    {
        log.info("Creating payment order for account: {} amount: {}",
                request.getAccountNumber(),
                request.getAmount()
                );

        RazorpayClient razorpayClient = new RazorpayClient(keyId,keySecrete);
        // pass razorpay key and razorpay secrete

        // Converted amount
        // USD to cent
        // INR to paise
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();


        // Created a new  request object
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount",convertedAmount);
        orderRequest.put("currency","USD/INR");
        orderRequest.put("receipt","rcpt"+ + System.currentTimeMillis() + UUID.randomUUID().toString()
                .replace("-","").substring(0,10)
        );

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created : {}",razorpayOrder.get("id").toString());

        // Create order in razorpay is done
        
    }

}
