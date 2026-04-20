package com.smartcontact.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SessionUtils{
    public static final String LOGIN_EMAIL = "LOGIN_EMAIL";

    public void saveEmailInSession(String email){
        ServletRequestAttributes attr =
            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        
        attr.getRequest().getSession().setAttribute(LOGIN_EMAIL, email);
    }
}
