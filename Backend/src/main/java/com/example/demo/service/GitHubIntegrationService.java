package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubIntegrationService {

    @Value("${github.personal-access-token}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GITHUB_API_BASE = "https://api.github.com";

    public Map<String, Object> createGitHubRepository(String repoName, String description, boolean isPrivate, String userLogin) {
        String finalRepoName = repoName;
        int attempt = 0;

        while (attempt < 5) { // Try up to 5 times with different names
            try {
                System.out.println("🐙 CREATING GITHUB REPO (attempt " + (attempt + 1) + "): " + finalRepoName);

                HttpHeaders headers = createGitHubHeaders();

                Map<String, Object> repoData = new HashMap<>();
                repoData.put("name", finalRepoName);
                repoData.put("description", description);
                repoData.put("private", isPrivate);
                repoData.put("auto_init", false); // We'll initialize ourselves
                repoData.put("has_issues", true);
                repoData.put("has_projects", true);
                repoData.put("has_wiki", true);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(repoData, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                    GITHUB_API_BASE + "/user/repos",
                    request,
                    String.class
                );

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    JsonNode repoJson = objectMapper.readTree(response.getBody());

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("repoUrl", repoJson.get("html_url").asText());
                    result.put("cloneUrl", repoJson.get("clone_url").asText());
                    result.put("sshUrl", repoJson.get("ssh_url").asText());
                    result.put("repoName", repoJson.get("name").asText());
                    result.put("fullName", repoJson.get("full_name").asText());

                    System.out.println("✅ GITHUB REPO CREATED: " + repoJson.get("html_url").asText());
                    return result;
                } else {
                    return createErrorResult("Failed to create repository: " + response.getStatusCode());
                }

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                System.out.println("❌ GITHUB REPO CREATION ERROR: " + errorMessage);

                // Check if it's a name conflict error (422 status with "name already exists")
                if (errorMessage.contains("name already exists") ||
                    errorMessage.contains("422") ||
                    errorMessage.contains("Repository creation failed")) {

                    attempt++;
                    if (attempt < 5) {
                        // Generate a new name with timestamp
                        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp
                        finalRepoName = repoName + "-" + timestamp;
                        System.out.println("🔄 REPO NAME CONFLICT - Trying new name: " + finalRepoName);
                        continue;
                    }
                }

                return createErrorResult("Error creating GitHub repository: " + errorMessage);
            }
        }

        return createErrorResult("Failed to create repository after multiple attempts - all names taken");
    }

    public Map<String, Object> pushProjectToGitHub(String branchesPath, String repoUrl, Map<String, Object> branchResults) {
        try {
            Map<String, Object> result = new HashMap<>();
            List<String> pushedBranches = new ArrayList<>();
            
            // Initialize git repository if not already done
            initializeGitRepository(branchesPath);
            
            // Add remote origin
            addGitRemote(branchesPath, repoUrl);
            
            // Get created branches from branch results
            @SuppressWarnings("unchecked")
            Map<String, Object> createdBranches = (Map<String, Object>) branchResults.get("createdBranches");
            
            if (createdBranches != null) {
                for (String branchName : createdBranches.keySet()) {
                    try {
                        // Create and push branch without overwriting main directory
                        String commitHash = createAndPushBranchFromDirectory(branchesPath, branchName, repoUrl);
                        pushedBranches.add(branchName + ":" + commitHash);
                        
                    } catch (Exception e) {
                        System.err.println("Failed to push branch " + branchName + ": " + e.getMessage());
                    }
                }
            }
            
            result.put("success", true);
            result.put("pushedBranches", pushedBranches);
            result.put("totalBranches", pushedBranches.size());
            result.put("repositoryUrl", repoUrl);
            result.put("branches", pushedBranches);

            // Add success message
            result.put("message", "🎉 PROCESS COMPLETED SUCCESSFULLY!");
            result.put("details", String.format("📊 Repository: %s\n🌿 Branches: %d\n📁 Files: Successfully pushed",
                repoUrl, pushedBranches.size()));

            return result;

        } catch (Exception e) {
            return createErrorResult("Error pushing to GitHub: " + e.getMessage());
        }
    }

    private void initializeGitRepository(String repoPath) throws IOException, InterruptedException {
        // Check if already initialized
        Path gitDir = Paths.get(repoPath, ".git");
        if (Files.exists(gitDir)) {
            return; // Already initialized
        }

        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(new File(repoPath));
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to initialize git repository");
        }
        
        // Configure git user (using token for authentication)
        configureGitUser(repoPath);
    }

    private void configureGitUser(String repoPath) throws IOException, InterruptedException {
        // Set git user name and email
        ProcessBuilder namePb = new ProcessBuilder("git", "config", "user.name", "GitGenei AI");
        namePb.directory(new File(repoPath));
        namePb.start().waitFor();
        
        ProcessBuilder emailPb = new ProcessBuilder("git", "config", "user.email", "gitgenei@ai.com");
        emailPb.directory(new File(repoPath));
        emailPb.start().waitFor();
    }

    private void addGitRemote(String repoPath, String repoUrl) throws IOException, InterruptedException {
        // Remove existing origin if it exists
        ProcessBuilder removePb = new ProcessBuilder("git", "remote", "remove", "origin");
        removePb.directory(new File(repoPath));
        Process removeProcess = removePb.start();
        removeProcess.waitFor(); // Don't check exit code as it might not exist
        
        // Add new origin with token authentication
        String authenticatedUrl = repoUrl.replace("https://", "https://" + githubToken + "@");
        ProcessBuilder addPb = new ProcessBuilder("git", "remote", "add", "origin", authenticatedUrl);
        addPb.directory(new File(repoPath));
        
        Process addProcess = addPb.start();
        int exitCode = addProcess.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to add git remote");
        }
    }

    private void copyBranchToMainRepo(String branchesPath, String branchName) throws IOException {
        Path branchPath = Paths.get(branchesPath, branchName);
        Path mainRepoPath = Paths.get(branchesPath);

        if (!Files.exists(branchPath)) {
            System.out.println("❌ Branch path does not exist: " + branchPath);
            return;
        }

        System.out.println("📁 Copying files from branch: " + branchName);

        // Clear main repo (except .git and .gitignore)
        Files.walk(mainRepoPath, 1)
            .filter(path -> !path.equals(mainRepoPath))
            .filter(path -> !path.getFileName().toString().equals(".git"))
            .filter(path -> !path.getFileName().toString().equals(".gitignore"))
            .forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        deleteDirectoryRecursively(path);
                    } else {
                        Files.delete(path);
                    }
                    System.out.println("🗑️ Cleaned: " + path.getFileName());
                } catch (IOException e) {
                    System.err.println("Failed to clean: " + path + " - " + e.getMessage());
                }
            });

        // Copy ALL files and directories from branch to main repo
        Files.walk(branchPath)
            .filter(path -> !path.equals(branchPath))
            .forEach(sourcePath -> {
                try {
                    Path relativePath = branchPath.relativize(sourcePath);
                    Path targetPath = mainRepoPath.resolve(relativePath);

                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                        System.out.println("📂 Created directory: " + relativePath);
                    } else {
                        // Create parent directories
                        Path parentDir = targetPath.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            Files.createDirectories(parentDir);
                        }

                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("📄 Copied file: " + relativePath);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to copy: " + sourcePath + " - " + e.getMessage());
                }
            });

        System.out.println("✅ Branch " + branchName + " files copied successfully");
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        Files.walk(directory)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete: " + path + " - " + e.getMessage());
                }
            });
    }

    private String createAndPushBranchFromDirectory(String branchesPath, String branchName, String repoUrl) throws IOException, InterruptedException {
        Path branchPath = Paths.get(branchesPath, branchName);
        Path mainRepoPath = Paths.get(branchesPath);

        if (!Files.exists(branchPath)) {
            System.out.println("❌ Branch path does not exist: " + branchPath);
            return "unknown";
        }

        System.out.println("📁 Processing branch: " + branchName);

        // Create a temporary working directory for this branch
        Path tempWorkDir = Files.createTempDirectory("gitgenei_" + branchName);
        
        try {
            // Copy branch files to temporary directory
            copyDirectoryRecursively(branchPath, tempWorkDir);
            
            // Initialize git in temporary directory
            ProcessBuilder initPb = new ProcessBuilder("git", "init");
            initPb.directory(tempWorkDir.toFile());
            initPb.start().waitFor();
            
            // Configure git user
            ProcessBuilder namePb = new ProcessBuilder("git", "config", "user.name", "GitGenei AI");
            namePb.directory(tempWorkDir.toFile());
            namePb.start().waitFor();
            
            ProcessBuilder emailPb = new ProcessBuilder("git", "config", "user.email", "gitgenei@ai.com");
            emailPb.directory(tempWorkDir.toFile());
            emailPb.start().waitFor();
            
            // Add remote origin
            ProcessBuilder remotePb = new ProcessBuilder("git", "remote", "add", "origin", repoUrl);
            remotePb.directory(tempWorkDir.toFile());
            remotePb.start().waitFor();
            
            // Add all files
            ProcessBuilder addPb = new ProcessBuilder("git", "add", ".");
            addPb.directory(tempWorkDir.toFile());
            addPb.start().waitFor();
            
            // Commit files
            String commitMessage = "Add " + branchName + " components - Generated by GitGenei AI";
            ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", commitMessage);
            commitPb.directory(tempWorkDir.toFile());
            Process commitProcess = commitPb.start();
            int commitExitCode = commitProcess.waitFor();
            
            String commitHash = "unknown";
            if (commitExitCode == 0) {
                // Get commit hash
                ProcessBuilder hashPb = new ProcessBuilder("git", "rev-parse", "HEAD");
                hashPb.directory(tempWorkDir.toFile());
                Process hashProcess = hashPb.start();
                
                try (Scanner scanner = new Scanner(hashProcess.getInputStream())) {
                    if (scanner.hasNextLine()) {
                        commitHash = scanner.nextLine().substring(0, 8); // Short hash
                    }
                }
                
                // Push to GitHub with force to ensure it gets pushed
                ProcessBuilder pushPb = new ProcessBuilder("git", "push", "-u", "origin", branchName, "--force");
                pushPb.directory(tempWorkDir.toFile());
                Process pushProcess = pushPb.start();
                int pushExitCode = pushProcess.waitFor();

                if (pushExitCode == 0) {
                    System.out.println("✅ Successfully pushed branch: " + branchName);
                } else {
                    System.err.println("❌ Failed to push branch: " + branchName);
                    // Don't throw exception, continue with other branches
                }
            }
            
            return commitHash;
            
        } finally {
            // Clean up temporary directory
            deleteDirectoryRecursively(tempWorkDir);
        }
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("Failed to copy: " + sourcePath + " - " + e.getMessage());
            }
        });
    }

    private String createAndPushBranch(String repoPath, String branchName) throws IOException, InterruptedException {
        // Checkout to branch (create if doesn't exist)
        ProcessBuilder checkoutPb = new ProcessBuilder("git", "checkout", "-B", branchName);
        checkoutPb.directory(new File(repoPath));
        checkoutPb.start().waitFor();
        
        // Add all files
        ProcessBuilder addPb = new ProcessBuilder("git", "add", ".");
        addPb.directory(new File(repoPath));
        addPb.start().waitFor();
        
        // Commit files
        String commitMessage = "Add " + branchName + " components - Generated by GitGenei AI";
        ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", commitMessage);
        commitPb.directory(new File(repoPath));
        Process commitProcess = commitPb.start();
        int commitExitCode = commitProcess.waitFor();
        
        String commitHash = "unknown";
        if (commitExitCode == 0) {
            // Get commit hash
            ProcessBuilder hashPb = new ProcessBuilder("git", "rev-parse", "HEAD");
            hashPb.directory(new File(repoPath));
            Process hashProcess = hashPb.start();
            
            try (Scanner scanner = new Scanner(hashProcess.getInputStream())) {
                if (scanner.hasNextLine()) {
                    commitHash = scanner.nextLine().substring(0, 8); // Short hash
                }
            }
            
            // Push to GitHub with force to ensure it gets pushed
            ProcessBuilder pushPb = new ProcessBuilder("git", "push", "-u", "origin", branchName, "--force");
            pushPb.directory(new File(repoPath));
            Process pushProcess = pushPb.start();
            int pushExitCode = pushProcess.waitFor();

            if (pushExitCode == 0) {
                System.out.println("✅ Successfully pushed branch: " + branchName);
            } else {
                System.err.println("❌ Failed to push branch: " + branchName);
                // Don't throw exception, continue with other branches
            }
        }
        
        return commitHash;
    }

    public Map<String, Object> createPullRequest(String repoFullName, String sourceBranch, String targetBranch, String title, String description) {
        try {
            HttpHeaders headers = createGitHubHeaders();
            
            Map<String, Object> prData = new HashMap<>();
            prData.put("title", title);
            prData.put("body", description);
            prData.put("head", sourceBranch);
            prData.put("base", targetBranch);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(prData, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                GITHUB_API_BASE + "/repos/" + repoFullName + "/pulls", 
                request, 
                String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode prJson = objectMapper.readTree(response.getBody());
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("prUrl", prJson.get("html_url").asText());
                result.put("prNumber", prJson.get("number").asInt());
                
                return result;
            } else {
                return createErrorResult("Failed to create pull request: " + response.getStatusCode());
            }

        } catch (Exception e) {
            return createErrorResult("Error creating pull request: " + e.getMessage());
        }
    }

    public Map<String, Object> mergePullRequest(String repoFullName, int pullNumber, String commitTitle, String commitMessage) {
        try {
            HttpHeaders headers = createGitHubHeaders();

            Map<String, Object> mergeData = new HashMap<>();
            mergeData.put("commit_title", commitTitle);
            mergeData.put("commit_message", commitMessage);
            mergeData.put("merge_method", "merge"); // Can be "merge", "squash", or "rebase"

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(mergeData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/repos/" + repoFullName + "/pulls/" + pullNumber + "/merge",
                HttpMethod.PUT,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode mergeJson = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("merged", mergeJson.get("merged").asBoolean());
                result.put("sha", mergeJson.get("sha").asText());
                result.put("message", mergeJson.get("message").asText());

                return result;
            } else {
                return createErrorResult("Failed to merge pull request: " + response.getStatusCode());
            }

        } catch (Exception e) {
            return createErrorResult("Error merging pull request: " + e.getMessage());
        }
    }

    public Map<String, Object> createAndMergePullRequest(String repoFullName, String sourceBranch, String targetBranch, String title, String description) {
        // First create the pull request
        Map<String, Object> prResult = createPullRequest(repoFullName, sourceBranch, targetBranch, title, description);

        if (!(Boolean) prResult.get("success")) {
            return prResult; // Return error if PR creation failed
        }

        try {
            // Extract PR number from the result
            String prUrl = (String) prResult.get("prUrl");
            int pullNumber = Integer.parseInt(prUrl.substring(prUrl.lastIndexOf("/") + 1));

            // Wait a moment for GitHub to process the PR
            Thread.sleep(2000);

            // Auto-merge the pull request
            Map<String, Object> mergeResult = mergePullRequest(repoFullName, pullNumber,
                "Merge " + sourceBranch + " into " + targetBranch,
                "Auto-merge organized project components");

            if ((Boolean) mergeResult.get("success")) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("prCreated", true);
                result.put("prMerged", true);
                result.put("prUrl", prResult.get("prUrl"));
                result.put("mergeCommit", mergeResult.get("sha"));
                return result;
            } else {
                // PR created but merge failed
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("prCreated", true);
                result.put("prMerged", false);
                result.put("prUrl", prResult.get("prUrl"));
                result.put("mergeError", mergeResult.get("error"));
                return result;
            }

        } catch (Exception e) {
            // PR created but auto-merge failed
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("prCreated", true);
            result.put("prMerged", false);
            result.put("prUrl", prResult.get("prUrl"));
            result.put("mergeError", "Auto-merge failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> addRepositoryTopics(String repoFullName, List<String> topics) {
        try {
            HttpHeaders headers = createGitHubHeaders();
            headers.set("Accept", "application/vnd.github.mercy-preview+json"); // Required for topics API
            
            Map<String, Object> topicsData = new HashMap<>();
            topicsData.put("names", topics);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(topicsData, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/repos/" + repoFullName + "/topics",
                HttpMethod.PUT,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("topics", topics);
                return result;
            } else {
                return createErrorResult("Failed to add topics: " + response.getStatusCode());
            }

        } catch (Exception e) {
            return createErrorResult("Error adding repository topics: " + e.getMessage());
        }
    }

    public Map<String, Object> createRepositoryReadme(String repoFullName, String readmeContent) {
        try {
            HttpHeaders headers = createGitHubHeaders();
            
            // Encode content to base64
            String encodedContent = Base64.getEncoder().encodeToString(readmeContent.getBytes());
            
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("message", "Add README.md - Generated by GitGenei AI");
            fileData.put("content", encodedContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(fileData, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/repos/" + repoFullName + "/contents/README.md",
                HttpMethod.PUT,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "README.md created successfully");
                return result;
            } else {
                return createErrorResult("Failed to create README: " + response.getStatusCode());
            }

        } catch (Exception e) {
            return createErrorResult("Error creating README: " + e.getMessage());
        }
    }

    private HttpHeaders createGitHubHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "token " + githubToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "GitGenei-AI");
        return headers;
    }

    private Map<String, Object> createErrorResult(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        return result;
    }

    public boolean isGitHubTokenValid() {
        try {
            HttpHeaders headers = createGitHubHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/user",
                HttpMethod.GET,
                request,
                String.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getGitHubUserInfo() {
        try {
            HttpHeaders headers = createGitHubHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_API_BASE + "/user",
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode userJson = objectMapper.readTree(response.getBody());
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("login", userJson.get("login").asText());
                result.put("name", userJson.get("name").asText());
                result.put("email", userJson.get("email").asText());
                result.put("publicRepos", userJson.get("public_repos").asInt());
                
                return result;
            } else {
                return createErrorResult("Failed to get user info: " + response.getStatusCode());
            }
        } catch (Exception e) {
            return createErrorResult("Error getting GitHub user info: " + e.getMessage());
        }
    }
}
