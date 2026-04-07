package com.smartcontact.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);
    @Override
    public void onLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication != null) {
           log.info("User Logged Out: {}" , authentication.getName());
        }

          
        String status = request.getParameter("status");
        if ("password_updated".equals(status)) {

            response.sendRedirect("/public/login?update=success");

        } else {

            response.sendRedirect("/public/login?logout=success");

        }

    }

}
