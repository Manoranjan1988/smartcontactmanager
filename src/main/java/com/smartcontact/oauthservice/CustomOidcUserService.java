package com.smartcontact.oauthservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

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
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from Google");
        }
        log.info("OIDC User: {} ", email);

        User user = userService.processOauthUser(
                email,
                oidcUser.getFullName(),
                oidcUser.getPicture(),
                oidcUser.getSubject(),
                "GOOGLE");
        log.info("User after processing: {}", user.getEmail());
        return new CustomUserPrincipal(
                email,
                null,
                AuthorityUtils.createAuthorityList(user.getRole()),
                oidcUser.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo());
    }

}