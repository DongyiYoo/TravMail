package com.travmail.travmail.Controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/") 
    public String profile(@AuthenticationPrincipal OAuth2User principal, Model model) {
        
        //when not logged in
        if (principal == null) {
            return "login"; 
        }

        // when logged in
        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("name"));
            model.addAttribute("email", principal.getAttribute("email"));
            model.addAttribute("photo", principal.getAttribute("picture")); 
        }

        return "userProfile"; 
    }
}