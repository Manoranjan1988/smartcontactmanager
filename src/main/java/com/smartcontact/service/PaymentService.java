package com.smartcontact.service;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.smartcontact.entities.MyOrder;
import com.smartcontact.entities.User;
import com.smartcontact.enums.PaymentStatus;
import com.smartcontact.repository.MyOrderRepository;
import com.smartcontact.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webHookSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MyOrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    public synchronized Map<String, Object> createOrder(int amount, Principal principal) throws Exception {

        User user = userRepository.getUserByUserName(principal.getName());
        long currentTime = System.currentTimeMillis() / 1000;
        MyOrder existingOrder = orderRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, PaymentStatus.CREATED);

        if (existingOrder != null && existingOrder.getStatus() == PaymentStatus.CREATED) {
            long orderTime = existingOrder.getCreatedAt();

            if ((currentTime - orderTime) < 120
                    && Integer.parseInt(existingOrder.getAmount()) == amount) {
                System.out.println("Returning existing order (idempotent)");

                Map<String, Object> response = new HashMap<>();
                response.put("id", existingOrder.getOrderId());
                response.put("amount", Integer.parseInt(existingOrder.getAmount()) * 100);
                response.put("keyId", keyId);

                return response;

            }
        }
        // 1. Razorpay Client initialize (Authentication)
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        // 2. Order ki details set karo
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100);// Important: Convert from Rupees to Paisa
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        // 3. Create the order from Razorpay server
        Order order = client.orders.create(orderRequest);
        log.info("Order Details: {}", order);

        // *--- Save in DB ---*
        MyOrder myOrder = new MyOrder();
        myOrder.setAmount(String.valueOf(amount));
        myOrder.setOrderId(order.get("id"));
        Object createdAtObj = order.get("created_at");
        if (createdAtObj instanceof Number) {
            myOrder.setCreatedAt(((Number) createdAtObj).longValue());
        } else {
            myOrder.setCreatedAt(System.currentTimeMillis() / 1000);
        }
        myOrder.setStatus(PaymentStatus.CREATED);
        myOrder.setPaymentId(null);
        myOrder.setReceipt(order.get("receipt"));
        myOrder.setUser(user);

        this.orderRepository.save(myOrder);

        // 4. store the data in Map and return
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.get("id"));
        response.put("amount", order.get("amount"));
        response.put("keyId", keyId);// in frontend to open the Payment Popup
        return response;
    }

    // WebHook Processing................

    @Transactional
    public void processWebhook(String payload, String signature) throws Exception {

        boolean isValid = Utils.verifyWebhookSignature(payload, signature, webHookSecret);
        if (!isValid) {
            System.out.println("Inavlid Signature");
            throw new Exception("Invalid Signature");
        }
        log.info("Signature Verified......");

        JSONObject root = new JSONObject(payload);
        long webhookTimestamp = root.getLong("created_at");
        if (!root.has("payload")) {
            log.info("Root has no payload key");
            return;
        }
        JSONObject innerPayload = root.getJSONObject("payload");
        if (!innerPayload.has("payment")) {
            log.info("innerPayload has no payment");
            return;
        }

        JSONObject paymentEntity = innerPayload.getJSONObject("payment").getJSONObject("entity");
        long bankTimestamp = paymentEntity.getLong("created_at");
        String method = paymentEntity.optString("method");
        String contact = paymentEntity.optString("contact");
        String vpa = paymentEntity.optString("vpa", "N/A");
        String bankName = paymentEntity.optString("bank");

        JSONObject acquirerData = paymentEntity.optJSONObject("acquirer_data");
        String rrn = (acquirerData != null) ? acquirerData.optString("rrn") : "N/A";
        String upiTxnId = (acquirerData != null) ? acquirerData.optString("upi_transaction_id") : "N/A";
        String bankTxnId = (acquirerData != null) ? acquirerData.optString("bank_transaction_id") : "N/A";

        double feeInRupees = paymentEntity.optInt("fee") / 100.0;

        String event = root.getString("event");
        log.debug("Webhook Event: {}", event);

         if (paymentEntity.isNull("order_id")) {
            log.error("Order ID missing in webhook");
            return;
        }
        String razorpayOrderId = paymentEntity.getString("order_id");
        String razorpayPaymentId = paymentEntity.getString("id");

        MyOrder order = orderRepository.findByOrderId(razorpayOrderId);

        if (order != null) {
            if ("payment.captured".equals(event)) {
                order.setSignature(signature);
                if (PaymentStatus.PAID.equals(order.getStatus())) {
                    log.info("Payment already processed for this Order ID. Skipping...");
                    return;
                }
                order.setPaymentMethod(method);
                order.setVpa(vpa);
                order.setUserContact(contact);
                order.setRrn(rrn);
                order.setBank(bankName);
                order.setUpiTransactionId(upiTxnId);
                order.setBankTransactionId(bankTxnId);
                order.setGatewayFee(String.valueOf(feeInRupees));
                order.setBankPaymentTime(bankTimestamp);
                order.setWebhookReceiveTime(webhookTimestamp);
                order.setStatus(PaymentStatus.PAID);
                order.setPaymentId(razorpayPaymentId);

                log.info("Order: {} Payment Success", razorpayOrderId);
                log.info("DEBUG: Setting status to PAID");

                MyOrder updatedOrder = orderRepository.save(order);
                if (updatedOrder.getUser() == null) {
                    return;
                }
                log.info("Order ID: {} updated to PAID", razorpayOrderId);
                try {

                    if (order.isEmailSent()) {
                        log.info("Email already sent for order: {}", order.getOrderId());
                        return;
                    }
                    String userEmail = updatedOrder.getUser().getEmail();
                    String userName = updatedOrder.getUser().getName();
                    String amount = updatedOrder.getAmount();

                    emailService.sendPaymentSuccessEmail(userEmail, userName, razorpayOrderId, amount);
                    order.setEmailSent(true);
                    orderRepository.save(order);
                    log.info("Payment Confirmation Mail Sent to: {} ", userEmail);
                } catch (Exception e) {
                    log.error("Error sending payment mail: " + e);
                    throw new RuntimeException("Error sending payment email");
                }
            } else if ("payment.failed".equals(event)) {
                order.setStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                log.info("Order: {} Payment Fail!", razorpayOrderId);
            }

        }

        else {
            log.info("CRITICAL: Order NOT FOUND for ID: {} ", razorpayOrderId);

        }

    }

    public Page<MyOrder> getAllOrders(int currentPage, String email) {
        
        Pageable pageable = PageRequest.of(currentPage, 4);
        return orderRepository.getAllOrders(email, pageable);
    }
}
