package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.analyzers.*;
import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Architecture analysis tool for MCP.
 * Analyzes project architecture, complexity, and frameworks.
 */
public class ArchitectureAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ArchitectureAnalyzer.class);

    private final FileScanner fileScanner;
    private final JavaSourceParser javaParser;
    private final ComplexityAnalyzer complexityAnalyzer;
    private final FrameworkDetector frameworkDetector;
    private final DiagramGenerator diagramGenerator;

    // Framework analyzers
    private final SpringFrameworkAnalyzer springAnalyzer;
    private final SpringBootAnalyzer springBootAnalyzer;
    private final JpaAnalyzer jpaAnalyzer;
    private final SecurityAnalyzer securityAnalyzer;
    private final AopAnalyzer aopAnalyzer;

    public ArchitectureAnalyzer() {
        this.fileScanner = new FileScanner();
        this.javaParser = new JavaSourceParser();
        this.complexityAnalyzer = new ComplexityAnalyzer();
        this.frameworkDetector = new FrameworkDetector();
        this.diagramGenerator = new DiagramGenerator();
        this.springAnalyzer = new SpringFrameworkAnalyzer();
        this.springBootAnalyzer = new SpringBootAnalyzer();
        this.jpaAnalyzer = new JpaAnalyzer();
        this.securityAnalyzer = new SecurityAnalyzer();
        this.aopAnalyzer = new AopAnalyzer();
    }

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

        // Scan for files
        FileScanner.ScanResult scan = fileScanner.scanDirectory(
                projectRoot,
                options.includeGlobs,
                options.excludeGlobs);

        result.setTotalFiles(scan.getTotalJavaFiles());

        // Detect frameworks
        FrameworkDetector.DetectionResult frameworks = frameworkDetector.detectFrameworks(scan);
        result.setFrameworks(frameworks.getDetectedFrameworks());

        // Analyze Java files
        List<JavaFileInfo> javaFiles = new ArrayList<>();
        for (Path javaFile : scan.getJavaFiles()) {
            JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
            if (fileInfo != null) {
                // Calculate complexity
                for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
                    for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                        int complexity = complexityAnalyzer.calculateComplexity(method);
                        method.setComplexity(complexity);
                    }
                }
                javaFiles.add(fileInfo);
            }
        }

        // Calculate metrics
        result.setMetrics(calculateMetrics(javaFiles));

        // Analyze Spring Boot if detected
        if (frameworks.isSpringBootDetected()) {
            SpringBootAnalyzer.SpringBootAnalysisResult bootResult =
                    springBootAnalyzer.analyze(scan.getJavaFiles(), scan.getPomXmlFile(), scan.getConfigFiles());
            result.setSpringBootResult(bootResult);
        }

        // Analyze traditional Spring if detected
        if (frameworks.isSpringFrameworkDetected()) {
            SpringFrameworkAnalyzer.SpringAnalysisResult springResult =
                    springAnalyzer.analyze(scan.getXmlFiles());
            result.setSpringResult(springResult);
        }

        // Analyze JPA
        JpaAnalyzer.JpaAnalysisResult jpaResult = jpaAnalyzer.analyze(scan.getJavaFiles());
        if (jpaResult.getEntityCount() > 0) {
            result.setJpaResult(jpaResult);
        }

        // Analyze Security
        SecurityAnalyzer.SecurityAnalysisResult securityResult =
                securityAnalyzer.analyze(scan.getJavaFiles(), scan.getXmlFiles());
        if (securityResult.isSecurityEnabled()) {
            result.setSecurityResult(securityResult);
        }

        // Analyze AOP
        AopAnalyzer.AopAnalysisResult aopResult =
                aopAnalyzer.analyze(scan.getJavaFiles(), scan.getXmlFiles());
        if (aopResult.isAopEnabled()) {
            result.setAopResult(aopResult);
        }

        // Generate diagrams if requested
        if (options.generateDiagrams) {
            result.setArchitectureDiagram(diagramGenerator.generateArchitectureDiagram(javaFiles));
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
        private Set<String> frameworks = new HashSet<>();
        private ProjectMetrics metrics;
        private String architectureDiagram;

        // Framework-specific results
        private SpringBootAnalyzer.SpringBootAnalysisResult springBootResult;
        private SpringFrameworkAnalyzer.SpringAnalysisResult springResult;
        private JpaAnalyzer.JpaAnalysisResult jpaResult;
        private SecurityAnalyzer.SecurityAnalysisResult securityResult;
        private AopAnalyzer.AopAnalysisResult aopResult;

        // Getters and setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public Set<String> getFrameworks() { return new HashSet<>(frameworks); }
        public void setFrameworks(Set<String> frameworks) { this.frameworks = frameworks; }

        public ProjectMetrics getMetrics() { return metrics; }
        public void setMetrics(ProjectMetrics metrics) { this.metrics = metrics; }

        public String getArchitectureDiagram() { return architectureDiagram; }
        public void setArchitectureDiagram(String diagram) { this.architectureDiagram = diagram; }

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
