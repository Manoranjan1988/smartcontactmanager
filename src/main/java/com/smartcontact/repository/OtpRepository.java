package com.smartcontact.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcontact.entities.OtpEntity;

public interface OtpRepository  extends JpaRepository<OtpEntity, Long>{

        public Optional<OtpEntity> findByEmail(String email);
        
}
