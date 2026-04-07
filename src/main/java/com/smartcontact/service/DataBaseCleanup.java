package com.smartcontact.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartcontact.entities.User;
import com.smartcontact.repository.UserRepository;

@Component
public class DataBaseCleanup {
    private static final Logger log = LoggerFactory.getLogger(DataBaseCleanup.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")

    public void deleteUnverifiedUsers(){
        log.info("Scheduler triggered at {}", LocalDateTime.now()); 
        LocalDateTime cutoff = LocalDateTime.now().plusDays(1);

        List<User> junkUsers = userRepository.findByStatusAndRegistrationDateBefore("inactive",cutoff);

        if(junkUsers.size() >1000){
            log.warn("Cleanup skipped: too many users to delete!");
            return;
        }

        if(!junkUsers.isEmpty()){
            userRepository.deleteAll(junkUsers);
            log.info("Cleanup Done! Deleted {} users", junkUsers.size());
        }

    }

}
