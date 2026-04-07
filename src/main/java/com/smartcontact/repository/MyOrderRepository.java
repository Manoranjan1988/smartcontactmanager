package com.smartcontact.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcontact.entities.MyOrder;
import com.smartcontact.entities.User;
import com.smartcontact.enums.PaymentStatus;

public interface MyOrderRepository extends JpaRepository<MyOrder, Long> {
    public MyOrder findByOrderId(String OrderId);

    MyOrder findTopByUserAndStatusOrderByCreatedAtDesc(User user, PaymentStatus status);

    @Query("SELECT o FROM MyOrder o WHERE o.user.email = :email ORDER BY o.createdAt DESC")
    List<MyOrder> findByEmail(@Param("email") String email);
}
