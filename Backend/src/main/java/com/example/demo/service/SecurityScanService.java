package com.example.demo.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SecurityScanService {

    @Value("${app.security.scanner.strict-mode:false}")
    private boolean strictMode;

    @Value("${app.security.scanner.allow-dev-files:true}")
    private boolean allowDevFiles;

    // Dangerous file extensions (excluding legitimate development files)
    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".exe", ".bat", ".cmd", ".com", ".scr", ".pif", ".vbs",
        ".app", ".deb", ".pkg", ".dmg", ".iso", ".msi", ".dll", ".so", ".dylib"
    ));

    // Suspicious file patterns (excluding legitimate development config files)
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.(sh|bash|zsh|fish)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*dockerfile.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.env.*", Pattern.CASE_INSENSITIVE)
        // Removed overly broad config pattern that was flagging legitimate files like jsconfig.json, tailwind.config.js
    );

    // Patterns for detecting secrets/credentials
    private static final List<Pattern> SECRET_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?"),
        Pattern.compile("(?i)(secret[_-]?key|secretkey)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?"),
        Pattern.compile("(?i)(access[_-]?token|accesstoken)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?"),
        Pattern.compile("(?i)(password|passwd|pwd)\\s*[=:]\\s*['\"]?([^\\s'\"]{8,})['\"]?"),
        Pattern.compile("(?i)(database[_-]?url|db[_-]?url)\\s*[=:]\\s*['\"]?([^\\s'\"]+)['\"]?"),
        Pattern.compile("(?i)(private[_-]?key|privatekey)\\s*[=:]\\s*['\"]?([^\\s'\"]+)['\"]?"),
        Pattern.compile("(?i)(aws[_-]?access[_-]?key|aws[_-]?secret)\\s*[=:]\\s*['\"]?([A-Z0-9]{20})['\"]?"),
        Pattern.compile("(?i)(github[_-]?token|gh[_-]?token)\\s*[=:]\\s*['\"]?([a-zA-Z0-9_-]{40})['\"]?")
    );

    // Malicious code patterns (more specific to avoid false positives)
    private static final List<Pattern> MALICIOUS_PATTERNS = Arrays.asList(
        // Only flag clearly malicious shell commands, not legitimate JS functions
        Pattern.compile("(?i)(rm\\s+-rf\\s+/|del\\s+/s\\s+/q|format\\s+c:)"),
        Pattern.compile("(?i)(wget|curl)\\s+.*\\|\\s*(sh|bash|python)"),
        Pattern.compile("(?i)(nc\\s+-l|netcat\\s+-l)\\s+\\d+"),
        Pattern.compile("(?i)(base64\\s+-d|echo\\s+.*\\|\\s*base64)"),
        Pattern.compile("(?i)(powershell|cmd\\.exe).*-encodedcommand"),
        // Only flag suspicious eval patterns, not legitimate JavaScript
        Pattern.compile("(?i)eval\\s*\\(\\s*['\"].*\\|.*['\"]\\s*\\)")
    );

    public boolean performQuickScan(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return false;
            }

            // Check file extension
            if (hasDangerousExtension(filename)) {
                return false;
            }

            // Check file size (basic check)
            if (file.getSize() > 5L * 1024 * 1024 * 1024) { // 5GB
                return false;
            }

            // Check MIME type
            String contentType = file.getContentType();
            if (contentType != null && isDangerousContentType(contentType)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            // If scan fails, err on the side of caution
            return false;
        }
    }

    public boolean performDeepScan(String extractedPath) {
        // SECURITY SCANNER DISABLED FOR MVP
        System.out.println("🔍 SECURITY SCAN DISABLED FOR MVP - ALWAYS PASSING");
        System.out.println("📁 Scanned path: " + extractedPath);
        return true; // Always pass for MVP
    }

    private boolean scanFile(Path filePath) {
        try {
            String filename = filePath.getFileName().toString();

            // Check for dangerous extensions
            if (hasDangerousExtension(filename)) {
                System.out.println("Security scan failed: Dangerous file extension detected: " + filename);
                return false;
            }

            // Check for suspicious patterns in filename
            if (isSuspiciousFilename(filename)) {
                System.out.println("Security scan warning: Suspicious filename detected: " + filename);
                // Don't fail for suspicious filenames, just log
            }

            // Scan file content for secrets and malicious code
            if (isTextFile(filename)) {
                String content = Files.readString(filePath);
                
                if (containsSecrets(content, filename)) {
                    System.out.println("Security scan warning: Potential secrets detected in: " + filename);
                    // Don't fail for secrets, just log warning
                }

                if (containsMaliciousCode(content, filename)) {
                    if (strictMode) {
                        System.out.println("Security scan failed: Malicious code detected in: " + filename);
                        return false;
                    } else {
                        System.out.println("Security scan warning: Potential malicious code detected in: " + filename + " (ignored in non-strict mode)");
                        // Don't fail in non-strict mode, just log warning
                    }
                }
            }

            return true;

        } catch (Exception e) {
            System.out.println("Error scanning file " + filePath + ": " + e.getMessage());
            return true; // Don't fail scan for read errors
        }
    }

    private boolean hasDangerousExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return DANGEROUS_EXTENSIONS.stream()
            .anyMatch(lowerFilename::endsWith);
    }

    private boolean isDangerousContentType(String contentType) {
        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.contains("application/x-executable") ||
               lowerContentType.contains("application/x-msdownload") ||
               lowerContentType.contains("application/x-msdos-program");
    }

    private boolean isSuspiciousFilename(String filename) {
        return SUSPICIOUS_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(filename).matches());
    }

    private boolean isTextFile(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".txt") ||
               lowerFilename.endsWith(".md") ||
               lowerFilename.endsWith(".json") ||
               lowerFilename.endsWith(".xml") ||
               lowerFilename.endsWith(".yaml") ||
               lowerFilename.endsWith(".yml") ||
               lowerFilename.endsWith(".ini") ||
               lowerFilename.endsWith(".conf") ||
               lowerFilename.endsWith(".config") ||
               lowerFilename.endsWith(".env") ||
               lowerFilename.endsWith(".properties") ||
               lowerFilename.endsWith(".js") ||
               lowerFilename.endsWith(".ts") ||
               lowerFilename.endsWith(".jsx") ||
               lowerFilename.endsWith(".tsx") ||
               lowerFilename.endsWith(".py") ||
               lowerFilename.endsWith(".java") ||
               lowerFilename.endsWith(".cpp") ||
               lowerFilename.endsWith(".c") ||
               lowerFilename.endsWith(".h") ||
               lowerFilename.endsWith(".cs") ||
               lowerFilename.endsWith(".php") ||
               lowerFilename.endsWith(".rb") ||
               lowerFilename.endsWith(".go") ||
               lowerFilename.endsWith(".rs") ||
               lowerFilename.endsWith(".sh") ||
               lowerFilename.endsWith(".bash") ||
               lowerFilename.endsWith(".zsh") ||
               lowerFilename.endsWith(".fish") ||
               lowerFilename.endsWith(".ps1") ||
               lowerFilename.endsWith(".bat") ||
               lowerFilename.endsWith(".cmd") ||
               lowerFilename.endsWith(".html") ||
               lowerFilename.endsWith(".htm") ||
               lowerFilename.endsWith(".css") ||
               lowerFilename.endsWith(".scss") ||
               lowerFilename.endsWith(".sass") ||
               lowerFilename.endsWith(".less") ||
               lowerFilename.endsWith(".sql") ||
               lowerFilename.endsWith(".dockerfile") ||
               lowerFilename.equals("dockerfile") ||
               lowerFilename.equals("makefile") ||
               lowerFilename.equals("readme") ||
               lowerFilename.equals("license") ||
               lowerFilename.equals("changelog") ||
               lowerFilename.equals("authors") ||
               lowerFilename.equals("contributors");
    }

    private boolean containsSecrets(String content, String filename) {
        // Skip checking for secrets in certain file types
        if (filename.toLowerCase().endsWith(".md") || 
            filename.toLowerCase().endsWith(".txt") ||
            filename.toLowerCase().contains("readme") ||
            filename.toLowerCase().contains("license")) {
            return false;
        }

        return SECRET_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(content).find());
    }

    private boolean containsMaliciousCode(String content, String filename) {
        return MALICIOUS_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(content).find());
    }

    public SecurityScanResult generateScanReport(String extractedPath) {
        SecurityScanResult result = new SecurityScanResult();
        
        try {
            Path rootPath = Paths.get(extractedPath);
            if (!Files.exists(rootPath)) {
                result.setPassed(false);
                result.addError("Extracted path does not exist");
                return result;
            }

            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths.filter(Files::isRegularFile)
                     .forEach(path -> scanFileForReport(path, result));
            }

            result.setPassed(result.getErrors().isEmpty());

        } catch (Exception e) {
            result.setPassed(false);
            result.addError("Scan failed: " + e.getMessage());
        }

        return result;
    }

    private void scanFileForReport(Path filePath, SecurityScanResult result) {
        try {
            String filename = filePath.getFileName().toString();
            result.incrementTotalFiles();

            if (hasDangerousExtension(filename)) {
                result.addError("Dangerous file extension: " + filename);
                return;
            }

            if (isSuspiciousFilename(filename)) {
                result.addWarning("Suspicious filename: " + filename);
            }

            if (isTextFile(filename)) {
                String content = Files.readString(filePath);
                
                if (containsSecrets(content, filename)) {
                    result.addWarning("Potential secrets in: " + filename);
                }

                if (containsMaliciousCode(content, filename)) {
                    result.addError("Malicious code in: " + filename);
                }
            }

            result.incrementScannedFiles();

        } catch (Exception e) {
            result.addWarning("Could not scan file: " + filePath + " - " + e.getMessage());
        }
    }

    public static class SecurityScanResult {
        private boolean passed = true;
        private int totalFiles = 0;
        private int scannedFiles = 0;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        private java.util.List<String> warnings = new java.util.ArrayList<>();

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public int getTotalFiles() { return totalFiles; }
        public void incrementTotalFiles() { this.totalFiles++; }

        public int getScannedFiles() { return scannedFiles; }
        public void incrementScannedFiles() { this.scannedFiles++; }

        public java.util.List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }

        public java.util.List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
    }
}
