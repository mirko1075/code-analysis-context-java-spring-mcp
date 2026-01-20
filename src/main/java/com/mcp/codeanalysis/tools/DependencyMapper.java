package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.graph.BeanDependencyGraph;
import com.mcp.codeanalysis.graph.DependencyGraph;
import com.mcp.codeanalysis.utils.DiagramGenerator;
import com.mcp.codeanalysis.utils.FileScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Dependency mapping tool for MCP.
 * Analyzes package dependencies, bean dependencies, and circular dependencies.
 */
public class DependencyMapper {
    private static final Logger logger = LoggerFactory.getLogger(DependencyMapper.class);

    /**
     * Analyze project dependencies.
     *
     * @param projectPath Path to project root
     * @param options Analysis options
     * @return Dependency analysis result
     */
    public DependencyAnalysisResult analyze(String projectPath, DependencyOptions options) {
        Path projectRoot = Paths.get(projectPath);
        DependencyAnalysisResult result = new DependencyAnalysisResult();

        try {
            // Initialize scanner
            FileScanner fileScanner = new FileScanner(projectRoot);

            // Scan for files
            List<Path> javaFiles = fileScanner.scanJavaFiles();
            List<Path> xmlFiles = fileScanner.scanXmlFiles();

            // Build package dependency graph
            DependencyGraph packageGraph = new DependencyGraph();
            packageGraph.addFiles(javaFiles);
            result.setPackageGraph(packageGraph);

            // Detect circular dependencies if requested
            if (options.detectCircular) {
                List<List<String>> packageCycles = packageGraph.detectCircularDependencies();
                result.setPackageCircularDependencies(packageCycles);
            }

            // Calculate metrics if requested
            if (options.calculateMetrics) {
                Map<String, DependencyGraph.CouplingMetrics> metrics = packageGraph.calculateCouplingMetrics();
                result.setPackageCouplingMetrics(metrics);
            }

            // Build bean dependency graph if XML or Java files exist
            if (!xmlFiles.isEmpty() || !javaFiles.isEmpty()) {
                BeanDependencyGraph beanGraph = new BeanDependencyGraph();

                // Add XML beans
                if (!xmlFiles.isEmpty()) {
                    beanGraph.addXmlConfigs(xmlFiles);
                }

                // Add annotation-based beans
                if (!javaFiles.isEmpty()) {
                    beanGraph.addJavaFiles(javaFiles);
                }

                result.setBeanGraph(beanGraph);

                // Detect bean circular dependencies
                if (options.detectCircular) {
                    List<List<String>> beanCycles = beanGraph.detectCircularDependencies();
                    result.setBeanCircularDependencies(beanCycles);
                }
            }

            // Generate diagrams if requested
            if (options.generateDiagrams) {
                DiagramGenerator diagramGenerator = new DiagramGenerator();

                // Generate package dependency diagram
                String packageDiagram;
                if (options.calculateMetrics && result.getPackageCouplingMetrics() != null) {
                    packageDiagram = diagramGenerator.generateDependencyDiagram(
                            packageGraph, result.getPackageCouplingMetrics());
                } else {
                    packageDiagram = diagramGenerator.generateDependencyDiagram(packageGraph);
                }
                result.setPackageDependencyDiagram(packageDiagram);

                // Generate bean dependency diagram if beans exist
                if (result.getBeanGraph() != null && !result.getBeanGraph().getBeans().isEmpty()) {
                    String beanDiagram = diagramGenerator.generateBeanDiagram(result.getBeanGraph());
                    result.setBeanDependencyDiagram(beanDiagram);
                }

                // Generate circular dependency diagrams
                if (!result.getPackageCircularDependencies().isEmpty()) {
                    String circularDiagram = diagramGenerator.generateCircularDependencyDiagram(
                            result.getPackageCircularDependencies());
                    result.setCircularDependencyDiagram(circularDiagram);
                }
            }

        } catch (IOException e) {
            logger.error("Error analyzing dependencies: {}", projectPath, e);
        }

        return result;
    }

    // DTOs

    public static class DependencyOptions {
        public boolean detectCircular = true;
        public boolean calculateMetrics = true;
        public boolean generateDiagrams = true;
        public String focusPackage = null;
        public int maxDepth = 10;
    }

    public static class DependencyAnalysisResult {
        private DependencyGraph packageGraph;
        private BeanDependencyGraph beanGraph;
        private List<List<String>> packageCircularDependencies = new ArrayList<>();
        private List<List<String>> beanCircularDependencies = new ArrayList<>();
        private Map<String, DependencyGraph.CouplingMetrics> packageCouplingMetrics;
        private String packageDependencyDiagram;
        private String beanDependencyDiagram;
        private String circularDependencyDiagram;

        // Getters and setters

        public DependencyGraph getPackageGraph() {
            return packageGraph;
        }

        public void setPackageGraph(DependencyGraph graph) {
            this.packageGraph = graph;
        }

        public BeanDependencyGraph getBeanGraph() {
            return beanGraph;
        }

        public void setBeanGraph(BeanDependencyGraph graph) {
            this.beanGraph = graph;
        }

        public List<List<String>> getPackageCircularDependencies() {
            return new ArrayList<>(packageCircularDependencies);
        }

        public void setPackageCircularDependencies(List<List<String>> cycles) {
            this.packageCircularDependencies = cycles;
        }

        public List<List<String>> getBeanCircularDependencies() {
            return new ArrayList<>(beanCircularDependencies);
        }

        public void setBeanCircularDependencies(List<List<String>> cycles) {
            this.beanCircularDependencies = cycles;
        }

        public Map<String, DependencyGraph.CouplingMetrics> getPackageCouplingMetrics() {
            return packageCouplingMetrics != null ? new HashMap<>(packageCouplingMetrics) : null;
        }

        public void setPackageCouplingMetrics(Map<String, DependencyGraph.CouplingMetrics> metrics) {
            this.packageCouplingMetrics = metrics;
        }

        public String getPackageDependencyDiagram() {
            return packageDependencyDiagram;
        }

        public void setPackageDependencyDiagram(String diagram) {
            this.packageDependencyDiagram = diagram;
        }

        public String getBeanDependencyDiagram() {
            return beanDependencyDiagram;
        }

        public void setBeanDependencyDiagram(String diagram) {
            this.beanDependencyDiagram = diagram;
        }

        public String getCircularDependencyDiagram() {
            return circularDependencyDiagram;
        }

        public void setCircularDependencyDiagram(String diagram) {
            this.circularDependencyDiagram = diagram;
        }

        /**
         * Get summary statistics.
         */
        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();

            if (packageGraph != null) {
                summary.put("totalPackages", packageGraph.getNodes().size());
                summary.put("packageDependencies", packageGraph.getGraph().edgeSet().size());
                summary.put("packageCircularDependencies", packageCircularDependencies.size());
            }

            if (beanGraph != null) {
                summary.put("totalBeans", beanGraph.getBeans().size());
                summary.put("beanDependencies", beanGraph.getGraph().edgeSet().size());
                summary.put("beanCircularDependencies", beanCircularDependencies.size());
            }

            return summary;
        }
    }
}
