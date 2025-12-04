package com.travmail.travmail.Service;

import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service 
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    // get the repository
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // when google login succeed
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // get user details
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // get user email,name
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        // get token
        String token = userRequest.getAccessToken().getTokenValue();

        System.out.println("LOG CHECK: " + email + " has logged in.");
        System.out.println("TOKEN CHECK: " + token); 

        // save/update user
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setEmail(email);
        user.setName(name);
        user.setAccessToken(token); 

        // save
        userRepository.save(user);

        return oAuth2User;
    }
}
