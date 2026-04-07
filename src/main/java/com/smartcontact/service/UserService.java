package com.smartcontact.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;

import com.smartcontact.entities.OtpEntity;
import com.smartcontact.entities.User;
import com.smartcontact.repository.OtpRepository;
import com.smartcontact.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private OtpRepository otpRepository;

    public boolean checkExistsEmail(String email) {
        boolean emailStatus = userRepository.existsByEmail(email);
        return emailStatus;
    }

    public User prepareUser(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setStatus("inactive");
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return user;
    }

    public User finalizeSave(User user) {
        try {
            User userFinal = userRepository.save(user);
            return userFinal;
        } catch (Exception e) {
            log.error("Error saving user: ", e.getMessage());
            throw new RuntimeException("Database error: Registration failed!");
        }

    }

    public boolean verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token);
        if (user != null) {
            user.setStatus("active");
            user.setVerificationToken(null);
            user.setTerms(true);

            userRepository.save(user);
            return true;
        }
        return false;
    }

    public User getUserByEmail(String email) {

        User user = userRepository.getUserByUserName(email);
        return user;
    }

    @Transactional
    public String checkLimitAndGenerateOtp(String email) throws Exception {

        Optional<OtpEntity> optionalOtp = otpRepository.findByEmail(email);
        OtpEntity otpEntity;

        if (optionalOtp.isPresent()) {
            otpEntity = optionalOtp.get();
            // Cool-down check
            if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
                otpEntity.setCounter(0);
                otpRepository.save(otpEntity);
            }
            // Limit check
            if (otpEntity.getCounter() >= 3) {
                throw new Exception("MAX_LIMIT_REACHED");
            }
        }

        SecureRandom sr = new SecureRandom();
        int firstDigit = 1 + sr.nextInt(9);
        int remainingDigits = sr.nextInt(100000);
        String otp = firstDigit + String.format("%05d", remainingDigits);
        return otp;

    }

    @Transactional
    public void finalizeAndSaveOtp(String email, String otp) {
        OtpEntity otpEntity = otpRepository.findByEmail(email).orElse(new OtpEntity());
        otpEntity.setEmail(email);
        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusSeconds(40));
        otpEntity.setCounter(otpEntity.getCounter() + 1);
        otpRepository.save(otpEntity);
    }

    public boolean verifyOtp(String email, String enterOtp) {
        Optional<OtpEntity> oldOtp = otpRepository.findByEmail(email);
        if (oldOtp.isPresent()) {
            OtpEntity otpEntity = oldOtp.get();
            log.info("DB OTP Time: {}", otpEntity.getExpiryTime());
            log.info("Current Time: {}", LocalDateTime.now());
            log.info("Is Valid? ", otpEntity.getExpiryTime().isAfter(LocalDateTime.now()));
            if (otpEntity.getOtp().equals(enterOtp) &&
                    otpEntity.getExpiryTime().isAfter(LocalDateTime.now())) {
                otpRepository.delete(otpEntity);
                return true;
            }

        }
        return false;
    }

    @Transactional
    public User updatePassword(String email, String password) {
        User user = userRepository.getUserByUserName(email);
        user.setTerms(true);
        user.setPassword(passwordEncoder.encode(password));
        User updt = userRepository.save(user);
        return updt;

    }

    public List<User> findAllExceptAdmin() {
        return userRepository.findAllExceptAdmin();

    }

    public User findByUserId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not Found with ID:" + id));
    }

    @Transactional
    public void updateUserStatus(User user) {
        user.setTerms(true);
        userRepository.save(user);
    }

    @Transactional
    public User processOauthUser(String email, String name, String picture, String providerId, String providerName) {

        User user = null;
        if (providerName.equals("GITHUB") && providerId != null) {
            user = userRepository.findByProviderUserIdAndProvided(providerId, providerName);
        }
        if (user == null && email != null) {
            user = userRepository.getUserByUserName(email);
        }

        String randPassword = UUID.randomUUID().toString();
        if (user == null) {
            // NEW USER LOGIC
            log.info("Service: Creating New OAuth User: {}", email);
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setImage(picture != null ? picture : "default.png");
            user.setProviderUserId(providerId);
            user.setProvided(providerName);
            user.setAbout("Logged in via " + providerName + " Authentication");
            user.setPassword(passwordEncoder.encode(randPassword));
            user.setRole("ROLE_USER");
            user.setStatus("active");
            user.setTerms(true);
        } else {
            // EXISTING USER SYNC LOGIC
            if (!providerName.equals(user.getProvided())) {
                throw new OAuth2AuthenticationException(
                   new OAuth2Error("provider mismatch",user.getProvided(),null));
            }

            // Photo sync
            if (user.getImage() == null
                    || user.getImage().contains("googleusercontent.com")
                    || user.getImage().contains("avatars.githubusercontent.com")) {
                user.setImage(picture);

            } else {
                log.info("DEBUG: Cloudinary/Manual photo detected. Skipping Overwrite!");
            }
            log.info("DEBUG: Comparing ProviderName {} with DB Provided {}", providerName, user.getProvided());

            if (!providerName.equals(user.getProvided())) {
                log.info("User already exists with provider: {}. Skipping provider update.", user.getProvided());
                if (user.getProviderUserId() == null) {
                    user.setProviderUserId(providerId);
                }
            }

        }
        user.setLastLogin(user.getCurrentLogin());
        user.setCurrentLogin(LocalDateTime.now());
        user.setTerms(true);

        return userRepository.saveAndFlush(user);
    }

}
