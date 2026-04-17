package com.smartcontact.config;

import com.smartcontact.service.UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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

    @Autowired
    private OAuth2AuthorizedClientService clientService;

    @Override
public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication)
        throws IOException, ServletException {

    try {

        if (authentication instanceof OAuth2AuthenticationToken token) {

            String registrationId = token.getAuthorizedClientRegistrationId();
            String principalName = token.getName();

            log.info("=== SUCCESS HANDLER DEBUG ===");
            log.info("Registration ID: {}", registrationId);
            log.info("Principal: {}", principalName);
            log.info("Authorities BEFORE: {}", authentication.getAuthorities());

            // LOAD CLIENT (SAFE)
            OAuth2AuthorizedClient client =
                    clientService.loadAuthorizedClient(registrationId, principalName);

            if (client != null) {
                log.info("CLIENT FOUND");
                log.info("Scopes: {}", client.getAccessToken().getScopes());
            } else {
                log.warn("CLIENT NULL");
            }

            //ADD ROLE_USER SAFELY (WITHOUT BREAKING PRINCIPAL)
            Collection<GrantedAuthority> updatedAuthorities =
                    new ArrayList<>(token.getAuthorities());

            if (updatedAuthorities.stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_USER"))) {

                updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                OAuth2AuthenticationToken newAuth =
                        new OAuth2AuthenticationToken(
                                token.getPrincipal(),    
                                updatedAuthorities,
                                token.getAuthorizedClientRegistrationId()
                        );

                SecurityContextHolder.getContext().setAuthentication(newAuth);

                log.info("ROLE_USER injected");
            }

            log.info("Authorities AFTER: {}",
                    SecurityContextHolder.getContext().getAuthentication().getAuthorities());

            // ===============================
            // GOOGLE CONTACTS FLOW
            // ===============================
            if ("google-contacts".equals(registrationId)) {

                log.info("Google Contacts Flow - skipping DB + redirect");

                //direct redirect ONLY ONCE
                response.sendRedirect("/user/import-contacts");
                return;
            }
        }

        // ===============================
        // NORMAL LOGIN FLOW
        // ===============================
        String email = "";
        String picture = "";
        String providerId = "";
        String name = "";
        String providerName = "SELF";

        Object principal = authentication.getPrincipal();

        if (authentication instanceof OAuth2AuthenticationToken token) {

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
                name = oauthUser.getAttribute("name") != null
                        ? oauthUser.getAttribute("name").toString()
                        : oauthUser.getAttribute("login").toString();
                picture = oauthUser.getAttribute("avatar_url");
                providerId = oauthUser.getAttribute("id").toString();
            }
        } else {
            email = authentication.getName();
        }

        //(NO VALIDATION ERROR)
        if (email == null || email.isBlank()) {
            log.error("EMAIL NULL - aborting user creation");
            response.sendRedirect("/public/login?error=email_missing");
            return;
        }

        if (name == null || name.isBlank()) {
            name = "User"; // fallback to avoid validation crash
        }

        User dbUser = userService.processOauthUser(
                email, name, picture, providerId, providerName);

        log.info("All Sessions: {}",sessionRegistry.getAllPrincipals());
        log.info("LOGIN USER: {}", dbUser.getEmail());

        response.sendRedirect("/user/dashboard_home");

    } catch (Exception e) {
        log.error("!!! CRITICAL ERROR !!! ", e);
        response.sendRedirect("/public/login?error=handler_exception");
    }
}
}