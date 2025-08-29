package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Process GitHub user data
        processOAuth2User(userRequest, oauth2User);
        
        return oauth2User;
    }

    private void processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String githubId = String.valueOf(attributes.get("id"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String githubUsername = (String) attributes.get("login");
        String avatarUrl = (String) attributes.get("avatar_url");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // Check if user already exists
        User user = userRepository.findByGithubId(githubId)
                .orElse(new User());

        // Update user information
        user.setGithubId(githubId);
        user.setEmail(email != null ? email : githubUsername + "@github.local");
        user.setName(name != null ? name : githubUsername);
        user.setGithubUsername(githubUsername);
        user.setAvatarUrl(avatarUrl);
        user.setAccessToken(accessToken);

        userRepository.save(user);
    }
}
