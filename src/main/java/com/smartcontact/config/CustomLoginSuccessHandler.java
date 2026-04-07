package com.smartcontact.config;

import com.smartcontact.service.UserService;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smartcontact.entities.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
private static final Logger log = LoggerFactory.getLogger(CustomLoginSuccessHandler.class);
    @Autowired
    private UserService userService;

    @Autowired
    @Lazy
    private SessionRegistry sessionRegistry;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        try {
            
            String email = "";
            String picture = "";
            String providerId = "";
            String name = "";
            String providerName = "SELF";

            Object principal = authentication.getPrincipal();

            // 1. Sirf Data Extract Karo
            if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token) {
                
                providerName = token.getAuthorizedClientRegistrationId().toUpperCase();
                OAuth2User oauthUser = (OAuth2User) principal;

                if ("GOOGLE".equals(providerName)) {
                    email = oauthUser.getAttribute("email");
                    name = oauthUser.getAttribute("name");
                    picture = oauthUser.getAttribute("picture");
                    providerId = oauthUser.getAttribute("sub");
                } else if ("GITHUB".equals(providerName)) {
                    email = oauthUser.getAttribute("email");
                    if (email == null || email.isEmpty()) {
                        email = oauthUser.getAttribute("login") + "@github.com";
                    }
                    name = (oauthUser.getAttribute("name") != null) ? oauthUser.getAttribute("name").toString(): oauthUser.getAttribute("login").toString();
                    picture = oauthUser.getAttribute("avatar_url");
                    providerId = oauthUser.getAttribute("id").toString();
                }
            } else {
                email = authentication.getName();
            }

            User dbUser = userService.processOauthUser(email, name, picture, providerId, providerName);

            // 3. Security Context update aur Redirect
            if (dbUser != null) {
                
                log.info("LOGIN USER: {}",authentication.getName());

                log.info("Step 5: Authorities Set! Redirecting...");
                authentication.getAuthorities().forEach(auth -> 
                log.info("ROLE: {}", auth.getAuthority()));

                log.info("LOGIN SESSION ID: {}",request.getSession().getId());
                log.info("All Sessions: {}",sessionRegistry.getAllPrincipals());

                getRedirectStrategy().sendRedirect(request, response, "/user/dashboard_home");

            }

        } catch (Exception e) {
            log.error("!!! CRITICAL ERROR !!! ",e);
            response.sendRedirect("/public/login?error=handler_exception");
        }
    }
}