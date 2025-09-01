package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.UploadedProject;
import com.example.demo.entity.UploadedProject.ProjectStatus;
import com.example.demo.service.ProjectUploadService;
import com.example.demo.service.SecurityScanService;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:3001", allowCredentials = "true")
public class UploadController {

    @Autowired
    private ProjectUploadService projectUploadService;

    @Autowired
    private SecurityScanService securityScanService;

    @PostMapping("/project")
    public ResponseEntity<?> uploadProject(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            // Validate user authentication
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Check file size (5GB limit)
            long maxSize = 5L * 1024 * 1024 * 1024; // 5GB in bytes
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5GB limit"));
            }

            // Validate file type
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            if (!isValidProjectFile(filename, contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Please upload ZIP, TAR, or RAR files"));
            }

            // Generate unique file ID
            String fileId = UUID.randomUUID().toString();
            String userId = principal.getAttribute("id").toString();

            // Perform initial security scan
            boolean isSafe = securityScanService.performQuickScan(file);
            if (!isSafe) {
                return ResponseEntity.badRequest().body(Map.of("error", "File failed security scan"));
            }

            // Save uploaded file
            UploadedProject uploadedProject = projectUploadService.saveUploadedFile(file, fileId, userId);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", fileId);
            response.put("filename", filename);
            response.put("size", file.getSize());
            response.put("uploadTime", uploadedProject.getUploadTime());
            response.put("message", "File uploaded successfully. Analysis will begin shortly.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/status/{fileId}")
    public ResponseEntity<?> getUploadStatus(
            @PathVariable String fileId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            UploadedProject project = projectUploadService.getProjectByFileId(fileId, userId);
            
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("status", project.getStatus().toString());
            response.put("filename", project.getFilename());
            response.put("size", project.getFileSize());
            response.put("uploadTime", project.getUploadTime());
            response.put("analysisProgress", project.getAnalysisProgress());
            response.put("analysisStartTime", project.getAnalysisStartTime());
            response.put("analysisEndTime", project.getAnalysisEndTime());
            response.put("githubRepoUrl", project.getGithubRepoUrl());

            // Add status message based on current status
            String statusMessage = getStatusMessage(project.getStatus());
            response.put("statusMessage", statusMessage);

            if (project.getErrorMessage() != null) {
                response.put("errorMessage", project.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze/{fileId}")
    public ResponseEntity<?> startAnalysis(
            @PathVariable String fileId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            
            // Start asynchronous analysis
            projectUploadService.startProjectAnalysis(fileId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analysis started");
            response.put("fileId", fileId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start analysis: " + e.getMessage()));
        }
    }



    @GetMapping("/analysis/{fileId}")
    public ResponseEntity<?> getAnalysisResults(
            @PathVariable String fileId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            Map<String, Object> analysisResults = projectUploadService.getAnalysisResults(fileId, userId);
            
            if (analysisResults == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(analysisResults);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get analysis results: " + e.getMessage()));
        }
    }

    @DeleteMapping("/project/{fileId}")
    public ResponseEntity<?> deleteProject(
            @PathVariable String fileId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userId = principal.getAttribute("id").toString();
            boolean deleted = projectUploadService.deleteProject(fileId, userId);
            
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Project deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete project: " + e.getMessage()));
        }
    }

    private boolean isValidProjectFile(String filename, String contentType) {
        if (filename == null) return false;
        
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".zip") ||
               lowerFilename.endsWith(".tar") ||
               lowerFilename.endsWith(".tar.gz") ||
               lowerFilename.endsWith(".rar") ||
               lowerFilename.endsWith(".7z") ||
               (contentType != null && (
                   contentType.equals("application/zip") ||
                   contentType.equals("application/x-tar") ||
                   contentType.equals("application/gzip") ||
                   contentType.equals("application/x-rar-compressed") ||
                   contentType.equals("application/x-7z-compressed")
               ));
    }

    private String getStatusMessage(ProjectStatus status) {
        switch (status) {
            case UPLOADED: return "File uploaded successfully";
            case EXTRACTING: return "Extracting project files...";
            case SECURITY_SCANNING: return "Performing security scan...";
            case ANALYZING: return "AI is analyzing your project...";
            case ORGANIZING: return "Organizing project structure...";
            case CREATING_REPO: return "Creating GitHub repository...";
            case PUSHING_TO_GITHUB: return "Pushing to GitHub...";
            case COMPLETED: return "Analysis completed successfully!";
            case FAILED: return "Analysis failed";
            default: return "Processing...";
        }
    }
}
