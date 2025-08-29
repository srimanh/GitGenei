package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String githubId = String.valueOf(principal.getAttributes().get("id"));
        Optional<User> userOpt = userRepository.findByGithubId(githubId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "githubUsername", user.getGithubUsername(),
                "avatarUrl", user.getAvatarUrl()
            ));
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    @GetMapping("/github/repos")
    public ResponseEntity<?> getUserRepositories(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String githubId = String.valueOf(principal.getAttributes().get("id"));
        Optional<User> userOpt = userRepository.findByGithubId(githubId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            try {
                Object repos = gitHubService.getUserRepositories(user.getAccessToken());
                return ResponseEntity.ok(repos);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch repositories"));
            }
        }

        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
