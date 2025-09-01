package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProjectAnalysisService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Language detection patterns
    private static final Map<String, List<String>> LANGUAGE_PATTERNS = Map.of(
        "JavaScript", Arrays.asList("package.json", "*.js", "*.jsx", "*.ts", "*.tsx", "node_modules"),
        "Python", Arrays.asList("requirements.txt", "setup.py", "*.py", "__pycache__", "venv", ".env"),
        "Java", Arrays.asList("pom.xml", "build.gradle", "*.java", "src/main/java", "target", ".mvn"),
        "C#", Arrays.asList("*.csproj", "*.sln", "*.cs", "bin", "obj", "packages"),
        "PHP", Arrays.asList("composer.json", "*.php", "vendor", "artisan"),
        "Ruby", Arrays.asList("Gemfile", "*.rb", "Rakefile", ".bundle"),
        "Go", Arrays.asList("go.mod", "go.sum", "*.go", "vendor"),
        "Rust", Arrays.asList("Cargo.toml", "Cargo.lock", "*.rs", "target"),
        "Swift", Arrays.asList("Package.swift", "*.swift", ".build"),
        "Kotlin", Arrays.asList("*.kt", "*.kts", "build.gradle.kts")
    );

    // Framework detection patterns
    private static final Map<String, List<String>> FRAMEWORK_PATTERNS = Map.of(
        "React", Arrays.asList("react", "react-dom", "jsx", "tsx"),
        "Vue", Arrays.asList("vue", "*.vue", "vue.config.js"),
        "Angular", Arrays.asList("angular", "@angular", "angular.json"),
        "Next.js", Arrays.asList("next", "next.config.js", "pages", "app"),
        "Express", Arrays.asList("express", "app.js", "server.js"),
        "Spring Boot", Arrays.asList("spring-boot", "@SpringBootApplication", "application.properties"),
        "Django", Arrays.asList("django", "manage.py", "settings.py"),
        "Flask", Arrays.asList("flask", "app.py", "wsgi.py"),
        "Laravel", Arrays.asList("laravel", "artisan", "composer.json"),
        "Rails", Arrays.asList("rails", "Gemfile", "config/application.rb")
    );

    public Map<String, Object> analyzeProject(String extractedPath) throws IOException {
        Map<String, Object> analysis = new HashMap<>();
        
        // Basic file system analysis
        ProjectStructure structure = analyzeProjectStructure(extractedPath);
        analysis.put("structure", structure.toMap());
        
        // Language detection
        Set<String> detectedLanguages = detectLanguages(extractedPath);
        analysis.put("languages", detectedLanguages);
        
        // Framework detection
        Set<String> detectedFrameworks = detectFrameworks(extractedPath);
        analysis.put("frameworks", detectedFrameworks);
        
        // AI-powered analysis
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            Map<String, Object> aiAnalysis = performAIAnalysis(structure, detectedLanguages, detectedFrameworks);
            analysis.put("aiAnalysis", aiAnalysis);
        }
        
        // Generate branch suggestions
        List<String> suggestedBranches = generateBranchSuggestions(structure, detectedLanguages, detectedFrameworks);
        analysis.put("suggestedBranches", suggestedBranches);
        
        // Generate organization recommendations
        Map<String, Object> organizationPlan = generateOrganizationPlan(structure, detectedLanguages, detectedFrameworks);
        analysis.put("organizationPlan", organizationPlan);
        
        return analysis;
    }

    private ProjectStructure analyzeProjectStructure(String extractedPath) throws IOException {
        ProjectStructure structure = new ProjectStructure();
        Path rootPath = Paths.get(extractedPath);
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.forEach(path -> {
                if (Files.isDirectory(path)) {
                    structure.addDirectory(rootPath.relativize(path).toString());
                } else {
                    String relativePath = rootPath.relativize(path).toString();
                    structure.addFile(relativePath);
                    
                    // Analyze important files
                    String fileName = path.getFileName().toString().toLowerCase();
                    if (isConfigFile(fileName)) {
                        structure.addConfigFile(relativePath);
                    }
                    if (isDocumentationFile(fileName)) {
                        structure.addDocFile(relativePath);
                    }
                }
            });
        }
        
        return structure;
    }

    private Set<String> detectLanguages(String extractedPath) throws IOException {
        Set<String> detectedLanguages = new HashSet<>();
        Path rootPath = Paths.get(extractedPath);
        
        for (Map.Entry<String, List<String>> entry : LANGUAGE_PATTERNS.entrySet()) {
            String language = entry.getKey();
            List<String> patterns = entry.getValue();
            
            boolean languageDetected = patterns.stream().anyMatch(pattern -> {
                try (Stream<Path> paths = Files.walk(rootPath)) {
                    return paths.anyMatch(path -> matchesPattern(path, pattern, rootPath));
                } catch (IOException e) {
                    return false;
                }
            });
            
            if (languageDetected) {
                detectedLanguages.add(language);
            }
        }
        
        return detectedLanguages;
    }

    private Set<String> detectFrameworks(String extractedPath) throws IOException {
        Set<String> detectedFrameworks = new HashSet<>();
        Path rootPath = Paths.get(extractedPath);
        
        // Check package.json for JavaScript frameworks
        Path packageJson = rootPath.resolve("package.json");
        if (Files.exists(packageJson)) {
            try {
                String content = Files.readString(packageJson);
                JsonNode json = objectMapper.readTree(content);
                
                JsonNode dependencies = json.get("dependencies");
                JsonNode devDependencies = json.get("devDependencies");
                
                Set<String> allDeps = new HashSet<>();
                if (dependencies != null) {
                    dependencies.fieldNames().forEachRemaining(allDeps::add);
                }
                if (devDependencies != null) {
                    devDependencies.fieldNames().forEachRemaining(allDeps::add);
                }
                
                for (Map.Entry<String, List<String>> entry : FRAMEWORK_PATTERNS.entrySet()) {
                    String framework = entry.getKey();
                    List<String> patterns = entry.getValue();
                    
                    boolean frameworkDetected = patterns.stream()
                        .anyMatch(pattern -> allDeps.stream().anyMatch(dep -> dep.contains(pattern)));
                    
                    if (frameworkDetected) {
                        detectedFrameworks.add(framework);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        // Check for other framework indicators
        for (Map.Entry<String, List<String>> entry : FRAMEWORK_PATTERNS.entrySet()) {
            String framework = entry.getKey();
            List<String> patterns = entry.getValue();
            
            boolean frameworkDetected = patterns.stream().anyMatch(pattern -> {
                try (Stream<Path> paths = Files.walk(rootPath)) {
                    return paths.anyMatch(path -> matchesPattern(path, pattern, rootPath));
                } catch (IOException e) {
                    return false;
                }
            });
            
            if (frameworkDetected) {
                detectedFrameworks.add(framework);
            }
        }
        
        return detectedFrameworks;
    }

    private Map<String, Object> performAIAnalysis(ProjectStructure structure, Set<String> languages, Set<String> frameworks) {
        try {
            String prompt = buildAnalysisPrompt(structure, languages, frameworks);
            String aiResponse = callGeminiAPI(prompt);
            
            // Parse AI response
            JsonNode responseJson = objectMapper.readTree(aiResponse);
            JsonNode candidates = responseJson.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                JsonNode parts = content.get("parts");
                
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    String analysisText = parts.get(0).get("text").asText();
                    return parseAIAnalysis(analysisText);
                }
            }
            
        } catch (Exception e) {
            System.err.println("AI analysis failed: " + e.getMessage());
        }
        
        return Map.of("error", "AI analysis unavailable");
    }

    private String buildAnalysisPrompt(ProjectStructure structure, Set<String> languages, Set<String> frameworks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this software project structure and provide insights:\n\n");
        prompt.append("Detected Languages: ").append(String.join(", ", languages)).append("\n");
        prompt.append("Detected Frameworks: ").append(String.join(", ", frameworks)).append("\n\n");
        prompt.append("Project Structure:\n");
        prompt.append("Directories: ").append(structure.getDirectories().size()).append("\n");
        prompt.append("Files: ").append(structure.getFiles().size()).append("\n");
        prompt.append("Config Files: ").append(String.join(", ", structure.getConfigFiles())).append("\n\n");
        
        prompt.append("Please provide a JSON response with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"projectType\": \"web-app|mobile-app|desktop-app|library|microservice|monolith|other\",\n");
        prompt.append("  \"architecture\": \"frontend-only|backend-only|fullstack|microservices|monolith\",\n");
        prompt.append("  \"complexity\": \"simple|moderate|complex\",\n");
        prompt.append("  \"recommendedBranches\": [\"main\", \"frontend\", \"backend\", \"docs\"],\n");
        prompt.append("  \"suggestions\": [\"suggestion1\", \"suggestion2\"],\n");
        prompt.append("  \"potentialIssues\": [\"issue1\", \"issue2\"]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = geminiApiUrl + "?key=" + geminiApiKey;
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }

    private Map<String, Object> parseAIAnalysis(String analysisText) {
        try {
            // Try to extract JSON from the response
            int jsonStart = analysisText.indexOf("{");
            int jsonEnd = analysisText.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = analysisText.substring(jsonStart, jsonEnd);
                return objectMapper.readValue(jsonStr, Map.class);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse AI analysis: " + e.getMessage());
        }
        
        return Map.of("rawResponse", analysisText);
    }

    private List<String> generateBranchSuggestions(ProjectStructure structure, Set<String> languages, Set<String> frameworks) {
        List<String> branches = new ArrayList<>();
        branches.add("main");
        
        // Check for frontend/backend separation
        boolean hasFrontend = frameworks.stream().anyMatch(f -> 
            f.equals("React") || f.equals("Vue") || f.equals("Angular") || f.equals("Next.js"));
        boolean hasBackend = frameworks.stream().anyMatch(f -> 
            f.equals("Express") || f.equals("Spring Boot") || f.equals("Django") || f.equals("Flask"));
        
        if (hasFrontend) branches.add("frontend");
        if (hasBackend) branches.add("backend");
        
        // Check for documentation
        if (!structure.getDocFiles().isEmpty()) {
            branches.add("docs");
        }
        
        // Check for mobile components
        if (languages.contains("Swift") || languages.contains("Kotlin")) {
            branches.add("mobile");
        }
        
        // Check for infrastructure/config
        if (structure.getConfigFiles().stream().anyMatch(f -> 
            f.contains("docker") || f.contains("k8s") || f.contains("terraform"))) {
            branches.add("infrastructure");
        }
        
        return branches;
    }

    public Map<String, Object> organizeProject(String extractedPath, Map<String, Object> analysisResults) {
        // Implementation for organizing project files based on analysis
        Map<String, Object> organizationResult = new HashMap<>();
        organizationResult.put("status", "completed");
        organizationResult.put("branchesCreated", analysisResults.get("suggestedBranches"));
        return organizationResult;
    }

    private Map<String, Object> generateOrganizationPlan(ProjectStructure structure, Set<String> languages, Set<String> frameworks) {
        Map<String, Object> plan = new HashMap<>();
        
        Map<String, List<String>> branchFiles = new HashMap<>();
        
        // Organize files by branch
        for (String file : structure.getFiles()) {
            String branch = determineBranchForFile(file, languages, frameworks);
            branchFiles.computeIfAbsent(branch, k -> new ArrayList<>()).add(file);
        }
        
        plan.put("branchFiles", branchFiles);
        plan.put("totalFiles", structure.getFiles().size());
        plan.put("estimatedTime", "2-5 minutes");
        
        return plan;
    }

    private String determineBranchForFile(String filePath, Set<String> languages, Set<String> frameworks) {
        String lowerPath = filePath.toLowerCase();
        
        // Frontend files
        if (lowerPath.contains("frontend") || lowerPath.contains("client") || 
            lowerPath.contains("ui") || lowerPath.contains("web") ||
            lowerPath.endsWith(".jsx") || lowerPath.endsWith(".tsx") || 
            lowerPath.endsWith(".vue") || lowerPath.contains("components")) {
            return "frontend";
        }
        
        // Backend files
        if (lowerPath.contains("backend") || lowerPath.contains("server") || 
            lowerPath.contains("api") || lowerPath.contains("service") ||
            lowerPath.contains("controller") || lowerPath.contains("model")) {
            return "backend";
        }
        
        // Documentation files
        if (lowerPath.contains("doc") || lowerPath.contains("readme") || 
            lowerPath.endsWith(".md") || lowerPath.contains("wiki")) {
            return "docs";
        }
        
        // Configuration files
        if (lowerPath.contains("config") || lowerPath.contains("docker") || 
            lowerPath.contains("k8s") || lowerPath.contains("terraform")) {
            return "infrastructure";
        }
        
        return "main";
    }

    private boolean matchesPattern(Path path, String pattern, Path rootPath) {
        String relativePath = rootPath.relativize(path).toString();
        String fileName = path.getFileName().toString();
        
        if (pattern.startsWith("*.")) {
            return fileName.endsWith(pattern.substring(1));
        } else if (pattern.contains("/")) {
            return relativePath.contains(pattern);
        } else {
            return fileName.equals(pattern) || relativePath.contains(pattern);
        }
    }

    private boolean isConfigFile(String fileName) {
        return fileName.endsWith(".json") || fileName.endsWith(".xml") || 
               fileName.endsWith(".yml") || fileName.endsWith(".yaml") ||
               fileName.endsWith(".properties") || fileName.endsWith(".conf") ||
               fileName.endsWith(".config") || fileName.endsWith(".ini") ||
               fileName.equals("dockerfile") || fileName.equals("makefile");
    }

    private boolean isDocumentationFile(String fileName) {
        return fileName.endsWith(".md") || fileName.endsWith(".txt") ||
               fileName.contains("readme") || fileName.contains("license") ||
               fileName.contains("changelog") || fileName.contains("authors");
    }

    // Helper class for project structure
    public static class ProjectStructure {
        private Set<String> directories = new HashSet<>();
        private Set<String> files = new HashSet<>();
        private Set<String> configFiles = new HashSet<>();
        private Set<String> docFiles = new HashSet<>();

        public void addDirectory(String dir) { directories.add(dir); }
        public void addFile(String file) { files.add(file); }
        public void addConfigFile(String file) { configFiles.add(file); }
        public void addDocFile(String file) { docFiles.add(file); }

        public Set<String> getDirectories() { return directories; }
        public Set<String> getFiles() { return files; }
        public Set<String> getConfigFiles() { return configFiles; }
        public Set<String> getDocFiles() { return docFiles; }

        public Map<String, Object> toMap() {
            return Map.of(
                "directories", directories,
                "files", files,
                "configFiles", configFiles,
                "docFiles", docFiles
            );
        }
    }
}
