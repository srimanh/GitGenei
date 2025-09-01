package com.example.demo.controller;

import com.example.demo.service.ProgressTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/progress")
@CrossOrigin(origins = "http://localhost:3001", allowCredentials = "true")
public class ProgressController {

    @Autowired
    private ProgressTrackingService progressTrackingService;

    @MessageMapping("/subscribe")
    @SendToUser("/queue/progress")
    public Map<String, Object> subscribeToProgress(@Payload Map<String, String> payload, Principal principal) {
        String fileId = payload.get("fileId");
        String userId = principal.getName();
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "SUBSCRIPTION_CONFIRMED");
        response.put("fileId", fileId);
        response.put("userId", userId);
        response.put("message", "Subscribed to progress updates for " + fileId);
        
        return response;
    }

    @GetMapping("/status/{fileId}")
    @ResponseBody
    public ResponseEntity<?> getProgressStatus(
            @PathVariable String fileId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            Map<String, Object> progressStatus = progressTrackingService.getProgressStatus(fileId);
            
            if (progressStatus == null) {
                return ResponseEntity.notFound().build();
            }

            // Verify the user owns this progress session
            if (!userId.equals(progressStatus.get("userId"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            return ResponseEntity.ok(progressStatus);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get progress status: " + e.getMessage()));
        }
    }

    @PostMapping("/test/{fileId}")
    @ResponseBody
    public ResponseEntity<?> testProgress(
            @PathVariable String fileId,
            @RequestBody Map<String, Object> testData,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            
            // Start a test progress session
            progressTrackingService.startProgressSession(fileId, userId);
            
            // Simulate progress updates
            simulateProgressUpdates(fileId, userId);
            
            return ResponseEntity.ok(Map.of("message", "Test progress started for " + fileId));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start test progress: " + e.getMessage()));
        }
    }

    private void simulateProgressUpdates(String fileId, String userId) {
        new Thread(() -> {
            try {
                // Simulate upload progress
                for (int i = 10; i <= 30; i += 5) {
                    Thread.sleep(500);
                    progressTrackingService.updateUploadProgress(fileId, userId, i, i * 1024 * 1024, 100 * 1024 * 1024);
                }

                // Simulate extraction
                Thread.sleep(1000);
                progressTrackingService.updateExtractionProgress(fileId, userId, 40, "package.json");
                Thread.sleep(500);
                progressTrackingService.updateExtractionProgress(fileId, userId, 50, "src/main.js");

                // Simulate AI analysis
                Thread.sleep(1000);
                Map<String, Object> analysisData = new HashMap<>();
                analysisData.put("detectedLanguages", new String[]{"JavaScript", "TypeScript"});
                analysisData.put("detectedFrameworks", new String[]{"React", "Node.js"});
                progressTrackingService.updateAnalysisProgress(fileId, userId, 70, "Detecting project structure", analysisData);

                Thread.sleep(1000);
                progressTrackingService.updateAnalysisProgress(fileId, userId, 80, "Creating branch suggestions", analysisData);

                // Simulate GitHub integration
                Thread.sleep(1000);
                progressTrackingService.updateGitHubProgress(fileId, userId, 90, "Creating repository", "https://github.com/user/test-repo");

                Thread.sleep(1000);
                progressTrackingService.updateGitHubProgress(fileId, userId, 95, "Pushing branches", "https://github.com/user/test-repo");

                // Complete
                Thread.sleep(1000);
                Map<String, Object> finalData = new HashMap<>();
                finalData.put("repoUrl", "https://github.com/user/test-repo");
                finalData.put("branchesCreated", new String[]{"main", "frontend", "backend"});
                finalData.put("totalFiles", 42);
                progressTrackingService.completeProgress(fileId, userId, finalData);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                progressTrackingService.errorProgress(fileId, userId, "Simulation interrupted", null);
            } catch (Exception e) {
                progressTrackingService.errorProgress(fileId, userId, "Simulation error: " + e.getMessage(), null);
            }
        }).start();
    }
}
