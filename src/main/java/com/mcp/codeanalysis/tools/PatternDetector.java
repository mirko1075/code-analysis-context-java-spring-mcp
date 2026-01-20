package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.analyzers.*;
import com.mcp.codeanalysis.utils.FileScanner;
import com.mcp.codeanalysis.utils.FrameworkDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Pattern detection tool for MCP.
 * Detects Spring and enterprise patterns, antipatterns, and provides recommendations.
 */
public class PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(PatternDetector.class);

    /**
     * Analyze project for patterns.
     *
     * @param projectPath Path to project root
     * @param options Pattern detection options
     * @return Pattern detection result
     */
    public PatternDetectionResult analyze(String projectPath, PatternOptions options) {
        Path projectRoot = Paths.get(projectPath);
        PatternDetectionResult result = new PatternDetectionResult();

        try {
            // Initialize scanner
            FileScanner fileScanner = new FileScanner(projectRoot);

            // Scan for files
            List<Path> javaFiles = fileScanner.scanJavaFiles();
            List<Path> xmlFiles = fileScanner.scanXmlFiles();
            List<Path> configFiles = new ArrayList<>();
            configFiles.addAll(fileScanner.scanYamlFiles());
            configFiles.addAll(fileScanner.scanPropertiesFiles());

            Path pomFile = null;
            List<Path> pomFiles = fileScanner.findPomFiles();
            if (!pomFiles.isEmpty()) {
                pomFile = pomFiles.get(0);
            }

            // Detect frameworks
            FrameworkDetector frameworkDetector = new FrameworkDetector(projectRoot);
            FrameworkDetector.FrameworkInfo frameworkInfo = frameworkDetector.detect();

            // Run pattern analyzers based on detected frameworks and requested types
            if (shouldAnalyze("spring-boot", options.patterns, frameworkInfo)) {
                SpringBootAnalyzer bootAnalyzer = new SpringBootAnalyzer();
                SpringBootAnalyzer.SpringBootAnalysisResult bootResult =
                        bootAnalyzer.analyze(javaFiles, pomFile, configFiles);
                analyzeSpringBootPatterns(bootResult, result);
            }

            if (shouldAnalyze("spring-mvc", options.patterns, frameworkInfo)) {
                SpringFrameworkAnalyzer springAnalyzer = new SpringFrameworkAnalyzer();
                SpringFrameworkAnalyzer.SpringAnalysisResult springResult =
                        springAnalyzer.analyze(xmlFiles);
                analyzeSpringMvcPatterns(springResult, result);
            }

            if (shouldAnalyze("jpa", options.patterns, frameworkInfo)) {
                JpaAnalyzer jpaAnalyzer = new JpaAnalyzer();
                JpaAnalyzer.JpaAnalysisResult jpaResult = jpaAnalyzer.analyze(javaFiles);
                analyzeJpaPatterns(jpaResult, result);
            }

            if (shouldAnalyze("security", options.patterns, frameworkInfo)) {
                SecurityAnalyzer securityAnalyzer = new SecurityAnalyzer();
                SecurityAnalyzer.SecurityAnalysisResult securityResult =
                        securityAnalyzer.analyze(javaFiles, xmlFiles);
                analyzeSecurityPatterns(securityResult, result);
            }

            if (shouldAnalyze("aop", options.patterns, frameworkInfo)) {
                AopAnalyzer aopAnalyzer = new AopAnalyzer();
                AopAnalyzer.AopAnalysisResult aopResult =
                        aopAnalyzer.analyze(javaFiles, xmlFiles);
                analyzeAopPatterns(aopResult, result);
            }

            // Generate recommendations if requested
            if (options.generateRecommendations) {
                generateRecommendations(result, frameworkInfo);
            }

        } catch (IOException e) {
            logger.error("Error detecting patterns: {}", projectPath, e);
        }

        return result;
    }

    /**
     * Check if pattern type should be analyzed.
     */
    private boolean shouldAnalyze(String patternType, List<String> requestedPatterns,
                                  FrameworkDetector.FrameworkInfo frameworkInfo) {
        if (requestedPatterns.isEmpty()) {
            // Auto-detect based on framework
            return switch (patternType) {
                case "spring-boot" -> frameworkInfo.isSpringBoot();
                case "spring-mvc" -> frameworkInfo.isSpringMvc();
                case "jpa" -> frameworkInfo.isSpringData();
                case "security" -> frameworkInfo.isSpringSecurity();
                case "aop" -> frameworkInfo.isSpringAop();
                default -> false;
            };
        } else {
            return requestedPatterns.contains(patternType);
        }
    }

    /**
     * Analyze Spring Boot patterns.
     */
    private void analyzeSpringBootPatterns(SpringBootAnalyzer.SpringBootAnalysisResult bootResult,
                                          PatternDetectionResult result) {
        if (!bootResult.isSpringBootDetected()) {
            return;
        }

        result.addPattern("Spring Boot Application", "Detected @SpringBootApplication: " + bootResult.getMainClass());
        result.addPattern("Spring Boot Starters", "Using starters: " + String.join(", ", bootResult.getStarters()));

        for (String feature : bootResult.getFeatures()) {
            result.addPattern("Spring Boot Feature", feature);
        }

        if (bootResult.getServerPort() > 0) {
            result.addPattern("Server Configuration", "Running on port " + bootResult.getServerPort());
        }

        if (!bootResult.getActiveProfiles().isEmpty()) {
            result.addPattern("Spring Profiles", "Active profiles: " + String.join(", ", bootResult.getActiveProfiles()));
        }
    }

    /**
     * Analyze Spring MVC patterns.
     */
    private void analyzeSpringMvcPatterns(SpringFrameworkAnalyzer.SpringAnalysisResult springResult,
                                         PatternDetectionResult result) {
        if (!springResult.isMvcEnabled()) {
            return;
        }

        result.addPattern("Spring MVC", "Detected Spring MVC configuration");

        // Get all MVC components and categorize them
        List<String> mvcComponents = springResult.getMvcComponents();
        long controllerCount = mvcComponents.stream()
                .filter(c -> c.contains("Controller"))
                .count();
        long viewResolverCount = mvcComponents.stream()
                .filter(c -> c.contains("ViewResolver"))
                .count();

        if (controllerCount > 0) {
            result.addPattern("MVC Controllers", "Found " + controllerCount + " controller(s)");
        }

        if (viewResolverCount > 0) {
            result.addPattern("View Resolver", "Using " + viewResolverCount + " view resolver(s)");
        }
    }

    /**
     * Analyze JPA patterns and detect antipatterns.
     */
    private void analyzeJpaPatterns(JpaAnalyzer.JpaAnalysisResult jpaResult,
                                   PatternDetectionResult result) {
        if (jpaResult.getEntityCount() == 0) {
            return;
        }

        result.addPattern("JPA Entities", "Found " + jpaResult.getEntityCount() + " entities");
        result.addPattern("JPA Relationships", "Found " + jpaResult.getRelationshipCount() + " relationships");

        // Detect N+1 query antipattern
        List<String> nPlusOneQueries = jpaResult.getAllPotentialNPlusOneQueries();
        if (!nPlusOneQueries.isEmpty()) {
            for (String nPlusOne : nPlusOneQueries) {
                result.addAntipattern("Potential N+1 Query", nPlusOne);
                result.addRecommendation("Use @BatchSize or JOIN FETCH for relationship: " + nPlusOne);
            }
        }

        // Detect entities with issues
        for (JpaAnalyzer.EntityInfo entityInfo : jpaResult.getEntitiesWithIssues()) {
            for (String issue : entityInfo.getIssues()) {
                result.addAntipattern("Entity Issue", entityInfo.getClassName() + ": " + issue);
            }
        }
    }

    /**
     * Analyze Security patterns.
     */
    private void analyzeSecurityPatterns(SecurityAnalyzer.SecurityAnalysisResult securityResult,
                                        PatternDetectionResult result) {
        if (!securityResult.isSecurityEnabled()) {
            return;
        }

        result.addPattern("Spring Security", "Security is enabled");

        for (String mechanism : securityResult.getAuthenticationMechanisms()) {
            result.addPattern("Authentication Mechanism", mechanism);
        }

        if (!securityResult.getSecuredMethods().isEmpty()) {
            result.addPattern("Method Security", "Secured " + securityResult.getSecuredMethods().size() + " methods");
        }

        if (!securityResult.getUserDetailsServices().isEmpty()) {
            result.addPattern("User Details Service", "Custom user details services: " +
                    securityResult.getUserDetailsServices().size());
        }
    }

    /**
     * Analyze AOP patterns.
     */
    private void analyzeAopPatterns(AopAnalyzer.AopAnalysisResult aopResult,
                                   PatternDetectionResult result) {
        if (!aopResult.isAopEnabled()) {
            return;
        }

        result.addPattern("Spring AOP", "AOP is enabled");
        result.addPattern("Aspects", "Found " + aopResult.getAspectCount() + " aspects");
        result.addPattern("Advice", "Found " + aopResult.getTotalAdviceCount() + " advice");

        Map<String, Integer> breakdown = aopResult.getAdviceTypeBreakdown();
        for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
            result.addPattern("Advice Type", entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Generate recommendations based on detected patterns.
     */
    private void generateRecommendations(PatternDetectionResult result,
                                        FrameworkDetector.FrameworkInfo frameworkInfo) {
        // Spring Boot recommendations
        if (frameworkInfo.isSpringBoot()) {
            result.addRecommendation("Use Spring Boot DevTools for hot reload during development");
            result.addRecommendation("Configure Actuator endpoints for production monitoring");
        }

        // Transaction recommendations
        if (frameworkInfo.isSpringData() && frameworkInfo.isSpringTransactions()) {
            result.addRecommendation("Ensure @Transactional is placed on service layer methods, not controllers");
        }

        // Security recommendations
        if (frameworkInfo.isSpringSecurity()) {
            result.addRecommendation("Use BCryptPasswordEncoder for password hashing");
            result.addRecommendation("Enable CSRF protection for web applications");
        }
    }

    // DTOs

    public static class PatternOptions {
        public List<String> patterns = new ArrayList<>(); // Empty = auto-detect
        public boolean generateRecommendations = true;
        public boolean detectAntipatterns = true;
    }

    public static class PatternDetectionResult {
        private final Map<String, List<String>> patterns = new HashMap<>();
        private final Map<String, List<String>> antipatterns = new HashMap<>();
        private final List<String> recommendations = new ArrayList<>();

        public void addPattern(String category, String pattern) {
            patterns.computeIfAbsent(category, k -> new ArrayList<>()).add(pattern);
        }

        public void addAntipattern(String category, String antipattern) {
            antipatterns.computeIfAbsent(category, k -> new ArrayList<>()).add(antipattern);
        }

        public void addRecommendation(String recommendation) {
            recommendations.add(recommendation);
        }

        public Map<String, List<String>> getPatterns() {
            return new HashMap<>(patterns);
        }

        public Map<String, List<String>> getAntipatterns() {
            return new HashMap<>(antipatterns);
        }

        public List<String> getRecommendations() {
            return new ArrayList<>(recommendations);
        }

        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalPatterns", patterns.values().stream().mapToInt(List::size).sum());
            summary.put("totalAntipatterns", antipatterns.values().stream().mapToInt(List::size).sum());
            summary.put("totalRecommendations", recommendations.size());
            return summary;
        }
    }
}
