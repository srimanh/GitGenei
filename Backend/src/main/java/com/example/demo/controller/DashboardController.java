package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GitHubService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3001", allowCredentials = "true")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String githubId = String.valueOf(principal.getAttributes().get("id"));
        Optional<User> userOpt = userRepository.findByGithubId(githubId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("email", user.getEmail());
            profile.put("name", user.getName());
            profile.put("githubUsername", user.getGithubUsername());
            profile.put("avatarUrl", user.getAvatarUrl());
            profile.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(profile);
        }

        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }

    @GetMapping("/repositories")
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
                return ResponseEntity.ok(Map.of("repositories", repos));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch repositories: " + e.getMessage()));
            }
        }

        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String githubId = String.valueOf(principal.getAttributes().get("id"));
        Optional<User> userOpt = userRepository.findByGithubId(githubId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            try {
                Object repos = gitHubService.getUserRepositories(user.getAccessToken());
                Object profile = gitHubService.getUserProfile(user.getAccessToken());
                
                Map<String, Object> stats = new HashMap<>();
                stats.put("profile", profile);
                stats.put("totalRepositories", repos);
                
                return ResponseEntity.ok(stats);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch stats: " + e.getMessage()));
            }
        }

        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }
}
