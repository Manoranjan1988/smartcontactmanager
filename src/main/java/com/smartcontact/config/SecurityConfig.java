package com.smartcontact.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import com.smartcontact.oauthservice.CustomOauth2UserService;
import com.smartcontact.oauthservice.CustomOidcUserService;



@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService getUserDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(getUserDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Autowired
    private CustomAuthenticationFailureHandler failureHandler;
    @Autowired
    private CustomLogoutSuccessHandler logoutHandler;
    @Autowired
    private CustomLoginSuccessHandler successHandler;
    @Autowired
    private CustomOidcUserService customOidcService;
    @Autowired
    private CustomOauth2UserService customOAuth2Service;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/public/**", "/css/**", "/image/**", "/images/**", "/js/**", "/favicon.ico","/error/**","/.well-known/**","/api/webhook/**")
                .permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER","ADMIN")
                .anyRequest().authenticated()

        ).requestCache(cache -> cache.disable()
    
        ).formLogin(form -> form
                .loginPage("/public/login")
                .loginProcessingUrl("/public/dologin")
                .successHandler(successHandler)
                .failureHandler(failureHandler)

        ).oauth2Login(oauth -> oauth
                .loginPage("/public/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOidcService)
                    .userService(customOAuth2Service)
                )
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            ).oauth2Client(Customizer.withDefaults()
        ).csrf(csrf -> csrf
                 .ignoringRequestMatchers("/user/create_order","/api/webhook/**","/user/processContact", "/user/processUpdate", "/user/update-profile","/user/send-email-blast")

        ).headers(headers -> headers
            .cacheControl(cache ->{})
        )
        
        .logout(logout -> logout
                .logoutUrl("/user/do-logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessHandler(logoutHandler)
                .deleteCookies("JSESSIONID","XSRF-TOKEN")
                .permitAll()

        ).sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/public/login?session=expired")
                .sessionRegistry(sessionRegistry())
                
        );
        return http.build();
    }
}
