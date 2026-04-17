package com.smartcontact.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcontact.entities.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    public boolean existsByEmail(String email);

    public User findByVerificationToken(String token);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    public User getUserByUserName(@Param("email") String email);

    public void deleteByEmail(String email);

    @Query("SELECT u FROM User u where u.role != 'ROLE_ADMIN' ORDER BY u.id ASC")
    List<User> findAllExceptAdmin();

    public User findByProviderUserIdAndProvided(String providerUserId, String providerName);

    List<User> findByStatusAndRegistrationDateBefore(String status, LocalDateTime dateTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findByEmailForUpdate(@Param("email") String email);
}
