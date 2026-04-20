package com.smartcontact.oauthservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.smartcontact.config.CustomUserPrincipal;
import com.smartcontact.entities.User;
import com.smartcontact.service.UserService;

@Service
public class CustomOidcUserService extends OidcUserService {
    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    @Autowired
    private UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("------------OIDC User Service Started------------");

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_email", "Email not found", null));
        }

        log.info("OIDC User: {}", email);

        User user = null;

        
        if (!"google-contacts".equals(registrationId)) {

            Boolean emailVerified = oidcUser.getEmailVerified();

            if (Boolean.FALSE.equals(emailVerified)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_email", "Google email not verified", null));
            }

            user = userService.processOauthUser(
                    email,
                    oidcUser.getFullName(),
                    oidcUser.getPicture(),
                    oidcUser.getSubject(),
                    "GOOGLE");

            log.info("User after processing: {}", user.getEmail());

            if (!"active".equalsIgnoreCase(user.getStatus()) || user.getVerificationToken() != null) {
                ServletRequestAttributes attr 
                = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                attr.getRequest().getSession().setAttribute("LOGIN_EMAIL", email);
                log.info("Github user email saved in session: {}", email);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("disabled", "Account is not active", null));
            }

        } else {
            
            log.info("Google-contacts flow: skipping DB save");
        }

        
        return new CustomUserPrincipal(
                email,
                null,
                AuthorityUtils.createAuthorityList(
                        user != null ? user.getRole() : "ROLE_USER"),
                oidcUser.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo());
    }
}