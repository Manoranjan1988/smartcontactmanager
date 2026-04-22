package com.smartcontact.controller;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcontact.entities.User;
import com.smartcontact.exception.EmailSendException;
import com.smartcontact.exception.MaxAttemptsException;
import com.smartcontact.service.EmailService;
import com.smartcontact.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/public")
public class SmartController {
    private static final Logger log = LoggerFactory.getLogger(SmartController.class);

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    /* Signup Opeing Page handler */
    @GetMapping("signup")
    public String signupHandler(Model model) {
        model.addAttribute("title", "Signup - Smart Contact Manager");
        model.addAttribute("user", new User());
        return "public/signup";
    }

    /* Login Opening Page handler */
    @GetMapping("login")
    public String loginHandler(
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "session", required = false) String session,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "update", required = false) String update,
            Model model,
            HttpServletRequest request) {

        log.info("=== LOGIN PAGE HIT ===");
        String email = (String) request.getSession().getAttribute("LOGIN_EMAIL");
        log.info("Session EMAIL: {}", request.getSession().getAttribute("LOGIN_EMAIL"));

        String query = request.getQueryString();
        String fullUrl = request.getRequestURL().toString() +
                (query != null ? "?" + query : "");

        log.info("URL: {}", fullUrl);

        if ("success".equals(logout) && !"null".equals(error)) {
            model.addAttribute("msg", "You have been logged out successfully!");
            model.addAttribute("type", "alert-success");
            return "public/login";
        }
        if ("expired".equals(session) && !"null".equals(error)) {
            model.addAttribute("msg", "Your session expired! You logged in from another device.");
            model.addAttribute("type", "alert-warning");
            return "public/login";
        }
        if ("success".equals(update) && !"null".equals(error)) {
            model.addAttribute("msgTitle", "Password Updated!");
            model.addAttribute("msgDash", "Password Update Successful ! Please login with your new password.");
            model.addAttribute("msgType", "success");
            return "public/login";
        }
        if ("disabled".equals(error)) {
            request.getSession().setAttribute("showResend", true);

            User user = userService.getUserByEmail(email);

            String message = "Your account is not verified. Please verify your email.";
            boolean showResend = true;

            if(user != null && user.getVerificationToken() == null) {
            message = "Your account is deactivated. Please contact admin.";
            showResend = false;
            }

            if (user != null && user.getResendBlockedUntil() != null &&
                    user.getResendBlockedUntil().isAfter(LocalDateTime.now())) {

                Duration duration = Duration.between(
                        LocalDateTime.now(),
                        user.getResendBlockedUntil());

                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();

                message = String.format(
                        "Max attempts reached. Try again after %d hr %d min",
                        hours, minutes);
                showResend = false;        
            }

            model.addAttribute("msg", message);
            model.addAttribute("type", "error");
            model.addAttribute("showResend", showResend);
            model.addAttribute("email", email);

            return "public/login";
        }

        if (error != null && !"null".equals(error)) {

            String message = switch (error) {

                case "bad" -> "Wrong credentials! Please try again.";
                case "oauth" -> "OAuth login failed. Please try again.";
                case "verify_email" -> "Email not verified from provider";
                case "inactive_user" -> "User inactive by Admin, Contact for support";
                default -> "Invalid Username & Password.";
            };

            model.addAttribute("msg", message);
            model.addAttribute("type", "alert-danger");

            return "public/login";
        }

        return "public/login";
    }

    /* About Opening Page handler */
    @GetMapping("about")
    public String aboutHandler(Model model) {
        model.addAttribute("title", "About - Smart Contact Manager");
        return "public/about";
    }

    /* Contactus Page handler */
    @GetMapping("contactus")
    public String contactUsHandler(Model model) {
        model.addAttribute("title", "Contact us - Smart Contact Manager");
        return "public/contactus";
    }
    /* Terms Page handler */
    @GetMapping("terms")
    public String termsHandler(Model model) {
        model.addAttribute("title", "Terms - Smart Contact Manager");
        return "public/terms";
    }

    /* privacy Policy Page handler */
    @GetMapping("privacy")
    public String privacyHandler(Model model) {
        model.addAttribute("title", "Privacy & Policy - Smart Contact Manager");
        return "public/privacy";
    }

    /* refund Policy Page handler */
    @GetMapping("refund")
    public String refundHandler(Model model) {
        model.addAttribute("title", "Refund Policy - Smart Contact Manager");
        return "public/refund";
    }

    /* Signup Processing Page handler */
    @PostMapping("/do_register")
    public String doRegister(@Valid @ModelAttribute("user") User user, BindingResult br, Model model,
            RedirectAttributes ra, HttpServletRequest request) {
        try {
            if (br.hasErrors()) {
                return "public/signup";
            }

            User dbUser = userService.getUserByEmail(user.getEmail());
            if (dbUser != null) {
                log.info("Email: {}", dbUser.getEmail());
                log.info("Provider Name: {}", dbUser.getProvided());

                if (dbUser.getProvided().equals("SELF")) {
                    model.addAttribute("msg", "Login using email & password");
                    model.addAttribute("type", "error");
                    return "public/signup";
                } else if (dbUser.getProvided().equals("GOOGLE")) {
                    model.addAttribute("msg", "Login using Google");
                    model.addAttribute("type", "error");
                    return "public/signup";
                } else if (dbUser.getProvided().equals("GITHUB")) {
                    model.addAttribute("msg", "Login using GitHub");
                    model.addAttribute("type", "error");
                    return "public/signup";
                }
            }

            String url = baseUrl;

            User preparedUser = userService.prepareUser(user);

            try {
                userService.finalizeSave(preparedUser);
                boolean isSent = emailService.sendVerificationEmail(preparedUser.getEmail(),
                        preparedUser.getVerificationToken(), url);

                if (isSent) {

                    ra.addFlashAttribute("msg", "Register Successful, Activate your mail");
                    ra.addFlashAttribute("type", "success");
                } else {
                    ra.addFlashAttribute("msg", "Registration okay, but mail sending failed!");
                    ra.addFlashAttribute("type", "error");
                }
                return "redirect:/public/signup";
            } catch (RuntimeException e) {
                model.addAttribute("msg", "Request already in progress or Email exists!");
                model.addAttribute("type", "danger");
                return "public/signup";
            }

        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            log.error("Internal Error", e);
            ra.addFlashAttribute("user", user);
            ra.addFlashAttribute("msg", "Something went wrong !!");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/public/signup";
        }
    }

    /* Email Verification handler */
    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token, Model model) {
        System.out.println("Verify Handler Started.....");
        boolean isVerified = userService.verifyUser(token);
        if (isVerified) {
            model.addAttribute("msg", "Account activated! You can now login.");
            model.addAttribute("type", "success");
        } else {
            model.addAttribute("msg", "Invalid or expired link!");
            model.addAttribute("type", "error");
        }
         return "public/verify-result";
    }

    /* Forgot Password handler */
    @GetMapping("/forgotpassword")
    public String forgotPasswordHandler(
            @RequestParam(value = "clear", required = false) String clear,
            Model model, HttpSession session) {
        if ("true".equals(clear)) {
            session.removeAttribute("otpSent");
            session.removeAttribute("resetEmail");
        }

        Boolean otpSent = (Boolean) session.getAttribute("otpSent");
        String email = (String) session.getAttribute("resetEmail");

        if (otpSent != null && otpSent) {
            model.addAttribute("otpSent", true);
            model.addAttribute("email", email);
        } else {
            model.addAttribute("otpSent", false);
            model.addAttribute("email", email);
        }
        model.addAttribute("title", "Password : Change");
        return "public/forgotpassword";
    }

    /* Send OTP handler */
    @PostMapping("/send_otp")
    public String sendOtpHandler(@RequestParam("email") String email,
            Model model, RedirectAttributes ra, HttpSession session) {

        User user = userService.getUserByEmail(email);

        if (user == null) {
            model.addAttribute("msg", "Invalid Account");
            return "public/forgotpassword";
        } else if (user.getStatus().equals("inactive")) {
            model.addAttribute("msg", "Contact to Admin for Activation");
            return "public/forgotpassword";
        } else if (!"SELF".equals(user.getProvided())) {
            model.addAttribute("msg", "Password management is handled by your social provider.");
            return "public/forgotpassword";
        } else {
            try {
                String otp = userService.checkLimitAndGenerateOtp(email);
                emailService.otpEmailVerification(email, otp);
                userService.finalizeAndSaveOtp(email, otp);

                session.setAttribute("otpSent", true);
                session.setAttribute("resetEmail", email);

                ra.addFlashAttribute("otpSent", true);
                ra.addFlashAttribute("email", email);
                ra.addFlashAttribute("msg", "OTP sent! (Attempt: " + email + ")");

            }

            catch (Exception e) {
                ra.addFlashAttribute("otpSent", false);
                session.removeAttribute("resetEmail");
                session.removeAttribute("otpSent");
                if ("MAX_LIMIT_REACHED".equals(e.getMessage())) {
                    ra.addFlashAttribute("msg", "Max limit reached! Please try after 5 minutes.");
                } else {
                    ra.addFlashAttribute("msg", "Mail Server Down! Try after some time.");
                    System.out.println("Error is: " + e.getMessage());
                }
            }
            return "redirect:/public/forgotpassword";

        }
    }

    @PostMapping("/do_password")
    public String chnagePasswordHandler(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("cnfpassword") String cnfpassword,
            @RequestParam("otp") String otp,
            Model model, RedirectAttributes ra, HttpSession session) {

        Boolean otpSent = (Boolean) session.getAttribute("otpSent");
        String emailReset = (String) session.getAttribute("resetEmail");

        String passRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,500}$";

        if (!password.equals(cnfpassword) && otpSent != null & otpSent) {
            model.addAttribute("msg", "Passwords do not match!");
            model.addAttribute("otpSent", true);
            model.addAttribute("email", emailReset);
            return "public/forgotpassword";
        } else if (!password.matches(passRegex)) {
            model.addAttribute("msg", "Password must be 8+ chars with Upper, Lower, Number & Special char!");
            model.addAttribute("otpSent", true);
            model.addAttribute("email", emailReset);
            return "public/forgotpassword";
        }

        boolean isValid = userService.verifyOtp(email, otp);
        if (isValid) {
            User updt = userService.updatePassword(email, password);
            System.out.println("User Status after update: " + updt.getStatus());
            System.out.println("Update Successful");
            ra.addFlashAttribute("msg", "Password updated successfully! Please login.");
            ra.addFlashAttribute("type", "alert-success");
            session.removeAttribute("otpSent");
            session.removeAttribute("resetEmail");
            return "redirect:/public/login";
        } else {
            ra.addFlashAttribute("msg", "Invalid or Expired OTP!");
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("email", email);
            return "redirect:/public/forgotpassword";
        }
    }

    @PostMapping("/contact_process")
    public String contactusHandler(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            HttpServletRequest request,
            RedirectAttributes ra) {

        HttpSession session = request.getSession();
        long currentTime = System.currentTimeMillis();
        boolean shouldBlock = false;

        synchronized (session.getId().intern()) {
            Long lastSent = (Long) session.getAttribute("last_contact_time");
            if (lastSent != null && (currentTime - lastSent < 60000)) {
                shouldBlock = true;
            } else {
                session.setAttribute("last_contact_time", currentTime);
            }

        }

        if (shouldBlock) {
            session.setAttribute("contact_msg", "blocked");
            return "redirect:/";
        }
        try {
            emailService.sendEmail(name, email, subject, message);
            session.setAttribute("contact_msg", "success");
            return "redirect:/";

        } catch (Exception e) {
            log.error("Mail Error: {}", e.getMessage());
            ra.addFlashAttribute("msgTitle", "Something Wrong");
            ra.addFlashAttribute("msgDash", "Something Error Occured!");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/public/about";
        }

    }

    @PostMapping("/resend-verification")
    public String verificationEmail(RedirectAttributes ra, HttpServletRequest request) {

        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("LOGIN_EMAIL");

        if (email == null) {
            ra.addFlashAttribute("msg", "Session expired. Please login again.");
            return "redirect:/public/login";
        }

        try {
            int attemptsLeft = userService.reVerifyEmail(email);
            ra.addFlashAttribute("showResend", true);
            ra.addFlashAttribute("msg", "Verification email sent!");
            ra.addFlashAttribute("type", "success");
            ra.addFlashAttribute("attemptsLeft", attemptsLeft);

        } catch (MaxAttemptsException e) {
            ra.addFlashAttribute("msg", e.getMessage());
            ra.addFlashAttribute("showResend", false);
            ra.addFlashAttribute("type", "error");
            ra.addFlashAttribute("attemptsLeft", 0);
            return "redirect:/public/login";
        }

        return "redirect:/public/login";
    }
}