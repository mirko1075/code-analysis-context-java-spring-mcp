package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.analyzers.*;
import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Architecture analysis tool for MCP.
 * Analyzes project architecture, complexity, and frameworks.
 */
public class ArchitectureAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ArchitectureAnalyzer.class);

    /**
     * Analyze project architecture.
     *
     * @param projectPath Path to project root
     * @param options Analysis options
     * @return Analysis result
     */
    public AnalysisResult analyze(String projectPath, AnalysisOptions options) {
        Path projectRoot = Paths.get(projectPath);
        AnalysisResult result = new AnalysisResult();

        try {
            // Initialize scanner and parsers
            FileScanner fileScanner = new FileScanner(projectRoot);
            JavaSourceParser javaParser = new JavaSourceParser();

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

            result.setTotalFiles(javaFiles.size());

            // Detect frameworks
            FrameworkDetector frameworkDetector = new FrameworkDetector(projectRoot);
            FrameworkDetector.FrameworkInfo frameworkInfo = frameworkDetector.detect();
            result.setFrameworkInfo(frameworkInfo);

            // Analyze Java files (complexity is calculated during parsing)
            List<JavaFileInfo> parsedJavaFiles = new ArrayList<>();
            for (Path javaFile : javaFiles) {
                JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
                if (fileInfo != null) {
                    parsedJavaFiles.add(fileInfo);
                }
            }

            // Calculate metrics
            result.setMetrics(calculateMetrics(parsedJavaFiles));

            // Framework analyzers
            SpringBootAnalyzer springBootAnalyzer = new SpringBootAnalyzer();
            SpringFrameworkAnalyzer springAnalyzer = new SpringFrameworkAnalyzer();
            JpaAnalyzer jpaAnalyzer = new JpaAnalyzer();
            SecurityAnalyzer securityAnalyzer = new SecurityAnalyzer();
            AopAnalyzer aopAnalyzer = new AopAnalyzer();

            // Analyze Spring Boot if detected
            if (frameworkInfo.isSpringBoot()) {
                SpringBootAnalyzer.SpringBootAnalysisResult bootResult =
                        springBootAnalyzer.analyze(javaFiles, pomFile, configFiles);
                result.setSpringBootResult(bootResult);
            }

            // Analyze traditional Spring if detected
            if (frameworkInfo.isTraditionalSpring()) {
                SpringFrameworkAnalyzer.SpringAnalysisResult springResult =
                        springAnalyzer.analyze(xmlFiles);
                result.setSpringResult(springResult);
            }

            // Analyze JPA
            JpaAnalyzer.JpaAnalysisResult jpaResult = jpaAnalyzer.analyze(javaFiles);
            if (jpaResult.getEntityCount() > 0) {
                result.setJpaResult(jpaResult);
            }

            // Analyze Security
            SecurityAnalyzer.SecurityAnalysisResult securityResult =
                    securityAnalyzer.analyze(javaFiles, xmlFiles);
            if (securityResult.isSecurityEnabled()) {
                result.setSecurityResult(securityResult);
            }

            // Analyze AOP
            AopAnalyzer.AopAnalysisResult aopResult =
                    aopAnalyzer.analyze(javaFiles, xmlFiles);
            if (aopResult.isAopEnabled()) {
                result.setAopResult(aopResult);
            }

            // Generate diagrams if requested
            if (options.generateDiagrams) {
                DiagramGenerator diagramGenerator = new DiagramGenerator();

                // Generate architecture diagram from package layers
                Map<String, List<String>> packageLayers = extractPackageLayers(parsedJavaFiles);
                if (!packageLayers.isEmpty()) {
                    result.setArchitectureDiagram(diagramGenerator.generateArchitectureDiagram(packageLayers));
                }

                // Generate complexity heatmap
                Map<String, Integer> complexities = extractComplexities(parsedJavaFiles);
                if (!complexities.isEmpty()) {
                    result.setComplexityHeatmap(diagramGenerator.generateComplexityHeatmap(complexities));
                }
            }

        } catch (IOException e) {
            logger.error("Error analyzing project: {}", projectPath, e);
        }

        return result;
    }

    /**
     * Calculate project metrics.
     */
    private ProjectMetrics calculateMetrics(List<JavaFileInfo> javaFiles) {
        ProjectMetrics metrics = new ProjectMetrics();

        int totalClasses = 0;
        int totalMethods = 0;
        int totalLines = 0;
        int totalCodeLines = 0;
        int complexitySum = 0;
        int maxComplexity = 0;

        for (JavaFileInfo file : javaFiles) {
            totalLines += file.getTotalLines();
            totalCodeLines += file.getCodeLines();

            for (JavaFileInfo.ClassInfo classInfo : file.getClasses()) {
                totalClasses++;
                for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                    totalMethods++;
                    int complexity = method.getComplexity();
                    complexitySum += complexity;
                    maxComplexity = Math.max(maxComplexity, complexity);
                }
            }
        }

        metrics.setTotalClasses(totalClasses);
        metrics.setTotalMethods(totalMethods);
        metrics.setTotalLines(totalLines);
        metrics.setCodeLines(totalCodeLines);
        metrics.setAverageComplexity(totalMethods > 0 ? (double) complexitySum / totalMethods : 0);
        metrics.setMaxComplexity(maxComplexity);

        return metrics;
    }

    /**
     * Extract package layers from Java files.
     */
    private Map<String, List<String>> extractPackageLayers(List<JavaFileInfo> javaFiles) {
        Map<String, List<String>> layers = new HashMap<>();

        for (JavaFileInfo file : javaFiles) {
            String packageName = file.getPackageName();
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }

            // Determine layer from package name
            String layer = determineLayer(packageName);
            layers.computeIfAbsent(layer, k -> new ArrayList<>());
            if (!layers.get(layer).contains(packageName)) {
                layers.get(layer).add(packageName);
            }
        }

        return layers;
    }

    /**
     * Determine architectural layer from package name.
     */
    private String determineLayer(String packageName) {
        String lowerPackage = packageName.toLowerCase();

        if (lowerPackage.contains("controller") || lowerPackage.contains("rest") || lowerPackage.contains("api")) {
            return "controller";
        } else if (lowerPackage.contains("service") || lowerPackage.contains("business")) {
            return "service";
        } else if (lowerPackage.contains("repository") || lowerPackage.contains("dao") || lowerPackage.contains("data")) {
            return "repository";
        } else if (lowerPackage.contains("entity") || lowerPackage.contains("model") || lowerPackage.contains("domain")) {
            return "model";
        } else if (lowerPackage.contains("config")) {
            return "config";
        } else if (lowerPackage.contains("util")) {
            return "util";
        } else {
            return "other";
        }
    }

    /**
     * Extract method complexities for heatmap.
     */
    private Map<String, Integer> extractComplexities(List<JavaFileInfo> javaFiles) {
        Map<String, Integer> complexities = new HashMap<>();

        for (JavaFileInfo file : javaFiles) {
            for (JavaFileInfo.ClassInfo classInfo : file.getClasses()) {
                for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                    if (method.getComplexity() > 5) { // Only include methods with complexity > 5
                        String key = classInfo.getName() + "." + method.getName();
                        complexities.put(key, method.getComplexity());
                    }
                }
            }
        }

        return complexities;
    }

    // DTOs

    public static class AnalysisOptions {
        public List<String> includeGlobs = List.of("**/*.java");
        public List<String> excludeGlobs = List.of("**/target/**", "**/build/**", "**/*Test.java");
        public boolean generateDiagrams = true;
        public boolean includeMetrics = true;
        public int minComplexity = 0;
        public int maxFiles = 1000;
    }

    public static class AnalysisResult {
        private int totalFiles;
        private FrameworkDetector.FrameworkInfo frameworkInfo;
        private ProjectMetrics metrics;
        private String architectureDiagram;
        private String complexityHeatmap;

        // Framework-specific results
        private SpringBootAnalyzer.SpringBootAnalysisResult springBootResult;
        private SpringFrameworkAnalyzer.SpringAnalysisResult springResult;
        private JpaAnalyzer.JpaAnalysisResult jpaResult;
        private SecurityAnalyzer.SecurityAnalysisResult securityResult;
        private AopAnalyzer.AopAnalysisResult aopResult;

        // Getters and setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public FrameworkDetector.FrameworkInfo getFrameworkInfo() { return frameworkInfo; }
        public void setFrameworkInfo(FrameworkDetector.FrameworkInfo info) { this.frameworkInfo = info; }

        public ProjectMetrics getMetrics() { return metrics; }
        public void setMetrics(ProjectMetrics metrics) { this.metrics = metrics; }

        public String getArchitectureDiagram() { return architectureDiagram; }
        public void setArchitectureDiagram(String diagram) { this.architectureDiagram = diagram; }

        public String getComplexityHeatmap() { return complexityHeatmap; }
        public void setComplexityHeatmap(String heatmap) { this.complexityHeatmap = heatmap; }

        public SpringBootAnalyzer.SpringBootAnalysisResult getSpringBootResult() { return springBootResult; }
        public void setSpringBootResult(SpringBootAnalyzer.SpringBootAnalysisResult result) { this.springBootResult = result; }

        public SpringFrameworkAnalyzer.SpringAnalysisResult getSpringResult() { return springResult; }
        public void setSpringResult(SpringFrameworkAnalyzer.SpringAnalysisResult result) { this.springResult = result; }

        public JpaAnalyzer.JpaAnalysisResult getJpaResult() { return jpaResult; }
        public void setJpaResult(JpaAnalyzer.JpaAnalysisResult result) { this.jpaResult = result; }

        public SecurityAnalyzer.SecurityAnalysisResult getSecurityResult() { return securityResult; }
        public void setSecurityResult(SecurityAnalyzer.SecurityAnalysisResult result) { this.securityResult = result; }

        public AopAnalyzer.AopAnalysisResult getAopResult() { return aopResult; }
        public void setAopResult(AopAnalyzer.AopAnalysisResult result) { this.aopResult = result; }
    }

    public static class ProjectMetrics {
        private int totalClasses;
        private int totalMethods;
        private int totalLines;
        private int codeLines;
        private double averageComplexity;
        private int maxComplexity;

        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }

        public int getTotalLines() { return totalLines; }
        public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

        public int getCodeLines() { return codeLines; }
        public void setCodeLines(int codeLines) { this.codeLines = codeLines; }

        public double getAverageComplexity() { return averageComplexity; }
        public void setAverageComplexity(double avg) { this.averageComplexity = avg; }

        public int getMaxComplexity() { return maxComplexity; }
        public void setMaxComplexity(int max) { this.maxComplexity = max; }
    }
}
