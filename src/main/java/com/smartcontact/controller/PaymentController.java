package com.smartcontact.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcontact.entities.MyOrder;
import com.smartcontact.entities.User;
import com.smartcontact.service.PaymentService;
import com.smartcontact.service.UserService;

@Controller
@RequestMapping("/user")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    UserService userService;

    @Autowired
    PaymentService paymentService;

  

    @GetMapping("/donate")
    public String paymentHandler(Model model, Principal principal) {
        model.addAttribute("title", "Donation | SCM");
        if (principal == null) {
            return "redirect:/public/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (!user.equals(null)) {
            model.addAttribute("user", user);
            return "user/donate";
        } else {
            return "redirect:/public/login";
        }

    }

    @PostMapping("/create_order")
    @ResponseBody // (Return in JSON format)
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> data, Principal principal) {
        log.info("Create order API Hit");
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Get the Amount from JS
            User user = userService.getUserByEmail(principal.getName());
            log.info("Order request by user: {}", user.getEmail());
            int amount = Integer.parseInt(data.get("amount").toString());
            if (amount < 1) {
                response.put("msg", "Minimum Rupees 1/- allowed");
                response.put("status", "error");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            log.info("Requested donation amount is: {}", amount);
            Map<String, Object> orderData = paymentService.createOrder(amount, principal);
            orderData.put("userName", user.getName());
            orderData.put("userEmail", user.getEmail());
            orderData.put("status", "success");
            // 3. Success Response bhejo JSON format mein
            return ResponseEntity.ok(orderData);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            log.error("Error during Order Creation: {}", e);
            error.put("message", "Order creation failed: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/order_history")
    public String orderHistoryHandler(
            @RequestParam(defaultValue = "0") int currentPage,
            Model model, Principal principal,RedirectAttributes ra) {

        String email = principal.getName();
        if (email == null) {
            return "redirect:/public/login";
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
        User user = userService.getUserByEmail(principal.getName());
        model.addAttribute("user", user);

        try {
            Page<MyOrder> allOrders = paymentService.getAllOrders(currentPage, email);

        if (currentPage >= allOrders.getTotalPages() && allOrders.getTotalPages() > 0) {
            currentPage = allOrders.getTotalPages() - 1;
            allOrders = paymentService.getAllOrders(currentPage, email);
        }
            model.addAttribute("orders", allOrders);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", allOrders.getTotalPages());
            return "user/order_history";
        } catch (Exception e) {
            ra.addFlashAttribute("msg", "Invalid Page! Redirecting to Home.");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/order_history?currentPage=0";
        }
        
    }
}
