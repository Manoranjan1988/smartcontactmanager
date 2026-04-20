package com.smartcontact.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcontact.dto.ImportProgress;
import com.smartcontact.entities.User;
import com.smartcontact.googleservice.ImportAsyncService;
import com.smartcontact.googleservice.ImportProgressService;
import com.smartcontact.service.UserService;

@Controller
@RequestMapping("/user")

public class GoogleContactController {

    private static final Logger log = LoggerFactory.getLogger(GoogleContactController.class);

    @Autowired
    private OAuth2AuthorizedClientService clientService;

    @Autowired
    private UserService userService;

    @Autowired
    private ImportProgressService progressService;

    @Autowired
    private ImportAsyncService importAsyncService;

    @GetMapping("/import-contacts")
    public String importContacts(Authentication authentication) {

        log.info("=== IMPORT CONTROLLER HIT ===");

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

        if (client == null) {
            log.warn("CLIENT NULL → redirecting to OAuth");
            return "redirect:/oauth2/authorization/google-contacts?prompt=consent";
        }

        String token = client.getAccessToken().getTokenValue();
        log.info("TOKEN: {}", token);

        return "redirect:/user/dashboard?import=success"; 
    }

    @GetMapping("/test-api")
    public String testGoogleApi(
            @RegisteredOAuth2AuthorizedClient("google-contacts") OAuth2AuthorizedClient client,
            Principal principal,
            Authentication authentication,
            RedirectAttributes ra) {

        String token = client.getAccessToken().getTokenValue();

        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            email = oauthUser.getAttribute("email");
        } else {
            email = principal.getName();
        }

        User user = userService.getUserByEmail(email);

        if (user == null) {
            return "redirect:/public/login";
        }

        log.info("Starting async import for user: {}", email);

        importAsyncService.startFullImport(user, token);

        return "redirect:/user/dashboard_home";
    }

    @GetMapping("/test-progress")
    @ResponseBody
    public ImportProgress test(Principal p) {
        User user = userService.getUserByEmail(p.getName());
        return progressService.get(user.getId());
    }
}
