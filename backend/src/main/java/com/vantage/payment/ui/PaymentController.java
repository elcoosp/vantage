package com.vantage.payment.ui;

import com.vantage.payment.app.PaymentService;
import com.vantage.payment.ui.dto.PaymentRequest;
import com.vantage.payment.ui.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.vantage.api.api.ApiApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController implements ApiApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header must not be blank");
        }
        PaymentResponse response = paymentService.processPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    public ResponseEntity<com.vantage.api.model.PaymentResponse> apiV1PaymentsPost(UUID idempotencyKey, com.vantage.api.model.PaymentRequest paymentRequest) {
        com.vantage.payment.ui.dto.PaymentRequest internalRequest =
            new com.vantage.payment.ui.dto.PaymentRequest(
                paymentRequest.getOrderId(),
                paymentRequest.getAmount(),
                paymentRequest.getCurrency()
            );
        com.vantage.payment.ui.dto.PaymentResponse internalResponse =
            paymentService.processPayment(idempotencyKey.toString(), internalRequest);
        com.vantage.api.model.PaymentResponse response = new com.vantage.api.model.PaymentResponse()
            .transactionId(internalResponse.transactionId().toString())
            .status(com.vantage.api.model.PaymentResponse.StatusEnum.fromValue(internalResponse.status()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}