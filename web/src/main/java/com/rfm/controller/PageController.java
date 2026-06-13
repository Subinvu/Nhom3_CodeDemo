package com.rfm.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Xử lý routing trang Thymeleaf.
 * KHÔNG chứa business logic — chỉ trả về tên template.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER"))) {
            return "redirect:/order";
        }
        return "index";
    }

    @GetMapping("/order")
    public String order() {
        return "order";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/customers")
    public String customers() {
        return "customers";
    }

    @GetMapping("/orders")
    public String ordersPage() {
        return "orders";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
