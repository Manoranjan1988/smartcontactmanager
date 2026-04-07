package com.smartcontact.config;

import com.smartcontact.entities.User;
import com.smartcontact.repository.UserRepository;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserStatusFIlter extends OncePerRequestFilter {

    
    @Autowired
    private UserRepository userRepository;
 
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain)throws ServletException, IOException {
        
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();  

          if(auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)){
                String email = auth.getName();
                User user = userRepository.getUserByUserName(email);

                if(user != null && "inactive".equals(user.getStatus())){
                    new SecurityContextLogoutHandler().logout(request, response, auth);
                    response.sendRedirect("/public/login?error=disabled");
                    return;
                }
          }
          filterChain.doFilter(request, response);
    }

}
