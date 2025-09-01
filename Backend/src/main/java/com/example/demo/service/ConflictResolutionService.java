package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ConflictResolutionService {

    public Map<String, Object> detectAndResolveConflicts(String extractedPath) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        // Detect various types of conflicts
        List<DuplicateFile> duplicateFiles = detectDuplicateFiles(extractedPath);
        List<String> largeFiles = detectLargeFiles(extractedPath);
        List<String> secretFiles = detectFilesWithSecrets(extractedPath);
        List<String> conflictingNames = detectConflictingNames(extractedPath);
        
        // Generate resolution suggestions
        Map<String, Object> resolutions = generateResolutions(duplicateFiles, largeFiles, secretFiles, conflictingNames);
        
        result.put("duplicateFiles", duplicateFiles);
        result.put("largeFiles", largeFiles);
        result.put("secretFiles", secretFiles);
        result.put("conflictingNames", conflictingNames);
        result.put("resolutions", resolutions);
        result.put("hasConflicts", !duplicateFiles.isEmpty() || !largeFiles.isEmpty() || !secretFiles.isEmpty() || !conflictingNames.isEmpty());
        
        return result;
    }

    private List<DuplicateFile> detectDuplicateFiles(String extractedPath) throws IOException {
        Map<String, List<String>> filesByName = new HashMap<>();
        Map<String, String> fileHashes = new HashMap<>();
        List<DuplicateFile> duplicates = new ArrayList<>();
        
        Path rootPath = Paths.get(extractedPath);
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         String fileName = path.getFileName().toString();
                         String relativePath = rootPath.relativize(path).toString();
                         
                         // Group by filename
                         filesByName.computeIfAbsent(fileName, k -> new ArrayList<>()).add(relativePath);
                         
                         // Calculate hash for content comparison
                         if (Files.size(path) < 10 * 1024 * 1024) { // Only hash files < 10MB
                             byte[] content = Files.readAllBytes(path);
                             String hash = Integer.toHexString(Arrays.hashCode(content));
                             fileHashes.put(relativePath, hash);
                         }
                     } catch (IOException e) {
                         // Skip files that can't be read
                     }
                 });
        }
        
        // Find duplicates by name
        for (Map.Entry<String, List<String>> entry : filesByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                String fileName = entry.getKey();
                List<String> paths = entry.getValue();
                
                // Check if they have the same content
                Set<String> uniqueHashes = new HashSet<>();
                for (String path : paths) {
                    String hash = fileHashes.get(path);
                    if (hash != null) {
                        uniqueHashes.add(hash);
                    }
                }
                
                DuplicateFile duplicate = new DuplicateFile();
                duplicate.fileName = fileName;
                duplicate.paths = paths;
                duplicate.sameContent = uniqueHashes.size() <= 1;
                duplicate.conflictType = duplicate.sameContent ? "IDENTICAL_CONTENT" : "DIFFERENT_CONTENT";
                
                duplicates.add(duplicate);
            }
        }
        
        return duplicates;
    }

    private List<String> detectLargeFiles(String extractedPath) throws IOException {
        List<String> largeFiles = new ArrayList<>();
        Path rootPath = Paths.get(extractedPath);
        long maxSize = 100 * 1024 * 1024; // 100MB threshold
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         if (Files.size(path) > maxSize) {
                             String relativePath = rootPath.relativize(path).toString();
                             largeFiles.add(relativePath + " (" + formatFileSize(Files.size(path)) + ")");
                         }
                     } catch (IOException e) {
                         // Skip files that can't be read
                     }
                 });
        }
        
        return largeFiles;
    }

    private List<String> detectFilesWithSecrets(String extractedPath) throws IOException {
        List<String> secretFiles = new ArrayList<>();
        Path rootPath = Paths.get(extractedPath);
        
        // Patterns for detecting secrets
        List<String> secretPatterns = Arrays.asList(
            "(?i)(api[_-]?key|apikey)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?",
            "(?i)(secret[_-]?key|secretkey)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?",
            "(?i)(password|passwd|pwd)\\s*[=:]\\s*['\"]?([^\\s'\"]{8,})['\"]?",
            "(?i)(database[_-]?url|db[_-]?url)\\s*[=:]\\s*['\"]?([^\\s'\"]+)['\"]?",
            "(?i)(aws[_-]?access[_-]?key|aws[_-]?secret)\\s*[=:]\\s*['\"]?([A-Z0-9]{20})['\"]?",
            "(?i)(github[_-]?token|gh[_-]?token)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{40})['\"]?"
        );
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> isTextFile(path.getFileName().toString()))
                 .forEach(path -> {
                     try {
                         String content = Files.readString(path);
                         for (String pattern : secretPatterns) {
                             if (content.matches(".*" + pattern + ".*")) {
                                 String relativePath = rootPath.relativize(path).toString();
                                 secretFiles.add(relativePath);
                                 break;
                             }
                         }
                     } catch (IOException e) {
                         // Skip files that can't be read
                     }
                 });
        }
        
        return secretFiles;
    }

    private List<String> detectConflictingNames(String extractedPath) throws IOException {
        List<String> conflicts = new ArrayList<>();
        Path rootPath = Paths.get(extractedPath);
        Map<String, List<String>> nameGroups = new HashMap<>();
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     String relativePath = rootPath.relativize(path).toString();
                     nameGroups.computeIfAbsent(fileName, k -> new ArrayList<>()).add(relativePath);
                 });
        }
        
        // Find case-insensitive conflicts
        for (Map.Entry<String, List<String>> entry : nameGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Check if the actual filenames are different (case-sensitive)
                Set<String> actualNames = new HashSet<>();
                for (String path : entry.getValue()) {
                    actualNames.add(Paths.get(path).getFileName().toString());
                }
                
                if (actualNames.size() > 1) {
                    conflicts.add("Case conflict: " + String.join(", ", actualNames));
                }
            }
        }
        
        return conflicts;
    }

    private Map<String, Object> generateResolutions(List<DuplicateFile> duplicateFiles, List<String> largeFiles, 
                                                   List<String> secretFiles, List<String> conflictingNames) {
        Map<String, Object> resolutions = new HashMap<>();
        
        // Duplicate file resolutions
        List<Map<String, Object>> duplicateResolutions = new ArrayList<>();
        for (DuplicateFile duplicate : duplicateFiles) {
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("file", duplicate.fileName);
            resolution.put("paths", duplicate.paths);
            
            if (duplicate.sameContent) {
                resolution.put("action", "KEEP_ONE");
                resolution.put("suggestion", "Keep the file in the most appropriate location and remove others");
                resolution.put("recommended", selectBestPath(duplicate.paths));
            } else {
                resolution.put("action", "RENAME");
                resolution.put("suggestion", "Rename files to reflect their different purposes");
                resolution.put("recommendations", generateRenamedPaths(duplicate.paths));
            }
            
            duplicateResolutions.add(resolution);
        }
        resolutions.put("duplicateFiles", duplicateResolutions);
        
        // Large file resolutions
        List<Map<String, Object>> largeFileResolutions = new ArrayList<>();
        for (String largeFile : largeFiles) {
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("file", largeFile);
            resolution.put("action", "GITIGNORE");
            resolution.put("suggestion", "Add to .gitignore or consider using Git LFS");
            largeFileResolutions.add(resolution);
        }
        resolutions.put("largeFiles", largeFileResolutions);
        
        // Secret file resolutions
        List<Map<String, Object>> secretResolutions = new ArrayList<>();
        for (String secretFile : secretFiles) {
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("file", secretFile);
            resolution.put("action", "REMOVE_SECRETS");
            resolution.put("suggestion", "Remove sensitive data and use environment variables");
            secretResolutions.add(resolution);
        }
        resolutions.put("secretFiles", secretResolutions);
        
        // Conflicting name resolutions
        List<Map<String, Object>> nameResolutions = new ArrayList<>();
        for (String conflict : conflictingNames) {
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("conflict", conflict);
            resolution.put("action", "STANDARDIZE_CASE");
            resolution.put("suggestion", "Standardize filename casing to avoid conflicts on case-sensitive systems");
            nameResolutions.add(resolution);
        }
        resolutions.put("conflictingNames", nameResolutions);
        
        return resolutions;
    }

    private String selectBestPath(List<String> paths) {
        // Prefer paths that are in standard locations
        for (String path : paths) {
            if (path.contains("src/main") || path.contains("lib") || path.contains("app")) {
                return path;
            }
        }
        
        // Prefer shorter paths
        return paths.stream()
                   .min(Comparator.comparing(String::length))
                   .orElse(paths.get(0));
    }

    private List<String> generateRenamedPaths(List<String> paths) {
        List<String> renamed = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            String dir = path.substring(0, path.lastIndexOf('/') + 1);
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            
            String newName = baseName + "_" + (i + 1) + extension;
            renamed.add(dir + newName);
        }
        return renamed;
    }

    private boolean isTextFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") ||
               lowerFileName.endsWith(".json") || lowerFileName.endsWith(".xml") ||
               lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml") ||
               lowerFileName.endsWith(".properties") || lowerFileName.endsWith(".conf") ||
               lowerFileName.endsWith(".js") || lowerFileName.endsWith(".ts") ||
               lowerFileName.endsWith(".py") || lowerFileName.endsWith(".java") ||
               lowerFileName.endsWith(".cpp") || lowerFileName.endsWith(".c") ||
               lowerFileName.endsWith(".h") || lowerFileName.endsWith(".cs") ||
               lowerFileName.endsWith(".php") || lowerFileName.endsWith(".rb") ||
               lowerFileName.endsWith(".go") || lowerFileName.endsWith(".rs");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }

    public Map<String, Object> applyResolutions(String extractedPath, Map<String, Object> resolutions) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> appliedActions = new ArrayList<>();
        
        // Apply duplicate file resolutions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> duplicateResolutions = (List<Map<String, Object>>) resolutions.get("duplicateFiles");
        if (duplicateResolutions != null) {
            for (Map<String, Object> resolution : duplicateResolutions) {
                String action = (String) resolution.get("action");
                if ("KEEP_ONE".equals(action)) {
                    String recommended = (String) resolution.get("recommended");
                    @SuppressWarnings("unchecked")
                    List<String> paths = (List<String>) resolution.get("paths");
                    
                    for (String path : paths) {
                        if (!path.equals(recommended)) {
                            Path filePath = Paths.get(extractedPath, path);
                            Files.deleteIfExists(filePath);
                            appliedActions.add("Removed duplicate: " + path);
                        }
                    }
                }
            }
        }
        
        result.put("appliedActions", appliedActions);
        result.put("success", true);
        
        return result;
    }

    // Helper class for duplicate file information
    public static class DuplicateFile {
        public String fileName;
        public List<String> paths;
        public boolean sameContent;
        public String conflictType;
    }
}
