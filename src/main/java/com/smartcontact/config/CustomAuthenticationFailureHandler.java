package com.smartcontact.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        log.info("=== FAILURE HANDLER START ===");
        log.info("Exception type: {}", exception.getClass().getSimpleName());
        String errorType = "bad";

        if (exception instanceof DisabledException) {
            errorType = "disabled";
        } else if (exception instanceof BadCredentialsException) {
            errorType = "bad";
        } else if (exception instanceof SessionAuthenticationException) {
            errorType = "concurrent";
        } else if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) exception;
            String provider = null;
            if(oauthEx.getError() != null){
                provider = oauthEx.getError().getDescription();
            }
            log.info("Provider from exception: {}", provider);
            
            if ("SELF".equals(provider)) {
                errorType = "oauth_self";
            } else if ("GOOGLE".equals(provider)) {
                errorType = "oauth_google";
            } else if ("GITHUB".equals(provider)) {
                errorType = "oauth_github";
            } else {
                errorType = "oauth";
            }
        }

        String redirectUrl = request.getContextPath() + "/public/login?error=" + errorType;

        log.info("Redirecting to: {}", redirectUrl);
        log.info("=== FAILURE HANDLER END ===");

        response.sendRedirect(redirectUrl);
    }

}
