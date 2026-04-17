package com.smartcontact.controller;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcontact.config.CustomUserPrincipal;
import com.smartcontact.entities.User;
import com.smartcontact.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    @Autowired
    UserService userService;

    @GetMapping("/manage_users")
    public String manageUsers(Model model, Principal principal) {
        try {
            model.addAttribute("title", "Manager Users | SCM");

            User adminUser = userService.getUserByEmail(principal.getName());
            log.info("The admin user is: {}", adminUser.getEmail());
            List<User> allUsers = userService.findAllExceptAdmin();

            model.addAttribute("users", allUsers);
            model.addAttribute("user", adminUser);
            return "admin/manage_users";
        } catch (Exception e) {
            log.info("Error in Admin Manage Users: {}" + e);
            return "redirect:/user/dashboard_home";
        }

    }

    @GetMapping("update-status/{id}")
    public String adminStausUpdate(
            @PathVariable("id") Long id,
            RedirectAttributes ra, Principal principal) {

        String username = "";
        Collection<? extends GrantedAuthority> authorities = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            // CASE 1: Normal Form Login
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) auth.getPrincipal();

            username = userPrincipal.getUsername();
            authorities = userPrincipal.getAuthorities();

        } else if (principal instanceof OAuth2AuthenticationToken) {
            // CASE 2: Google / GitHub OAuth2 Login
            OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) principal;
            OAuth2User oauth2User = oauth.getPrincipal();

            // Google ke liye "name" ya "email" uthate hain
            username = oauth2User.getAttribute("name");
            if (username == null)
                username = oauth2User.getAttribute("email");

            authorities = oauth.getAuthorities();
        }
        log.info("Logged in User Name: {}", username);
        log.info("User Authorities: {}", authorities);
        String loggedEmail = principal.getName();

        User user = userService.findByUserId(id);

        if (loggedEmail.equals(user.getEmail()) || "ROLE_ADMIN".equals(user.getRole())) {
            ra.addFlashAttribute("msg", "Action Restricted: Admins cannot be modified!");
            ra.addFlashAttribute("type", "error");
            return "redirect:/admin/manage_users";
        }

        if (user.getStatus().equals("active")) {
            user.setStatus("inactive");
            ra.addFlashAttribute("msg", "User: " + user.getName() + " is now INACTIVE!");
        } else {
            user.setStatus("active");
            ra.addFlashAttribute("msg", "User: " + user.getName() + " is now ACTIVE!");
        }
        userService.updateUserStatus(user);
        ra.addFlashAttribute("type", "success");
        return "redirect:/admin/manage_users";
    }
}
