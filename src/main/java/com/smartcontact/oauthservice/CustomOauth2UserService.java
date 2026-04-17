package com.smartcontact.oauthservice;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smartcontact.config.CustomUserPrincipal;
import com.smartcontact.entities.User;
import com.smartcontact.service.UserService;

@Service
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    private static final Logger log = LoggerFactory.getLogger(CustomOauth2UserService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("------ GITHUB OAuth2 SERVICE ------");

        OAuth2User oauthUser = super.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");

        // 🔥 GitHub email null fix
        if (email == null) {
            email = fetchGithubEmail(userRequest.getAccessToken().getTokenValue());
        }

        if (email == null || email.contains("noreply.github.com")) {
            throw new OAuth2AuthenticationException(
                    "GitHub email is private. Please make it public or use Google login.");
        }

        Object idObj = oauthUser.getAttribute("id");
        String providerId = (idObj != null) ? idObj.toString() : null;

        String name = oauthUser.getAttribute("name");
        if (name == null) {
            name = oauthUser.getAttribute("login");
        }

        String picture = oauthUser.getAttribute("avatar_url");

        log.info("GitHub Email: {} ", email);

        User user = userService.processOauthUser(
                email,
                name,
                picture,
                providerId,
                "GITHUB");

        if (!"active".equalsIgnoreCase(user.getStatus()) || user.getVerificationToken() != null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("disabled", "Account is not active", null));
        }

        return new CustomUserPrincipal(
                user.getEmail(),
                null,
                AuthorityUtils.createAuthorityList(user.getRole()),
                oauthUser.getAttributes(),
                null,
                null);
    }

    // 🔥 GitHub Email Fetch Helper
    private String fetchGithubEmail(String accessToken) {

        String url = "https://api.github.com/user/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {
                });

        List<Map<String, Object>> emails = response.getBody();

        if (emails != null) {
            for (Map<String, Object> e : emails) {
                Boolean primary = (Boolean) e.get("primary");
                Boolean verified = (Boolean) e.get("verified");

                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                    return (String) e.get("email");
                }
            }
        }

        return null;
    }
}
