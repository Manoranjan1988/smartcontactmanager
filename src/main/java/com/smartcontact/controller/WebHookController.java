package com.smartcontact.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcontact.service.PaymentService;

@RestController
@RequestMapping("/api/webhook")
public class WebHookController {
    private static final Logger log = LoggerFactory.getLogger(WebHookController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Razorpay-Signature") String signature){

        try {
            log.info("=== RAZORPAY WEBHOOK HIT! ===");
            paymentService.processWebhook(payload, signature);
            return ResponseEntity.ok("Webhook Received!");
        } catch (Exception e) {
            log.error("Webhook Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fail");
        }
    }

}
