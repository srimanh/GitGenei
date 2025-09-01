package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_projects")
public class UploadedProject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_id", unique = true, nullable = false)
    private String fileId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "content_type")
    private String contentType;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;
    
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
    
    @Column(name = "analysis_start_time")
    private LocalDateTime analysisStartTime;
    
    @Column(name = "analysis_end_time")
    private LocalDateTime analysisEndTime;
    
    @Column(name = "analysis_progress")
    private Integer analysisProgress;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "analysis_results", columnDefinition = "TEXT")
    private String analysisResults; // JSON string
    
    @Column(name = "github_repo_url")
    private String githubRepoUrl;
    
    @Column(name = "branches_created", columnDefinition = "TEXT")
    private String branchesCreated; // JSON array as string
    
    @Column(name = "security_scan_passed")
    private Boolean securityScanPassed;
    
    @Column(name = "extracted_path")
    private String extractedPath;

    public enum ProjectStatus {
        UPLOADED,
        EXTRACTING,
        SECURITY_SCANNING,
        ANALYZING,
        ORGANIZING,
        CREATING_REPO,
        PUSHING_TO_GITHUB,
        COMPLETED,
        FAILED
    }

    // Default constructor
    public UploadedProject() {
        this.uploadTime = LocalDateTime.now();
        this.status = ProjectStatus.UPLOADED;
        this.analysisProgress = 0;
        this.securityScanPassed = false;
    }

    // Constructor with required fields
    public UploadedProject(String fileId, String userId, String filename, Long fileSize, String filePath) {
        this();
        this.fileId = fileId;
        this.userId = userId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public LocalDateTime getAnalysisStartTime() {
        return analysisStartTime;
    }

    public void setAnalysisStartTime(LocalDateTime analysisStartTime) {
        this.analysisStartTime = analysisStartTime;
    }

    public LocalDateTime getAnalysisEndTime() {
        return analysisEndTime;
    }

    public void setAnalysisEndTime(LocalDateTime analysisEndTime) {
        this.analysisEndTime = analysisEndTime;
    }

    public Integer getAnalysisProgress() {
        return analysisProgress;
    }

    public void setAnalysisProgress(Integer analysisProgress) {
        this.analysisProgress = analysisProgress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAnalysisResults() {
        return analysisResults;
    }

    public void setAnalysisResults(String analysisResults) {
        this.analysisResults = analysisResults;
    }

    public String getGithubRepoUrl() {
        return githubRepoUrl;
    }

    public void setGithubRepoUrl(String githubRepoUrl) {
        this.githubRepoUrl = githubRepoUrl;
    }

    public String getBranchesCreated() {
        return branchesCreated;
    }

    public void setBranchesCreated(String branchesCreated) {
        this.branchesCreated = branchesCreated;
    }

    public Boolean getSecurityScanPassed() {
        return securityScanPassed;
    }

    public void setSecurityScanPassed(Boolean securityScanPassed) {
        this.securityScanPassed = securityScanPassed;
    }

    public String getExtractedPath() {
        return extractedPath;
    }

    public void setExtractedPath(String extractedPath) {
        this.extractedPath = extractedPath;
    }
}
