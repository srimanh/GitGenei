package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GitHubService {

    @Value("${github.personal-access-token}")
    private String personalAccessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public Object getUserRepositories(String accessToken) {
        String url = "https://api.github.com/user/repos?sort=updated&per_page=50";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Object.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch repositories from GitHub", e);
        }
    }

    public Object getUserProfile(String accessToken) {
        String url = "https://api.github.com/user";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Object.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile from GitHub", e);
        }
    }

    public Object getRepositoryContents(String owner, String repo, String path, String accessToken) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Object.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch repository contents from GitHub", e);
        }
    }
}
