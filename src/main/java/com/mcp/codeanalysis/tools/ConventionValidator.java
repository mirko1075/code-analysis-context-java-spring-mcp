package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.FileScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Convention validation tool for MCP.
 * Validates Java naming conventions, package structure, and Spring Boot best practices.
 */
public class ConventionValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConventionValidator.class);

    // Naming convention patterns
    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern PACKAGE_NAME = Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$");

    /**
     * Validate project conventions.
     *
     * @param projectPath Path to project root
     * @param options Validation options
     * @return Validation result
     */
    public ValidationResult validate(String projectPath, ValidationOptions options) {
        Path projectRoot = Paths.get(projectPath);
        ValidationResult result = new ValidationResult();

        try {
            // Scan for Java files
            FileScanner fileScanner = new FileScanner(projectRoot);
            List<Path> javaFiles = fileScanner.scanJavaFiles();

            JavaSourceParser javaParser = new JavaSourceParser();

            for (Path javaFile : javaFiles) {
                JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
                if (fileInfo != null) {
                    validateFile(fileInfo, result, options);
                }
            }

            // Calculate consistency scores
            calculateConsistencyScores(result);

        } catch (Exception e) {
            logger.error("Error validating conventions: {}", projectPath, e);
        }

        return result;
    }

    /**
     * Validate a single file.
     */
    private void validateFile(JavaFileInfo fileInfo, ValidationResult result, ValidationOptions options) {
        // Validate package naming
        validatePackageNaming(fileInfo, result, options);

        // Validate imports
        validateImports(fileInfo, result, options);

        // Validate each class
        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            validateClass(classInfo, fileInfo, result, options);
        }
    }

    /**
     * Validate package naming.
     */
    private void validatePackageNaming(JavaFileInfo fileInfo, ValidationResult result, ValidationOptions options) {
        String packageName = fileInfo.getPackageName();
        if (packageName != null && !packageName.isEmpty()) {
            if (!PACKAGE_NAME.matcher(packageName).matches()) {
                result.addViolation(new Violation(
                        "Package Naming",
                        "Package name should be lowercase: " + packageName,
                        fileInfo.getFilePath(),
                        0,
                        "WARN"
                ));
            }
        }
    }

    /**
     * Validate imports.
     */
    private void validateImports(JavaFileInfo fileInfo, ValidationResult result, ValidationOptions options) {
        List<String> imports = fileInfo.getImports();

        // Check for wildcard imports
        for (String imp : imports) {
            if (imp.endsWith(".*")) {
                result.addViolation(new Violation(
                        "Import Organization",
                        "Avoid wildcard imports: " + imp,
                        fileInfo.getFilePath(),
                        0,
                        "INFO"
                ));
            }
        }

        // Check for unused imports (basic check - if import not mentioned in any class)
        // This is a simplified check - a full analysis would require symbol resolution
    }

    /**
     * Validate a class.
     */
    private void validateClass(JavaFileInfo.ClassInfo classInfo, JavaFileInfo fileInfo,
                               ValidationResult result, ValidationOptions options) {
        String className = classInfo.getName();
        String filePath = fileInfo.getFilePath();

        // Validate class naming
        if (!PASCAL_CASE.matcher(className).matches()) {
            result.addViolation(new Violation(
                    "Class Naming",
                    "Class name should be PascalCase: " + className,
                    filePath,
                    classInfo.getStartLine(),
                    "WARN"
            ));
        }

        // Validate Spring component naming
        validateSpringComponentNaming(classInfo, fileInfo, result, options);

        // Validate fields
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            validateField(field, className, filePath, result, options);
        }

        // Validate methods
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            validateMethod(method, className, filePath, result, options);
        }

        // Validate Spring-specific patterns
        validateSpringPatterns(classInfo, fileInfo, result, options);
    }

    /**
     * Validate Spring component naming.
     */
    private void validateSpringComponentNaming(JavaFileInfo.ClassInfo classInfo, JavaFileInfo fileInfo,
                                               ValidationResult result, ValidationOptions options) {
        List<String> annotations = classInfo.getAnnotations();
        String className = classInfo.getName();

        // Controller naming
        if (annotations.contains("Controller") || annotations.contains("RestController")) {
            if (!className.endsWith("Controller")) {
                result.addViolation(new Violation(
                        "Spring Naming",
                        "Controller classes should end with 'Controller': " + className,
                        fileInfo.getFilePath(),
                        classInfo.getStartLine(),
                        "WARN"
                ));
            }
        }

        // Service naming
        if (annotations.contains("Service")) {
            if (!className.endsWith("Service")) {
                result.addViolation(new Violation(
                        "Spring Naming",
                        "Service classes should end with 'Service': " + className,
                        fileInfo.getFilePath(),
                        classInfo.getStartLine(),
                        "WARN"
                ));
            }
        }

        // Repository naming
        if (annotations.contains("Repository")) {
            if (!className.endsWith("Repository") && !className.endsWith("Dao")) {
                result.addViolation(new Violation(
                        "Spring Naming",
                        "Repository classes should end with 'Repository' or 'Dao': " + className,
                        fileInfo.getFilePath(),
                        classInfo.getStartLine(),
                        "WARN"
                ));
            }
        }
    }

    /**
     * Validate field.
     */
    private void validateField(JavaFileInfo.FieldInfo field, String className, String filePath,
                              ValidationResult result, ValidationOptions options) {
        String fieldName = field.getName();

        // Check if constant (static final)
        if (field.getModifiers().contains("static") && field.getModifiers().contains("final")) {
            if (!UPPER_SNAKE_CASE.matcher(fieldName).matches()) {
                result.addViolation(new Violation(
                        "Field Naming",
                        "Constants should be UPPER_SNAKE_CASE: " + className + "." + fieldName,
                        filePath,
                        field.getLine(),
                        "WARN"
                ));
            }
        } else {
            // Regular field - should be camelCase
            if (!CAMEL_CASE.matcher(fieldName).matches()) {
                result.addViolation(new Violation(
                        "Field Naming",
                        "Fields should be camelCase: " + className + "." + fieldName,
                        filePath,
                        field.getLine(),
                        "WARN"
                ));
            }
        }
    }

    /**
     * Validate method.
     */
    private void validateMethod(JavaFileInfo.MethodInfo method, String className, String filePath,
                               ValidationResult result, ValidationOptions options) {
        String methodName = method.getName();

        // Skip constructors
        if (methodName.equals(className)) {
            return;
        }

        // Method naming - should be camelCase
        if (!CAMEL_CASE.matcher(methodName).matches()) {
            result.addViolation(new Violation(
                    "Method Naming",
                    "Methods should be camelCase: " + className + "." + methodName,
                    filePath,
                    method.getStartLine(),
                    "WARN"
            ));
        }

        // Check for long methods (> 50 lines)
        int methodLength = method.getEndLine() - method.getStartLine();
        if (methodLength > 50) {
            result.addViolation(new Violation(
                    "Method Length",
                    "Method is too long (" + methodLength + " lines): " + className + "." + methodName,
                    filePath,
                    method.getStartLine(),
                    "INFO"
            ));
        }
    }

    /**
     * Validate Spring-specific patterns.
     */
    private void validateSpringPatterns(JavaFileInfo.ClassInfo classInfo, JavaFileInfo fileInfo,
                                       ValidationResult result, ValidationOptions options) {
        List<String> annotations = classInfo.getAnnotations();

        // Check for field injection (discouraged)
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            if (field.getAnnotations().contains("Autowired")) {
                result.addViolation(new Violation(
                        "Spring Injection",
                        "Prefer constructor injection over field injection: " + classInfo.getName() + "." + field.getName(),
                        fileInfo.getFilePath(),
                        field.getLine(),
                        "WARN"
                ));
            }
        }

        // Check for @Transactional on controllers (should be on services)
        if (annotations.contains("Controller") || annotations.contains("RestController")) {
            for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                if (method.getAnnotations().contains("Transactional")) {
                    result.addViolation(new Violation(
                            "Spring Transaction",
                            "@Transactional should be on service layer, not controllers: " + classInfo.getName() + "." + method.getName(),
                            fileInfo.getFilePath(),
                            method.getStartLine(),
                            "ERROR"
                    ));
                }
            }
        }

        // Check for proper exception handling in controllers
        if (annotations.contains("RestController")) {
            boolean hasExceptionHandler = annotations.contains("ControllerAdvice") ||
                    classInfo.getMethods().stream()
                            .anyMatch(m -> m.getAnnotations().contains("ExceptionHandler"));

            if (!hasExceptionHandler) {
                result.addViolation(new Violation(
                        "Spring Exception Handling",
                        "REST controllers should have @ExceptionHandler or @ControllerAdvice: " + classInfo.getName(),
                        fileInfo.getFilePath(),
                        classInfo.getStartLine(),
                        "INFO"
                ));
            }
        }
    }

    /**
     * Calculate consistency scores.
     */
    private void calculateConsistencyScores(ValidationResult result) {
        Map<String, Integer> totalByCategory = new HashMap<>();
        Map<String, Integer> violationsByCategory = new HashMap<>();

        for (Violation violation : result.getViolations()) {
            String category = violation.getCategory();
            violationsByCategory.put(category, violationsByCategory.getOrDefault(category, 0) + 1);
            totalByCategory.put(category, totalByCategory.getOrDefault(category, 0) + 1);
        }

        Map<String, Double> scores = new HashMap<>();
        for (String category : totalByCategory.keySet()) {
            int total = totalByCategory.get(category);
            int violations = violationsByCategory.getOrDefault(category, 0);
            double score = total > 0 ? ((total - violations) * 100.0 / total) : 100.0;
            scores.put(category, score);
        }

        result.setConsistencyScores(scores);
    }

    // DTOs

    public static class ValidationOptions {
        public boolean autoDetect = true;
        public String severity = "WARN"; // ERROR, WARN, INFO
        public Map<String, Object> rules = new HashMap<>();

        public ValidationOptions() {
            // Default rules
            Map<String, String> naming = new HashMap<>();
            naming.put("methods", "camelCase");
            naming.put("classes", "PascalCase");
            naming.put("constants", "UPPER_SNAKE_CASE");
            rules.put("naming", naming);
        }
    }

    public static class ValidationResult {
        private final List<Violation> violations = new ArrayList<>();
        private Map<String, Double> consistencyScores = new HashMap<>();

        public void addViolation(Violation violation) {
            violations.add(violation);
        }

        public List<Violation> getViolations() {
            return new ArrayList<>(violations);
        }

        public void setConsistencyScores(Map<String, Double> scores) {
            this.consistencyScores = scores;
        }

        public Map<String, Double> getConsistencyScores() {
            return new HashMap<>(consistencyScores);
        }

        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalViolations", violations.size());

            long errors = violations.stream().filter(v -> "ERROR".equals(v.getSeverity())).count();
            long warnings = violations.stream().filter(v -> "WARN".equals(v.getSeverity())).count();
            long infos = violations.stream().filter(v -> "INFO".equals(v.getSeverity())).count();

            summary.put("errors", errors);
            summary.put("warnings", warnings);
            summary.put("infos", infos);
            summary.put("consistencyScores", consistencyScores);

            return summary;
        }

        public List<Violation> filterBySeverity(String severity) {
            return violations.stream()
                    .filter(v -> v.getSeverity().equals(severity))
                    .toList();
        }
    }

    public static class Violation {
        private final String category;
        private final String message;
        private final String file;
        private final int line;
        private final String severity;

        public Violation(String category, String message, String file, int line, String severity) {
            this.category = category;
            this.message = message;
            this.file = file;
            this.line = line;
            this.severity = severity;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public String getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s:%d - %s: %s", severity, file, line, category, message);
        }
    }
}
