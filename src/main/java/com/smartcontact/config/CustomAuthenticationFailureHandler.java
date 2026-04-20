package com.smartcontact.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Component;

import com.smartcontact.entities.User;
import com.smartcontact.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        log.info("=== FAILURE HANDLER START ===");
        log.error("Exception type:{} ", exception.getClass().getSimpleName());
        String errorType = "bad";

        String email = (String) request.getSession().getAttribute(SessionUtils.LOGIN_EMAIL);        
        request.getSession().removeAttribute(SessionUtils.LOGIN_EMAIL);
        
        if (email == null) {
            email = request.getParameter("username");
            request.getSession().setAttribute("LOGIN_EMAIL", email);
        }

        log.info("Failure Handler Email: {}", email);

        if (exception instanceof DisabledException) {
            errorType = "disabled";
        } else if (exception instanceof BadCredentialsException) {
            errorType = "bad";
        } else if (exception instanceof SessionAuthenticationException) {
            errorType = "concurrent";
        } else if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException ex = (OAuth2AuthenticationException) exception;
            String code = ex.getError().getErrorCode();

            log.error("OAuth Code: {}", ex.getError().getErrorCode());
            log.error("OAuth Desc: {}", ex.getError().getDescription());
            if ("disabled".equals(code) && email != null) {
                User dbuser = userService.getUserByEmail(email);
                if (dbuser != null && !"SELF".equals(dbuser.getProvided().toString())) {
                    errorType = "inactive_user";
                } else {
                    errorType = "disabled";
                }
            } else if ("email_not_verified".equals(code)) {
                errorType = "verify_email";
            } else {
                errorType = "oauth";
            }
        } else {
            errorType = "error";
            log.error("Unhandled auth exception: {}", exception.getClass().getName(), exception);
        }

        String redirectUrl = request.getContextPath() + "/public/login";
        if (errorType != null && !errorType.isEmpty()) {
            redirectUrl += "?error=" + errorType;
        }

        log.info("Redirecting to: {}", redirectUrl);
        log.info("=== FAILURE HANDLER END ===");

        response.sendRedirect(redirectUrl);
    }

}
