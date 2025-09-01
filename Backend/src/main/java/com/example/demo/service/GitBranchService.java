package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class GitBranchService {

    public Map<String, Object> createBranchStructure(String extractedPath, Map<String, Object> analysisResults) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        // Get suggested branches from analysis
        @SuppressWarnings("unchecked")
        List<String> suggestedBranches = (List<String>) analysisResults.get("suggestedBranches");
        if (suggestedBranches == null) {
            suggestedBranches = Arrays.asList("main");
        }
        
        // Get organization plan
        @SuppressWarnings("unchecked")
        Map<String, Object> organizationPlan = (Map<String, Object>) analysisResults.get("organizationPlan");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> branchFiles = organizationPlan != null ? 
            (Map<String, List<String>>) organizationPlan.get("branchFiles") : new HashMap<>();
        
        // Create branch directories
        String branchesPath = extractedPath + "_branches";
        Path branchesDir = Paths.get(branchesPath);
        if (!Files.exists(branchesDir)) {
            Files.createDirectories(branchesDir);
        }
        
        Map<String, BranchInfo> createdBranches = new HashMap<>();
        
        for (String branchName : suggestedBranches) {
            BranchInfo branchInfo = createBranch(branchesDir, branchName, extractedPath, branchFiles.get(branchName));
            createdBranches.put(branchName, branchInfo);
        }
        
        // Create git repository structure
        initializeGitRepository(branchesDir.toString());
        
        result.put("branchesPath", branchesPath);
        result.put("createdBranches", createdBranches);
        result.put("totalBranches", createdBranches.size());
        result.put("status", "success");
        
        return result;
    }

    private BranchInfo createBranch(Path branchesDir, String branchName, String sourcePath, List<String> branchFiles) throws IOException {
        Path branchPath = branchesDir.resolve(branchName);
        if (!Files.exists(branchPath)) {
            Files.createDirectories(branchPath);
        }
        
        BranchInfo branchInfo = new BranchInfo(branchName, branchPath.toString());
        
        if (branchFiles != null && !branchFiles.isEmpty()) {
            // Copy specific files to this branch
            for (String filePath : branchFiles) {
                copyFileToBranch(sourcePath, branchPath.toString(), filePath, branchInfo);
            }
        } else {
            // For main branch or if no specific files, copy everything
            if ("main".equals(branchName)) {
                copyAllFilesToBranch(sourcePath, branchPath.toString(), branchInfo);
            }
        }
        
        // Create branch-specific files
        createBranchSpecificFiles(branchPath, branchName, branchInfo);
        
        return branchInfo;
    }

    private void copyFileToBranch(String sourcePath, String branchPath, String filePath, BranchInfo branchInfo) throws IOException {
        Path sourceFile = Paths.get(sourcePath, filePath);
        Path targetFile = Paths.get(branchPath, filePath);
        
        if (Files.exists(sourceFile)) {
            // Create parent directories if they don't exist
            Path parentDir = targetFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            branchInfo.addFile(filePath);
        }
    }

    private void copyAllFilesToBranch(String sourcePath, String branchPath, BranchInfo branchInfo) throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        Path targetDir = Paths.get(branchPath);
        
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                 .forEach(sourcePath1 -> {
                     try {
                         Path relativePath = sourceDir.relativize(sourcePath1);
                         Path targetPath = targetDir.resolve(relativePath);
                         
                         // Create parent directories
                         Path parentDir = targetPath.getParent();
                         if (parentDir != null && !Files.exists(parentDir)) {
                             Files.createDirectories(parentDir);
                         }
                         
                         Files.copy(sourcePath1, targetPath, StandardCopyOption.REPLACE_EXISTING);
                         branchInfo.addFile(relativePath.toString());
                     } catch (IOException e) {
                         System.err.println("Failed to copy file: " + sourcePath1 + " - " + e.getMessage());
                     }
                 });
        }
    }

    private void createBranchSpecificFiles(Path branchPath, String branchName, BranchInfo branchInfo) throws IOException {
        // Create branch-specific README
        String readmeContent = generateBranchReadme(branchName, branchInfo);
        Path readmePath = branchPath.resolve("README.md");
        Files.writeString(readmePath, readmeContent);
        branchInfo.addFile("README.md");
        
        // Create .gitignore if it doesn't exist
        Path gitignorePath = branchPath.resolve(".gitignore");
        if (!Files.exists(gitignorePath)) {
            String gitignoreContent = generateGitignore(branchName);
            Files.writeString(gitignorePath, gitignoreContent);
            branchInfo.addFile(".gitignore");
        }
        
        // Create branch-specific configuration files
        createBranchConfig(branchPath, branchName, branchInfo);
    }

    private String generateBranchReadme(String branchName, BranchInfo branchInfo) {
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(branchName.substring(0, 1).toUpperCase()).append(branchName.substring(1)).append(" Branch\n\n");
        readme.append("This branch contains the ").append(branchName).append(" components of the project.\n\n");
        
        if ("frontend".equals(branchName)) {
            readme.append("## Frontend Components\n\n");
            readme.append("This branch contains all frontend-related code including:\n");
            readme.append("- User interface components\n");
            readme.append("- Styling and assets\n");
            readme.append("- Client-side logic\n");
            readme.append("- Build configurations\n\n");
            readme.append("## Getting Started\n\n");
            readme.append("1. Install dependencies: `npm install`\n");
            readme.append("2. Start development server: `npm start`\n");
            readme.append("3. Build for production: `npm run build`\n\n");
        } else if ("backend".equals(branchName)) {
            readme.append("## Backend Components\n\n");
            readme.append("This branch contains all backend-related code including:\n");
            readme.append("- API endpoints\n");
            readme.append("- Database models\n");
            readme.append("- Business logic\n");
            readme.append("- Server configurations\n\n");
            readme.append("## Getting Started\n\n");
            readme.append("1. Install dependencies\n");
            readme.append("2. Configure database connection\n");
            readme.append("3. Start the server\n\n");
        } else if ("docs".equals(branchName)) {
            readme.append("## Documentation\n\n");
            readme.append("This branch contains project documentation including:\n");
            readme.append("- API documentation\n");
            readme.append("- User guides\n");
            readme.append("- Development guides\n");
            readme.append("- Architecture diagrams\n\n");
        } else if ("main".equals(branchName)) {
            readme.append("## Main Project\n\n");
            readme.append("This is the main branch containing the complete project.\n\n");
            readme.append("## Project Structure\n\n");
            readme.append("The project has been organized into the following branches:\n");
            readme.append("- `main`: Complete project\n");
            readme.append("- `frontend`: Frontend components\n");
            readme.append("- `backend`: Backend components\n");
            readme.append("- `docs`: Documentation\n\n");
        }
        
        readme.append("## Files in this branch\n\n");
        readme.append("Total files: ").append(branchInfo.getFileCount()).append("\n\n");
        
        readme.append("---\n");
        readme.append("*This branch contains organized project components*\n");
        
        return readme.toString();
    }

    private String generateGitignore(String branchName) {
        StringBuilder gitignore = new StringBuilder();
        
        // Common ignores
        gitignore.append("# Dependencies\n");
        gitignore.append("node_modules/\n");
        gitignore.append("vendor/\n\n");
        
        gitignore.append("# Build outputs\n");
        gitignore.append("dist/\n");
        gitignore.append("build/\n");
        gitignore.append("target/\n");
        gitignore.append("out/\n\n");
        
        gitignore.append("# Environment files\n");
        gitignore.append(".env\n");
        gitignore.append(".env.local\n");
        gitignore.append(".env.production\n\n");
        
        gitignore.append("# IDE files\n");
        gitignore.append(".vscode/\n");
        gitignore.append(".idea/\n");
        gitignore.append("*.swp\n");
        gitignore.append("*.swo\n\n");
        
        gitignore.append("# OS files\n");
        gitignore.append(".DS_Store\n");
        gitignore.append("Thumbs.db\n\n");
        
        // Branch-specific ignores
        if ("frontend".equals(branchName)) {
            gitignore.append("# Frontend specific\n");
            gitignore.append("npm-debug.log*\n");
            gitignore.append("yarn-debug.log*\n");
            gitignore.append("yarn-error.log*\n");
            gitignore.append(".next/\n");
            gitignore.append(".nuxt/\n\n");
        } else if ("backend".equals(branchName)) {
            gitignore.append("# Backend specific\n");
            gitignore.append("*.log\n");
            gitignore.append("logs/\n");
            gitignore.append("*.pid\n");
            gitignore.append("*.seed\n\n");
        }
        
        return gitignore.toString();
    }

    private void createBranchConfig(Path branchPath, String branchName, BranchInfo branchInfo) throws IOException {
        // Create a simple branch configuration file
        Map<String, Object> config = new HashMap<>();
        config.put("branchName", branchName);
        config.put("createdAt", new Date().toString());
        config.put("createdBy", "Auto-organized");
        config.put("description", "Organized branch for " + branchName + " components");

        // You could write this as JSON if needed
        StringBuilder configContent = new StringBuilder();
        configContent.append("# Branch Configuration\n");
        configContent.append("# Auto-organized project structure\n\n");
        configContent.append("BRANCH_NAME=").append(branchName).append("\n");
        configContent.append("CREATED_AT=").append(new Date()).append("\n");
        configContent.append("CREATED_BY=Auto-organized\n");
        
        Path configPath = branchPath.resolve(".branch-config");
        Files.writeString(configPath, configContent.toString());
        branchInfo.addFile(".branch-config");
    }

    private void initializeGitRepository(String branchesPath) throws IOException {
        // Initialize git repository in the branches directory
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(new File(branchesPath));
        
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Git repository initialized successfully");
            } else {
                System.err.println("Failed to initialize git repository");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git initialization interrupted", e);
        }
    }

    public Map<String, Object> createGitCommits(String branchesPath, Map<String, BranchInfo> branches) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> createdCommits = new ArrayList<>();
        
        for (Map.Entry<String, BranchInfo> entry : branches.entrySet()) {
            String branchName = entry.getKey();
            BranchInfo branchInfo = entry.getValue();
            
            try {
                // Create and checkout branch
                createGitBranch(branchesPath, branchName);
                
                // Add files and commit
                String commitHash = commitBranchFiles(branchesPath, branchName, branchInfo);
                createdCommits.add(branchName + ":" + commitHash);
                
            } catch (Exception e) {
                System.err.println("Failed to create commit for branch " + branchName + ": " + e.getMessage());
            }
        }
        
        result.put("createdCommits", createdCommits);
        result.put("totalCommits", createdCommits.size());
        result.put("status", "success");
        
        return result;
    }

    private void createGitBranch(String repoPath, String branchName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "checkout", "-b", branchName);
        pb.directory(new File(repoPath));
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to create git branch: " + branchName);
        }
    }

    private String commitBranchFiles(String repoPath, String branchName, BranchInfo branchInfo) throws IOException, InterruptedException {
        // Add all files
        ProcessBuilder addPb = new ProcessBuilder("git", "add", ".");
        addPb.directory(new File(repoPath));
        Process addProcess = addPb.start();
        addProcess.waitFor();
        
        // Commit files
        String commitMessage = "Add " + branchName + " components";
        ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", commitMessage);
        commitPb.directory(new File(repoPath));
        Process commitProcess = commitPb.start();
        int exitCode = commitProcess.waitFor();
        
        if (exitCode == 0) {
            // Get commit hash
            ProcessBuilder hashPb = new ProcessBuilder("git", "rev-parse", "HEAD");
            hashPb.directory(new File(repoPath));
            Process hashProcess = hashPb.start();
            
            try (Scanner scanner = new Scanner(hashProcess.getInputStream())) {
                if (scanner.hasNextLine()) {
                    return scanner.nextLine().substring(0, 8); // Short hash
                }
            }
        }
        
        return "unknown";
    }

    // Helper class to track branch information
    public static class BranchInfo {
        private String name;
        private String path;
        private List<String> files;
        private int fileCount;

        public BranchInfo(String name, String path) {
            this.name = name;
            this.path = path;
            this.files = new ArrayList<>();
            this.fileCount = 0;
        }

        public void addFile(String filePath) {
            files.add(filePath);
            fileCount++;
        }

        // Getters
        public String getName() { return name; }
        public String getPath() { return path; }
        public List<String> getFiles() { return files; }
        public int getFileCount() { return fileCount; }
    }
}
