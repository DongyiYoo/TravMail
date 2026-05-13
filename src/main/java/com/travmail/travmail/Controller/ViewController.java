package com.travmail.travmail.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.TravMailRepository;
import com.travmail.travmail.Repository.UserRepository;

@Controller
public class ViewController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravMailRepository travMailRepository;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OAuth2User principal, Model model) {

        // when logged in
        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("name"));
        }
        return "index";
    }

    @GetMapping("/manage")
    public String manageEmails(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return "redirect:/oauth2/authorization/google";
        }

        // find user
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // search user's travmail
            model.addAttribute("mails", travMailRepository.findByUser(user));
        }
        model.addAttribute("name", name);

        return "manage";

    }
}