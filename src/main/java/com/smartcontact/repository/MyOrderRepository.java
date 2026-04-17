package com.smartcontact.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<MyOrder> getAllOrders(@Param("email") String email,Pageable pageable);
}
