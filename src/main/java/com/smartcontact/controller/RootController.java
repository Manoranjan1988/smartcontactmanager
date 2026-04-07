package com.smartcontact.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class RootController {
    @GetMapping("/")
    public String rootHomeHandler(Model model, HttpSession session) {
        model.addAttribute("title", "Home - Smart Contact Manager");

        String msgType = (String) session.getAttribute("contact_msg");
        if (msgType != null) {
            if ("success".equals(msgType)) {
                model.addAttribute("msgTitle", "Inquiry Sent");
                model.addAttribute("msgDash", "Mail Sent Successfully!");
                model.addAttribute("msgType", "success");
            }
             else if ("blocked".equals(msgType)) {
                model.addAttribute("msgTitle", "Wait!");
                model.addAttribute("msgDash", "Please wait 1 minute before sending another inquiry.");
                model.addAttribute("msgType", "error");

            }
            session.removeAttribute("contact_msg");

        }
        return "public/home";

    }
}
