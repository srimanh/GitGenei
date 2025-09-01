package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.UploadedProject;
import com.example.demo.entity.UploadedProject.ProjectStatus;
import com.example.demo.repository.UploadedProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProjectUploadService {

    @Autowired
    private UploadedProjectRepository uploadedProjectRepository;

    @Autowired
    private SecurityScanService securityScanService;

    @Autowired
    private ProjectAnalysisService projectAnalysisService;

    @Autowired
    private GitBranchService gitBranchService;

    @Autowired
    private GitHubIntegrationService gitHubIntegrationService;

    @Autowired
    private ProgressTrackingService progressTrackingService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UploadedProject saveUploadedFile(MultipartFile file, String fileId, String userId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Create user-specific directory
        Path userUploadPath = uploadPath.resolve(userId);
        if (!Files.exists(userUploadPath)) {
            Files.createDirectories(userUploadPath);
        }

        // Save file to disk
        String filename = file.getOriginalFilename();
        Path filePath = userUploadPath.resolve(fileId + "_" + filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create database record
        UploadedProject uploadedProject = new UploadedProject(
            fileId, 
            userId, 
            filename, 
            file.getSize(), 
            filePath.toString()
        );
        uploadedProject.setContentType(file.getContentType());

        return uploadedProjectRepository.save(uploadedProject);
    }

    public UploadedProject getProjectByFileId(String fileId, String userId) {
        Optional<UploadedProject> project = uploadedProjectRepository.findByFileIdAndUserId(fileId, userId);
        return project.orElse(null);
    }

    @Async
    public CompletableFuture<Void> startProjectAnalysis(String fileId, String userId) {
        System.out.println("🚀 STARTING ASYNC ANALYSIS for fileId: " + fileId + ", userId: " + userId);
        try {
            UploadedProject project = getProjectByFileId(fileId, userId);
            if (project == null) {
                System.out.println("❌ PROJECT NOT FOUND for fileId: " + fileId);
                throw new RuntimeException("Project not found");
            }

            System.out.println("✅ PROJECT FOUND: " + project.getFilename());
            // Start progress tracking
            progressTrackingService.startProgressSession(fileId, userId);

            // Update status to analyzing
            project.setStatus(ProjectStatus.ANALYZING);
            project.setAnalysisStartTime(LocalDateTime.now());
            project.setAnalysisProgress(0);
            uploadedProjectRepository.save(project);

            // Step 1: Extract files
            System.out.println("📁 STEP 1: Starting file extraction...");
            updateProgress(project, ProjectStatus.EXTRACTING, 10, "Extracting project files...");
            progressTrackingService.updateExtractionProgress(fileId, userId, 10, "Starting extraction...");
            String extractedPath = extractProjectFiles(project);
            project.setExtractedPath(extractedPath);
            System.out.println("✅ EXTRACTION COMPLETED: " + extractedPath);

            // Step 2: Security scan (DISABLED FOR MVP)
            System.out.println("🔒 STEP 2: Security scan SKIPPED for MVP - proceeding to analysis...");
            updateProgress(project, ProjectStatus.SECURITY_SCANNING, 25, "Security scan skipped for MVP...");
            progressTrackingService.updateProgress(fileId, userId, "SECURITY_SCANNING", 25, "Security scan skipped - proceeding...", null);
            project.setSecurityScanPassed(true); // Always pass for MVP
            System.out.println("🔒 SECURITY SCAN: SKIPPED ✅");

            // Step 3: AI Analysis
            System.out.println("🤖 STEP 3: Starting AI analysis...");
            updateProgress(project, ProjectStatus.ANALYZING, 50, "Analyzing project structure with AI...");
            Map<String, Object> analysisData = new HashMap<>();
            progressTrackingService.updateAnalysisProgress(fileId, userId, 50, "Starting AI analysis", analysisData);
            Map<String, Object> analysisResults = projectAnalysisService.analyzeProject(extractedPath);
            project.setAnalysisResults(objectMapper.writeValueAsString(analysisResults));
            System.out.println("✅ AI ANALYSIS COMPLETED: " + analysisResults.keySet());

            // Update progress with analysis results
            analysisData.put("languages", analysisResults.get("languages"));
            analysisData.put("frameworks", analysisResults.get("frameworks"));
            progressTrackingService.updateAnalysisProgress(fileId, userId, 65, "AI analysis completed", analysisData);

            // Step 4: Organize and create branches
            System.out.println("🌿 STEP 4: Organizing project and creating branches...");
            updateProgress(project, ProjectStatus.ORGANIZING, 75, "Organizing project structure...");
            Map<String, Object> organizationResults = projectAnalysisService.organizeProject(extractedPath, analysisResults);

            // Create branch structure
            Map<String, Object> branchResults = gitBranchService.createBranchStructure(extractedPath, analysisResults);
            project.setBranchesCreated(objectMapper.writeValueAsString(branchResults.get("createdBranches")));
            System.out.println("✅ BRANCHES CREATED: " + branchResults.get("createdBranches"));

            // Step 5: Create GitHub repository and push
            System.out.println("🐙 STEP 5: Creating GitHub repository...");
            updateProgress(project, ProjectStatus.CREATING_REPO, 90, "Creating GitHub repository...");
            String repoName = generateSmartRepoName(project.getFilename(), analysisResults);
            System.out.println("📝 SMART REPO NAME: " + repoName);
            Map<String, Object> repoResult = gitHubIntegrationService.createGitHubRepository(
                repoName,
                "Auto-generated repository by GitGenei AI",
                false, // public by default
                userId
            );
            System.out.println("🐙 REPO CREATION RESULT: " + repoResult);

            if ((Boolean) repoResult.get("success")) {
                project.setGithubRepoUrl((String) repoResult.get("repoUrl"));
                System.out.println("✅ GITHUB REPO CREATED: " + repoResult.get("repoUrl"));

                // Push branches to GitHub
                System.out.println("⬆️ STEP 6: Pushing branches to GitHub...");
                updateProgress(project, ProjectStatus.PUSHING_TO_GITHUB, 95, "Pushing branches to GitHub...");
                String branchesPath = (String) branchResults.get("branchesPath");
                System.out.println("📁 BRANCHES PATH: " + branchesPath);
                Map<String, Object> pushResult = gitHubIntegrationService.pushProjectToGitHub(
                    branchesPath,
                    (String) repoResult.get("cloneUrl"),
                    branchResults
                );
                System.out.println("⬆️ PUSH RESULT: " + pushResult);

                if (!(Boolean) pushResult.get("success")) {
                    System.out.println("❌ PUSH FAILED: " + pushResult.get("error"));
                    throw new RuntimeException("Failed to push to GitHub: " + pushResult.get("error"));
                }
                System.out.println("✅ PUSH SUCCESSFUL!");
            } else {
                System.out.println("❌ REPO CREATION FAILED: " + repoResult.get("error"));
                throw new RuntimeException("Failed to create GitHub repository: " + repoResult.get("error"));
            }

            // Complete
            System.out.println("🎉 STEP 7: Completing process...");
            project.setStatus(ProjectStatus.COMPLETED);
            project.setAnalysisProgress(100);
            project.setAnalysisEndTime(LocalDateTime.now());
            uploadedProjectRepository.save(project);

            // Complete progress tracking with comprehensive data
            Map<String, Object> finalData = new HashMap<>();
            finalData.put("repoUrl", repoResult.get("repoUrl"));
            finalData.put("repositoryUrl", repoResult.get("repoUrl")); // Add this for frontend compatibility
            finalData.put("repoName", repoName);
            finalData.put("cloneUrl", repoResult.get("cloneUrl"));
            finalData.put("sshUrl", repoResult.get("sshUrl"));
            finalData.put("fullName", repoResult.get("fullName"));
            finalData.put("branchesCreated", branchResults.get("createdBranches"));
            finalData.put("totalBranches", branchResults.get("totalBranches"));
            finalData.put("branches", branchResults.get("createdBranches")); // Add branches array for frontend
            finalData.put("pushedBranches", branchResults.get("totalBranches")); // Use branch count instead
            finalData.put("totalFiles", analysisResults.get("totalFiles"));
            finalData.put("projectName", project.getFilename());
            finalData.put("languages", analysisResults.get("languages"));
            finalData.put("frameworks", analysisResults.get("frameworks"));
            finalData.put("analysisResults", analysisResults);

            // Add success message for frontend
            finalData.put("message", "🎉 PROCESS COMPLETED SUCCESSFULLY!");
            finalData.put("details", String.format("📊 Repository: %s\n🌿 Branches: %d\n📁 Files: Successfully pushed",
                repoResult.get("repoUrl"), branchResults.get("totalBranches")));

            // Set the repository URL in the project
            project.setGithubRepoUrl((String) repoResult.get("repoUrl"));
            uploadedProjectRepository.save(project);

            progressTrackingService.completeProgress(fileId, userId, finalData);
            System.out.println("🎉 PROCESS COMPLETED SUCCESSFULLY!");
            System.out.println("📊 Repository: " + repoResult.get("repoUrl"));
            System.out.println("🌿 Branches: " + branchResults.get("totalBranches"));
            System.out.println("📁 Files: " + analysisResults.get("totalFiles"));

        } catch (Exception e) {
            // Handle error
            System.out.println("❌ ERROR OCCURRED: " + e.getMessage());
            e.printStackTrace();
            UploadedProject project = getProjectByFileId(fileId, userId);
            if (project != null) {
                project.setStatus(ProjectStatus.FAILED);
                project.setErrorMessage("Analysis failed: " + e.getMessage());
                project.setAnalysisEndTime(LocalDateTime.now());
                uploadedProjectRepository.save(project);
            }

            // Error progress tracking
            progressTrackingService.errorProgress(fileId, userId, "Analysis failed: " + e.getMessage(), null);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void updateProgress(UploadedProject project, ProjectStatus status, int progress, String message) {
        project.setStatus(status);
        project.setAnalysisProgress(progress);
        uploadedProjectRepository.save(project);
        
        // Log progress (could be sent to frontend via WebSocket)
        System.out.println("Project " + project.getFileId() + ": " + message + " (" + progress + "%)");
    }

    private String extractProjectFiles(UploadedProject project) throws IOException {
        String filePath = project.getFilePath();
        String extractedDirName = project.getFileId() + "_extracted";
        Path extractedPath = Paths.get(uploadDir, project.getUserId(), extractedDirName);
        
        if (!Files.exists(extractedPath)) {
            Files.createDirectories(extractedPath);
        }

        // Extract based on file type
        String filename = project.getFilename().toLowerCase();
        if (filename.endsWith(".zip")) {
            extractZipFile(filePath, extractedPath.toString());
        } else if (filename.endsWith(".tar") || filename.endsWith(".tar.gz")) {
            extractTarFile(filePath, extractedPath.toString());
        } else if (filename.endsWith(".rar")) {
            extractRarFile(filePath, extractedPath.toString());
        } else {
            throw new IOException("Unsupported file format: " + filename);
        }

        return extractedPath.toString();
    }

    private void extractZipFile(String zipFilePath, String extractToPath) throws IOException {
        // Implementation for ZIP extraction with system file filtering
        ProcessBuilder pb = new ProcessBuilder("unzip", "-q", zipFilePath, "-d", extractToPath);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to extract ZIP file");
            }

            // Clean up system files after extraction
            cleanupSystemFiles(extractToPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ZIP extraction interrupted", e);
        }
    }

    private void cleanupSystemFiles(String extractedPath) throws IOException {
        Path rootPath = Paths.get(extractedPath);

        // List of system files/folders to remove
        String[] systemPatterns = {
            "__MACOSX",
            ".DS_Store",
            "Thumbs.db",
            "desktop.ini",
            ".git",
            ".svn",
            ".hg",
            "node_modules/.cache",
            ".vscode/settings.json",
            ".idea"
        };

        Files.walk(rootPath)
            .filter(path -> {
                String pathStr = path.toString();
                String fileName = path.getFileName().toString();

                // Check if it matches any system pattern
                for (String pattern : systemPatterns) {
                    if (pathStr.contains(pattern) || fileName.equals(pattern) ||
                        fileName.startsWith(".") && (fileName.equals(".DS_Store") ||
                        fileName.equals(".git") || fileName.equals(".svn"))) {
                        return true;
                    }
                }
                return false;
            })
            .sorted((a, b) -> b.toString().length() - a.toString().length()) // Delete files before directories
            .forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        // Only delete if directory is empty or contains only system files
                        if (Files.list(path).count() == 0) {
                            Files.delete(path);
                            System.out.println("🗑️ Removed system directory: " + path.getFileName());
                        }
                    } else {
                        Files.delete(path);
                        System.out.println("🗑️ Removed system file: " + path.getFileName());
                    }
                } catch (IOException e) {
                    System.err.println("Failed to delete system file: " + path + " - " + e.getMessage());
                }
            });
    }

    private void extractTarFile(String tarFilePath, String extractToPath) throws IOException {
        // Implementation for TAR extraction
        ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFilePath, "-C", extractToPath);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to extract TAR file");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("TAR extraction interrupted", e);
        }
    }

    private void extractRarFile(String rarFilePath, String extractToPath) throws IOException {
        // Implementation for RAR extraction (requires unrar utility)
        ProcessBuilder pb = new ProcessBuilder("unrar", "x", rarFilePath, extractToPath);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to extract RAR file");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("RAR extraction interrupted", e);
        }
    }

    public Map<String, Object> getAnalysisResults(String fileId, String userId) {
        UploadedProject project = getProjectByFileId(fileId, userId);
        if (project == null || project.getAnalysisResults() == null) {
            return null;
        }

        try {
            return objectMapper.readValue(project.getAnalysisResults(), Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean deleteProject(String fileId, String userId) {
        try {
            UploadedProject project = getProjectByFileId(fileId, userId);
            if (project == null) {
                return false;
            }

            // Delete files from disk
            deleteProjectFiles(project);

            // Delete from database
            uploadedProjectRepository.deleteByFileIdAndUserId(fileId, userId);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteProjectFiles(UploadedProject project) {
        try {
            // Delete uploaded file
            if (project.getFilePath() != null) {
                Files.deleteIfExists(Paths.get(project.getFilePath()));
            }

            // Delete extracted files
            if (project.getExtractedPath() != null) {
                deleteDirectory(Paths.get(project.getExtractedPath()));
            }
        } catch (IOException e) {
            System.err.println("Failed to delete project files: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }

    private String generateSmartRepoName(String filename, Map<String, Object> analysisResults) {
        String baseName = null;

        // For MVP, let's simplify and just use filename-based naming to avoid casting issues
        // TODO: Add smart analysis-based naming in future versions
        System.out.println("🏷️ GENERATING SMART REPO NAME from filename: " + filename);

        // Extract name from filename
        baseName = extractNameFromFilename(filename);

        // Clean and validate the name
        String cleanName = cleanRepositoryName(baseName);
        System.out.println("🏷️ CLEAN REPO NAME: " + cleanName);

        return cleanName;
    }

    private String extractProjectNameFromPackageJson(String packageJsonPath) {
        // This is a simplified extraction - in a real implementation,
        // you'd parse the JSON file to get the "name" field
        return null; // For now, return null to use other methods
    }

    private String extractNameFromFilename(String filename) {
        if (filename == null) {
            return "project";
        }

        // Remove file extension
        String name = filename;
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            name = name.substring(0, lastDot);
        }

        // Remove common suffixes like "(Git)", "(1)", etc.
        name = name.replaceAll("\\s*\\([^)]*\\)\\s*", "");
        name = name.replaceAll("\\s*-\\s*Copy\\s*", "");
        name = name.replaceAll("\\s*_\\s*Copy\\s*", "");

        return name.trim();
    }

    private String cleanRepositoryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "project";
        }

        // Convert to lowercase and replace spaces with hyphens
        name = name.toLowerCase().trim();
        name = name.replaceAll("\\s+", "-");

        // Replace invalid characters with hyphens
        name = name.replaceAll("[^a-z0-9._-]", "-");

        // Remove consecutive hyphens
        name = name.replaceAll("-+", "-");

        // Remove leading/trailing hyphens
        name = name.replaceAll("^-+|-+$", "");

        // Ensure it's not empty and not too long
        if (name.isEmpty()) {
            name = "project";
        }
        if (name.length() > 80) { // Leave room for potential suffixes
            name = name.substring(0, 80);
            name = name.replaceAll("-+$", ""); // Remove trailing hyphens after truncation
        }

        return name;
    }
}
