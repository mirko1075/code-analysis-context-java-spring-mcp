package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.ComplexityAnalyzer;
import com.mcp.codeanalysis.utils.FileScanner;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Coverage analysis tool for MCP.
 * Parses JaCoCo reports, identifies untested code, and generates test scaffolds.
 */
public class CoverageAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CoverageAnalyzer.class);

    /**
     * Analyze test coverage and generate test scaffolds.
     *
     * @param projectPath Path to project root
     * @param options Coverage analysis options
     * @return Coverage analysis result
     */
    public CoverageAnalysisResult analyze(String projectPath, CoverageOptions options) {
        Path projectRoot = Paths.get(projectPath);
        CoverageAnalysisResult result = new CoverageAnalysisResult();

        try {
            // Parse coverage report if provided
            if (options.reportPath != null && !options.reportPath.isEmpty()) {
                Path reportPath = Paths.get(options.reportPath);
                if (!reportPath.isAbsolute()) {
                    reportPath = projectRoot.resolve(reportPath);
                }

                if (Files.exists(reportPath)) {
                    parseCoverageReport(reportPath, result);
                } else {
                    logger.warn("Coverage report not found: {}", reportPath);
                }
            }

            // Scan for Java files to analyze
            FileScanner fileScanner = new FileScanner(projectRoot);
            List<Path> javaFiles = fileScanner.scanJavaFiles();

            // Analyze source files for complexity
            JavaSourceParser javaParser = new JavaSourceParser();
            Map<String, JavaFileInfo> fileInfoMap = new HashMap<>();

            for (Path javaFile : javaFiles) {
                JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
                if (fileInfo != null) {
                    String className = getClassNameFromPath(projectRoot, javaFile);
                    fileInfoMap.put(className, fileInfo);
                }
            }

            // Find coverage gaps and prioritize
            findCoverageGaps(fileInfoMap, result, options);

            // Generate test scaffolds if requested
            if (options.generateTests) {
                generateTestScaffolds(result, fileInfoMap, options);
            }

        } catch (Exception e) {
            logger.error("Error analyzing coverage: {}", projectPath, e);
        }

        return result;
    }

    /**
     * Parse JaCoCo XML coverage report.
     */
    private void parseCoverageReport(Path reportPath, CoverageAnalysisResult result) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(reportPath.toFile());
            Element root = document.getRootElement();

            // Parse package elements
            List<Element> packages = root.elements("package");
            for (Element packageElement : packages) {
                String packageName = packageElement.attributeValue("name").replace('/', '.');

                // Parse class elements
                List<Element> classes = packageElement.elements("class");
                for (Element classElement : classes) {
                    String className = classElement.attributeValue("name").replace('/', '.');
                    parseClassCoverage(classElement, className, result);
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing coverage report: {}", reportPath, e);
        }
    }

    /**
     * Parse coverage for a single class.
     */
    private void parseClassCoverage(Element classElement, String className, CoverageAnalysisResult result) {
        // Parse method coverage
        List<Element> methods = classElement.elements("method");
        for (Element method : methods) {
            String methodName = method.attributeValue("name");
            String methodDesc = method.attributeValue("desc");

            // Parse line coverage counter
            Element lineCounter = null;
            List<Element> counters = method.elements("counter");
            for (Element counter : counters) {
                if ("LINE".equals(counter.attributeValue("type"))) {
                    lineCounter = counter;
                    break;
                }
            }

            if (lineCounter != null) {
                int covered = Integer.parseInt(lineCounter.attributeValue("covered"));
                int missed = Integer.parseInt(lineCounter.attributeValue("missed"));

                if (missed > 0) {
                    // Method has coverage gaps
                    CoverageGap gap = new CoverageGap(className, methodName, missed, covered);
                    result.addCoverageGap(gap);
                }
            }
        }
    }

    /**
     * Find coverage gaps based on source files.
     */
    private void findCoverageGaps(Map<String, JavaFileInfo> fileInfoMap,
                                  CoverageAnalysisResult result,
                                  CoverageOptions options) {
        for (Map.Entry<String, JavaFileInfo> entry : fileInfoMap.entrySet()) {
            String className = entry.getKey();
            JavaFileInfo fileInfo = entry.getValue();

            for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
                // Skip test classes
                if (className.contains("Test") || className.contains("IT")) {
                    continue;
                }

                for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                    // Check if method is already in coverage gaps
                    boolean hasGap = result.getCoverageGaps().stream()
                            .anyMatch(g -> g.getClassName().equals(className) &&
                                          g.getMethodName().equals(method.getName()));

                    if (!hasGap && !isConstructor(method, classInfo)) {
                        // Method not covered - add as gap
                        CoverageGap gap = new CoverageGap(className, method.getName(), 0, 0);
                        gap.setComplexity(method.getComplexity());
                        result.addCoverageGap(gap);
                    }
                }
            }
        }

        // Prioritize gaps based on complexity and threshold
        prioritizeGaps(result, options);
    }

    /**
     * Prioritize coverage gaps.
     */
    private void prioritizeGaps(CoverageAnalysisResult result, CoverageOptions options) {
        for (CoverageGap gap : result.getCoverageGaps()) {
            int complexity = gap.getComplexity();

            if (complexity >= 15 || gap.getClassName().contains("Controller") ||
                gap.getClassName().contains("Service")) {
                gap.setPriority("CRITICAL");
            } else if (complexity >= 10) {
                gap.setPriority("HIGH");
            } else if (complexity >= 5) {
                gap.setPriority("MEDIUM");
            } else {
                gap.setPriority("LOW");
            }
        }

        // Filter by priority if specified
        if (options.priority != null && !options.priority.equals("all")) {
            result.filterByPriority(options.priority.toUpperCase());
        }
    }

    /**
     * Generate test scaffolds for coverage gaps.
     */
    private void generateTestScaffolds(CoverageAnalysisResult result,
                                      Map<String, JavaFileInfo> fileInfoMap,
                                      CoverageOptions options) {
        for (CoverageGap gap : result.getCoverageGaps()) {
            String testScaffold = generateTestScaffold(gap, fileInfoMap, options);
            gap.setTestScaffold(testScaffold);
        }
    }

    /**
     * Generate JUnit test scaffold for a coverage gap.
     */
    private String generateTestScaffold(CoverageGap gap,
                                       Map<String, JavaFileInfo> fileInfoMap,
                                       CoverageOptions options) {
        StringBuilder scaffold = new StringBuilder();
        String className = gap.getClassName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String testClassName = simpleClassName + "Test";

        // Determine test type based on class annotations
        String testAnnotation = "@Test";
        boolean isController = className.contains("Controller");
        boolean isService = className.contains("Service");
        boolean isRepository = className.contains("Repository");

        // Package declaration
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            scaffold.append("package ").append(className.substring(0, lastDot)).append(";\n\n");
        }

        // Imports
        scaffold.append("import org.junit.jupiter.api.Test;\n");
        scaffold.append("import org.junit.jupiter.api.BeforeEach;\n");

        if (isController) {
            scaffold.append("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;\n");
            scaffold.append("import org.springframework.test.web.servlet.MockMvc;\n");
            scaffold.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        } else if (isService || isRepository) {
            scaffold.append("import org.springframework.boot.test.context.SpringBootTest;\n");
        }

        scaffold.append("import org.mockito.Mock;\n");
        scaffold.append("import org.mockito.junit.jupiter.MockitoExtension;\n");
        scaffold.append("import org.junit.jupiter.api.extension.ExtendWith;\n");
        scaffold.append("\nimport static org.junit.jupiter.api.Assertions.*;\n");
        scaffold.append("import static org.mockito.Mockito.*;\n\n");

        // Class declaration
        scaffold.append("@ExtendWith(MockitoExtension.class)\n");
        if (isController) {
            scaffold.append("@WebMvcTest(").append(simpleClassName).append(".class)\n");
        } else if (isService || isRepository) {
            scaffold.append("@SpringBootTest\n");
        }

        scaffold.append("public class ").append(testClassName).append(" {\n\n");

        // Mock dependencies
        if (isController) {
            scaffold.append("    @Autowired\n");
            scaffold.append("    private MockMvc mockMvc;\n\n");
        }

        scaffold.append("    @Mock\n");
        scaffold.append("    private ").append(simpleClassName).append(" ").append(uncapitalize(simpleClassName)).append(";\n\n");

        // Setup method
        scaffold.append("    @BeforeEach\n");
        scaffold.append("    void setUp() {\n");
        scaffold.append("        // Initialize test dependencies\n");
        scaffold.append("    }\n\n");

        // Test method for the uncovered method
        scaffold.append("    @Test\n");
        scaffold.append("    void test").append(capitalize(gap.getMethodName())).append("() {\n");
        scaffold.append("        // TODO: Implement test for ").append(gap.getMethodName()).append("\n");
        scaffold.append("        // Arrange\n\n");
        scaffold.append("        // Act\n\n");
        scaffold.append("        // Assert\n");
        scaffold.append("        fail(\"Test not implemented\");\n");
        scaffold.append("    }\n");

        scaffold.append("}\n");

        return scaffold.toString();
    }

    /**
     * Check if method is a constructor.
     */
    private boolean isConstructor(JavaFileInfo.MethodInfo method, JavaFileInfo.ClassInfo classInfo) {
        return method.getName().equals(classInfo.getName());
    }

    /**
     * Get class name from file path.
     */
    private String getClassNameFromPath(Path projectRoot, Path javaFile) {
        Path relativePath = projectRoot.relativize(javaFile);
        String pathStr = relativePath.toString();

        // Remove src/main/java/ or src/test/java/ prefix
        pathStr = pathStr.replaceFirst("src[/\\\\]main[/\\\\]java[/\\\\]", "");
        pathStr = pathStr.replaceFirst("src[/\\\\]test[/\\\\]java[/\\\\]", "");

        // Remove .java extension
        pathStr = pathStr.replaceFirst("\\.java$", "");

        // Convert path separators to dots
        return pathStr.replace('/', '.').replace('\\', '.');
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    // DTOs

    public static class CoverageOptions {
        public String reportPath = "target/site/jacoco/jacoco.xml";
        public String framework = "junit5";
        public Map<String, Integer> threshold = new HashMap<>();
        public String priority = "all"; // crit, high, med, low, all
        public boolean generateTests = true;
        public boolean analyzeComplexity = true;

        public CoverageOptions() {
            threshold.put("lines", 80);
            threshold.put("methods", 80);
            threshold.put("branches", 75);
        }
    }

    public static class CoverageAnalysisResult {
        private final List<CoverageGap> coverageGaps = new ArrayList<>();
        private final Map<String, Object> summary = new HashMap<>();

        public void addCoverageGap(CoverageGap gap) {
            coverageGaps.add(gap);
        }

        public List<CoverageGap> getCoverageGaps() {
            return new ArrayList<>(coverageGaps);
        }

        public void filterByPriority(String priority) {
            coverageGaps.removeIf(gap -> !gap.getPriority().equals(priority));
        }

        public Map<String, Object> getSummary() {
            Map<String, Object> result = new HashMap<>();
            result.put("totalGaps", coverageGaps.size());

            long critical = coverageGaps.stream().filter(g -> "CRITICAL".equals(g.getPriority())).count();
            long high = coverageGaps.stream().filter(g -> "HIGH".equals(g.getPriority())).count();
            long medium = coverageGaps.stream().filter(g -> "MEDIUM".equals(g.getPriority())).count();
            long low = coverageGaps.stream().filter(g -> "LOW".equals(g.getPriority())).count();

            result.put("critical", critical);
            result.put("high", high);
            result.put("medium", medium);
            result.put("low", low);

            return result;
        }
    }

    public static class CoverageGap {
        private final String className;
        private final String methodName;
        private final int missedLines;
        private final int coveredLines;
        private int complexity = 1;
        private String priority = "LOW";
        private String testScaffold;

        public CoverageGap(String className, String methodName, int missedLines, int coveredLines) {
            this.className = className;
            this.methodName = methodName;
            this.missedLines = missedLines;
            this.coveredLines = coveredLines;
        }

        public void setComplexity(int complexity) {
            this.complexity = complexity;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public void setTestScaffold(String testScaffold) {
            this.testScaffold = testScaffold;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getMissedLines() {
            return missedLines;
        }

        public int getCoveredLines() {
            return coveredLines;
        }

        public int getComplexity() {
            return complexity;
        }

        public String getPriority() {
            return priority;
        }

        public String getTestScaffold() {
            return testScaffold;
        }

        public double getCoveragePercentage() {
            int total = missedLines + coveredLines;
            if (total == 0) return 0.0;
            return (coveredLines * 100.0) / total;
        }
    }
}
