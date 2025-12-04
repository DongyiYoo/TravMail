package com.travmail.travmail;

import com.travmail.travmail.Service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    // get CustomOAuth2UserService
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(registry -> {
            registry.requestMatchers("/").permitAll(); // everyone gets permit for the mainpage
            registry.anyRequest().authenticated();     // need login for the rest
        })
            .oauth2Login(oauth2 -> oauth2
                // use userservice when login
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService) 
                )
                .defaultSuccessUrl("/",true) 
            )
            // logout
            .logout(logout -> logout
            .logoutUrl("/logout")       
            .logoutSuccessUrl("/login")      
            .invalidateHttpSession(true) 
            .deleteCookies("JSESSIONID") 
            .permitAll()
            )

         .build();
        }
    }