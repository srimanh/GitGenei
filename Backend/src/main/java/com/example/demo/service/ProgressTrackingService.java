package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressTrackingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Store progress for each file upload session
    private final Map<String, ProgressSession> progressSessions = new ConcurrentHashMap<>();

    public void startProgressSession(String fileId, String userId) {
        ProgressSession session = new ProgressSession(fileId, userId);
        progressSessions.put(fileId, session);
        
        // Send initial progress update
        sendProgressUpdate(fileId, userId, "STARTED", 0, "Upload session started", null);
    }

    public void updateProgress(String fileId, String userId, String stage, int percentage, String message, Map<String, Object> data) {
        ProgressSession session = progressSessions.get(fileId);
        if (session != null) {
            session.updateProgress(stage, percentage, message, data);
            sendProgressUpdate(fileId, userId, stage, percentage, message, data);
        }
    }

    public void completeProgress(String fileId, String userId, Map<String, Object> finalData) {
        ProgressSession session = progressSessions.get(fileId);
        if (session != null) {
            session.complete(finalData);
            sendProgressUpdate(fileId, userId, "COMPLETED", 100, "Process completed successfully", finalData);
            
            // Clean up session after a delay
            cleanupSession(fileId, 300000); // 5 minutes
        }
    }

    public void errorProgress(String fileId, String userId, String errorMessage, Map<String, Object> errorData) {
        ProgressSession session = progressSessions.get(fileId);
        if (session != null) {
            session.error(errorMessage, errorData);
            sendProgressUpdate(fileId, userId, "ERROR", session.getPercentage(), errorMessage, errorData);
            
            // Clean up session after a delay
            cleanupSession(fileId, 60000); // 1 minute for errors
        }
    }

    private void sendProgressUpdate(String fileId, String userId, String stage, int percentage, String message, Map<String, Object> data) {
        Map<String, Object> progressUpdate = new HashMap<>();
        progressUpdate.put("fileId", fileId);
        progressUpdate.put("stage", stage);
        progressUpdate.put("percentage", percentage);
        progressUpdate.put("message", message);
        progressUpdate.put("timestamp", LocalDateTime.now().toString());
        
        if (data != null) {
            progressUpdate.put("data", data);
        }

        // Send to specific user
        messagingTemplate.convertAndSendToUser(userId, "/queue/progress", progressUpdate);
        
        // Also send to general topic for debugging (optional)
        messagingTemplate.convertAndSend("/topic/progress/" + fileId, progressUpdate);
    }

    public ProgressSession getProgressSession(String fileId) {
        return progressSessions.get(fileId);
    }

    public Map<String, Object> getProgressStatus(String fileId) {
        ProgressSession session = progressSessions.get(fileId);
        if (session != null) {
            return session.toMap();
        }
        return null;
    }

    private void cleanupSession(String fileId, long delayMs) {
        // Schedule cleanup
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                progressSessions.remove(fileId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Progress session class to track individual upload sessions
    public static class ProgressSession {
        private final String fileId;
        private final String userId;
        private final LocalDateTime startTime;
        private String currentStage;
        private int percentage;
        private String currentMessage;
        private Map<String, Object> currentData;
        private LocalDateTime lastUpdate;
        private boolean completed;
        private boolean hasError;
        private String errorMessage;

        public ProgressSession(String fileId, String userId) {
            this.fileId = fileId;
            this.userId = userId;
            this.startTime = LocalDateTime.now();
            this.currentStage = "INITIALIZED";
            this.percentage = 0;
            this.currentMessage = "Session initialized";
            this.lastUpdate = LocalDateTime.now();
            this.completed = false;
            this.hasError = false;
        }

        public void updateProgress(String stage, int percentage, String message, Map<String, Object> data) {
            this.currentStage = stage;
            this.percentage = percentage;
            this.currentMessage = message;
            this.currentData = data;
            this.lastUpdate = LocalDateTime.now();
        }

        public void complete(Map<String, Object> finalData) {
            this.completed = true;
            this.percentage = 100;
            this.currentStage = "COMPLETED";
            this.currentMessage = "Process completed successfully";
            this.currentData = finalData;
            this.lastUpdate = LocalDateTime.now();
        }

        public void error(String errorMessage, Map<String, Object> errorData) {
            this.hasError = true;
            this.errorMessage = errorMessage;
            this.currentStage = "ERROR";
            this.currentMessage = errorMessage;
            this.currentData = errorData;
            this.lastUpdate = LocalDateTime.now();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("fileId", fileId);
            map.put("userId", userId);
            map.put("startTime", startTime.toString());
            map.put("currentStage", currentStage);
            map.put("percentage", percentage);
            map.put("currentMessage", currentMessage);
            map.put("lastUpdate", lastUpdate.toString());
            map.put("completed", completed);
            map.put("hasError", hasError);
            
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            
            if (currentData != null) {
                map.put("data", currentData);
            }
            
            return map;
        }

        // Getters
        public String getFileId() { return fileId; }
        public String getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public String getCurrentStage() { return currentStage; }
        public int getPercentage() { return percentage; }
        public String getCurrentMessage() { return currentMessage; }
        public Map<String, Object> getCurrentData() { return currentData; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public boolean isCompleted() { return completed; }
        public boolean hasError() { return hasError; }
        public String getErrorMessage() { return errorMessage; }
    }

    // Predefined progress stages
    public static class ProgressStages {
        public static final String UPLOADING = "UPLOADING";
        public static final String EXTRACTING = "EXTRACTING";
        public static final String SECURITY_SCANNING = "SECURITY_SCANNING";
        public static final String AI_ANALYZING = "AI_ANALYZING";
        public static final String ORGANIZING = "ORGANIZING";
        public static final String CREATING_BRANCHES = "CREATING_BRANCHES";
        public static final String CREATING_REPO = "CREATING_REPO";
        public static final String PUSHING_TO_GITHUB = "PUSHING_TO_GITHUB";
        public static final String COMPLETED = "COMPLETED";
        public static final String ERROR = "ERROR";
    }

    // Helper methods for common progress updates
    public void updateUploadProgress(String fileId, String userId, int percentage, long uploadedBytes, long totalBytes) {
        Map<String, Object> data = new HashMap<>();
        data.put("uploadedBytes", uploadedBytes);
        data.put("totalBytes", totalBytes);
        data.put("speed", calculateSpeed(uploadedBytes, totalBytes));
        
        updateProgress(fileId, userId, ProgressStages.UPLOADING, percentage, 
                      "Uploading... " + formatBytes(uploadedBytes) + " / " + formatBytes(totalBytes), data);
    }

    public void updateExtractionProgress(String fileId, String userId, int percentage, String currentFile) {
        Map<String, Object> data = new HashMap<>();
        data.put("currentFile", currentFile);
        
        updateProgress(fileId, userId, ProgressStages.EXTRACTING, percentage, 
                      "Extracting files... " + currentFile, data);
    }

    public void updateAnalysisProgress(String fileId, String userId, int percentage, String analysisStep, Map<String, Object> analysisData) {
        Map<String, Object> data = new HashMap<>();
        data.put("analysisStep", analysisStep);
        if (analysisData != null) {
            data.putAll(analysisData);
        }
        
        updateProgress(fileId, userId, ProgressStages.AI_ANALYZING, percentage, 
                      "AI analyzing project... " + analysisStep, data);
    }

    public void updateGitHubProgress(String fileId, String userId, int percentage, String action, String repoUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("repoUrl", repoUrl);
        
        updateProgress(fileId, userId, ProgressStages.PUSHING_TO_GITHUB, percentage, 
                      "GitHub integration... " + action, data);
    }

    private String calculateSpeed(long uploadedBytes, long totalBytes) {
        // Simple speed calculation - in a real implementation, you'd track time
        return "Calculating...";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}
