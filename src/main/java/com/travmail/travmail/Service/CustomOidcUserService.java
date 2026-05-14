package com.travmail.travmail.Service;

import com.travmail.travmail.Entity.User;
import com.travmail.travmail.Repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    // get the repository
    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // when google login succeed
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        try {
            // get user details
            OidcUser oidcUser = super.loadUser(userRequest);

            // get user email,name
            String email = oidcUser.getAttribute("email");
            String name = oidcUser.getAttribute("name");

            // get token
            String token = userRequest.getAccessToken().getTokenValue();

            System.out.println("LOG CHECK: " + email + " has logged in.");

            // save/update user
            User user = userRepository.findByEmail(email).orElse(new User());
            user.setEmail(email);
            user.setName(name);
            user.setAccessToken(token);

            // save
            userRepository.save(user);

            return oidcUser;

        } catch (Exception e) {
            System.err.println("Failed to persist user data.");
            e.printStackTrace(); 
            throw e;
        }
    }
}