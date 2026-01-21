package com.mcp.codeanalysis.graph;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds and analyzes package/class dependency graphs.
 * Uses JGraphT to create directed graphs from Java imports.
 */
public class DependencyGraph {
    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);

    private final Graph<String, DefaultEdge> graph;
    private final JavaSourceParser javaParser;
    private final DependencyLevel level;

    /**
     * Level of dependency analysis.
     */
    public enum DependencyLevel {
        PACKAGE,  // Package-level dependencies (e.g., com.example.service)
        CLASS     // Class-level dependencies (e.g., com.example.service.UserService)
    }

    /**
     * Create a DependencyGraph at package level.
     */
    public DependencyGraph() {
        this(DependencyLevel.PACKAGE);
    }

    /**
     * Create a DependencyGraph at specified level.
     *
     * @param level Dependency level (package or class)
     */
    public DependencyGraph(DependencyLevel level) {
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.javaParser = new JavaSourceParser();
        this.level = level;
    }

    /**
     * Add Java files to the dependency graph.
     *
     * @param javaFiles List of Java source files to analyze
     */
    public void addFiles(List<Path> javaFiles) {
        for (Path javaFile : javaFiles) {
            try {
                addFile(javaFile);
            } catch (Exception e) {
                logger.warn("Failed to add file to graph: {}", javaFile, e);
            }
        }
    }

    /**
     * Add a single Java file to the dependency graph.
     *
     * @param javaFile Java source file to analyze
     */
    public void addFile(Path javaFile) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        String packageName = fileInfo.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            logger.debug("Skipping file with no package: {}", javaFile);
            return;
        }

        // Add nodes and edges based on level
        if (level == DependencyLevel.PACKAGE) {
            addPackageDependencies(fileInfo);
        } else {
            addClassDependencies(fileInfo);
        }
    }

    /**
     * Add package-level dependencies from a Java file.
     */
    private void addPackageDependencies(JavaFileInfo fileInfo) {
        String sourcePackage = fileInfo.getPackageName();

        // Add source package as node
        graph.addVertex(sourcePackage);

        // Add edges for each imported package
        for (String importStatement : fileInfo.getImports()) {
            String targetPackage = extractPackageFromImport(importStatement);

            if (targetPackage != null && !targetPackage.equals(sourcePackage)) {
                // Add target package as node
                graph.addVertex(targetPackage);

                // Add edge if not already present
                try {
                    graph.addEdge(sourcePackage, targetPackage);
                } catch (IllegalArgumentException e) {
                    // Edge already exists, ignore
                }
            }
        }
    }

    /**
     * Add class-level dependencies from a Java file.
     */
    private void addClassDependencies(JavaFileInfo fileInfo) {
        String packageName = fileInfo.getPackageName();

        // Add each class in the file as a node
        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            String sourceClass = packageName + "." + classInfo.getName();
            graph.addVertex(sourceClass);

            // Add edges for each import
            for (String importStatement : fileInfo.getImports()) {
                String targetClass = extractClassFromImport(importStatement);

                if (targetClass != null && !targetClass.equals(sourceClass)) {
                    graph.addVertex(targetClass);

                    try {
                        graph.addEdge(sourceClass, targetClass);
                    } catch (IllegalArgumentException e) {
                        // Edge already exists, ignore
                    }
                }
            }
        }
    }

    /**
     * Extract package name from import statement.
     *
     * @param importStatement Import statement (e.g., "com.example.service.UserService")
     * @return Package name (e.g., "com.example.service") or null if not valid
     */
    private String extractPackageFromImport(String importStatement) {
        // Filter out Java standard library and common third-party libraries
        if (isStandardLibrary(importStatement)) {
            return null;
        }

        int lastDotIndex = importStatement.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return importStatement.substring(0, lastDotIndex);
        }

        return null;
    }

    /**
     * Extract class name from import statement.
     *
     * @param importStatement Import statement
     * @return Full class name or null if not valid
     */
    private String extractClassFromImport(String importStatement) {
        if (isStandardLibrary(importStatement)) {
            return null;
        }

        // Remove wildcard imports
        if (importStatement.endsWith(".*")) {
            return null;
        }

        return importStatement;
    }

    /**
     * Check if import is from standard library.
     */
    private boolean isStandardLibrary(String importStatement) {
        return importStatement.startsWith("java.") ||
               importStatement.startsWith("javax.") ||
               importStatement.startsWith("jakarta.") ||
               importStatement.startsWith("org.springframework.") ||
               importStatement.startsWith("org.junit.") ||
               importStatement.startsWith("org.mockito.");
    }

    /**
     * Detect circular dependencies in the graph.
     *
     * @return List of circular dependency cycles
     */
    public List<List<String>> detectCircularDependencies() {
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);

        if (!cycleDetector.detectCycles()) {
            return Collections.emptyList();
        }

        Set<String> verticesInCycles = cycleDetector.findCycles();
        List<List<String>> cycles = new ArrayList<>();

        // Find actual cycles
        for (String vertex : verticesInCycles) {
            List<String> cycle = findCycleContaining(vertex, cycleDetector);
            if (cycle != null && !cycles.contains(cycle)) {
                cycles.add(cycle);
            }
        }

        return cycles;
    }

    /**
     * Find a cycle containing the given vertex.
     */
    private List<String> findCycleContaining(String vertex, CycleDetector<String, DefaultEdge> detector) {
        List<String> cycle = new ArrayList<>();
        cycle.add(vertex);

        Set<String> visited = new HashSet<>();
        visited.add(vertex);

        String current = vertex;

        // Follow edges until we return to the starting vertex
        while (true) {
            Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(current);

            String next = null;
            for (DefaultEdge edge : outgoingEdges) {
                String target = graph.getEdgeTarget(edge);
                if (detector.findCycles().contains(target)) {
                    next = target;
                    break;
                }
            }

            if (next == null) {
                break;
            }

            if (next.equals(vertex)) {
                // Completed the cycle
                return cycle;
            }

            if (visited.contains(next)) {
                // Already visited, break to avoid infinite loop
                break;
            }

            cycle.add(next);
            visited.add(next);
            current = next;
        }

        return null;
    }

    /**
     * Calculate coupling metrics for the graph.
     *
     * @return Map of node to coupling metrics
     */
    public Map<String, CouplingMetrics> calculateCouplingMetrics() {
        Map<String, CouplingMetrics> metrics = new HashMap<>();

        for (String vertex : graph.vertexSet()) {
            int afferentCoupling = graph.inDegreeOf(vertex);  // How many depend on this
            int efferentCoupling = graph.outDegreeOf(vertex); // How many this depends on

            double instability = calculateInstability(afferentCoupling, efferentCoupling);

            CouplingMetrics metric = new CouplingMetrics(
                vertex,
                afferentCoupling,
                efferentCoupling,
                instability
            );

            metrics.put(vertex, metric);
        }

        return metrics;
    }

    /**
     * Calculate instability metric.
     * Instability = Ce / (Ca + Ce)
     * 0 = maximally stable, 1 = maximally unstable
     */
    private double calculateInstability(int afferentCoupling, int efferentCoupling) {
        int total = afferentCoupling + efferentCoupling;
        if (total == 0) {
            return 0.0;
        }
        return (double) efferentCoupling / total;
    }

    /**
     * Get all nodes in the graph.
     *
     * @return Set of node names
     */
    public Set<String> getNodes() {
        return new HashSet<>(graph.vertexSet());
    }

    /**
     * Get dependencies of a specific node.
     *
     * @param node Node name
     * @return Set of nodes that this node depends on
     */
    public Set<String> getDependenciesOf(String node) {
        Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(node);
        return outgoingEdges.stream()
                .map(graph::getEdgeTarget)
                .collect(Collectors.toSet());
    }

    /**
     * Get dependents of a specific node.
     *
     * @param node Node name
     * @return Set of nodes that depend on this node
     */
    public Set<String> getDependentsOf(String node) {
        Set<DefaultEdge> incomingEdges = graph.incomingEdgesOf(node);
        return incomingEdges.stream()
                .map(graph::getEdgeSource)
                .collect(Collectors.toSet());
    }

    /**
     * Get the number of nodes in the graph.
     *
     * @return Number of nodes
     */
    public int getNodeCount() {
        return graph.vertexSet().size();
    }

    /**
     * Get the number of edges in the graph.
     *
     * @return Number of edges
     */
    public int getEdgeCount() {
        return graph.edgeSet().size();
    }

    /**
     * Get the underlying JGraphT graph.
     *
     * @return The graph
     */
    public Graph<String, DefaultEdge> getGraph() {
        return graph;
    }

    /**
     * Coupling metrics for a node.
     */
    public static class CouplingMetrics {
        private final String node;
        private final int afferentCoupling;  // Ca - incoming dependencies
        private final int efferentCoupling;  // Ce - outgoing dependencies
        private final double instability;     // I = Ce / (Ca + Ce)

        public CouplingMetrics(String node, int afferentCoupling, int efferentCoupling, double instability) {
            this.node = node;
            this.afferentCoupling = afferentCoupling;
            this.efferentCoupling = efferentCoupling;
            this.instability = instability;
        }

        public String getNode() {
            return node;
        }

        public int getAfferentCoupling() {
            return afferentCoupling;
        }

        public int getEfferentCoupling() {
            return efferentCoupling;
        }

        public double getInstability() {
            return instability;
        }

        public boolean isStable() {
            return instability < 0.5;
        }

        public boolean isUnstable() {
            return instability >= 0.5;
        }

        @Override
        public String toString() {
            return "CouplingMetrics{" +
                    "node='" + node + '\'' +
                    ", Ca=" + afferentCoupling +
                    ", Ce=" + efferentCoupling +
                    ", I=" + String.format("%.2f", instability) +
                    '}';
        }
    }
}
