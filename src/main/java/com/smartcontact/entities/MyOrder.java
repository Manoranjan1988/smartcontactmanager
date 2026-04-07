package com.smartcontact.entities;


import com.smartcontact.enums.PaymentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;
    private String orderId; 
    private Long createdAt;
    private String amount;
    private String receipt;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; 
    private String paymentId; 
    private String signature;
    private String paymentMethod;
    private String vpa;
    private String userContact;
    private String rrn;
    private String upiTransactionId;
    private String BankTransactionId;
    private String bank;
    private String gatewayFee;
    private Long bankPaymentTime;
    private Long webhookReceiveTime;   
    private boolean emailSent = false;
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
    @Override
    public String toString() {
        return "MyOrder [user=" + user + "]";
    }

    
    
}
