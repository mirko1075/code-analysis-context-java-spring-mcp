package com.mcp.codeanalysis.utils;

import com.mcp.codeanalysis.graph.BeanDependencyGraph;
import com.mcp.codeanalysis.graph.DependencyGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Mermaid diagrams for code architecture and dependencies.
 */
public class DiagramGenerator {

    /**
     * Generate a package dependency diagram.
     *
     * @param dependencyGraph Package dependency graph
     * @return Mermaid diagram as string
     */
    public String generateDependencyDiagram(DependencyGraph dependencyGraph) {
        return generateDependencyDiagram(dependencyGraph, null);
    }

    /**
     * Generate a package dependency diagram with coupling metrics.
     *
     * @param dependencyGraph Package dependency graph
     * @param metrics Coupling metrics for styling (optional)
     * @return Mermaid diagram as string
     */
    public String generateDependencyDiagram(DependencyGraph dependencyGraph,
                                           Map<String, DependencyGraph.CouplingMetrics> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        Graph<String, DefaultEdge> graph = dependencyGraph.getGraph();
        Set<String> nodes = dependencyGraph.getNodes();

        // Add nodes with optional styling based on metrics
        for (String node : nodes) {
            String nodeId = sanitizeNodeId(node);
            String label = formatPackageName(node);

            if (metrics != null && metrics.containsKey(node)) {
                DependencyGraph.CouplingMetrics metric = metrics.get(node);
                String style = getMetricStyle(metric);
                sb.append(String.format("    %s[\"%s\"]%s\n", nodeId, label, style));
            } else {
                sb.append(String.format("    %s[\"%s\"]\n", nodeId, label));
            }
        }

        // Add edges
        for (String source : nodes) {
            Set<String> dependencies = dependencyGraph.getDependenciesOf(source);
            for (String target : dependencies) {
                String sourceId = sanitizeNodeId(source);
                String targetId = sanitizeNodeId(target);
                sb.append(String.format("    %s --> %s\n", sourceId, targetId));
            }
        }

        // Add styling classes
        if (metrics != null) {
            sb.append("\n");
            sb.append("    classDef stable fill:#90EE90\n");
            sb.append("    classDef unstable fill:#FFB6C1\n");
            sb.append("    classDef moderate fill:#FFE4B5\n");
        }

        return sb.toString();
    }

    /**
     * Generate a bean dependency diagram.
     *
     * @param beanGraph Bean dependency graph
     * @return Mermaid diagram as string
     */
    public String generateBeanDiagram(BeanDependencyGraph beanGraph) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        Graph<String, DefaultEdge> graph = beanGraph.getGraph();
        Set<String> beans = beanGraph.getBeans();

        // Add beans as nodes with styling based on source
        for (String bean : beans) {
            String nodeId = sanitizeNodeId(bean);
            BeanDependencyGraph.BeanInfo info = beanGraph.getBeanInfo(bean);

            String style = "";
            if (info != null) {
                if (info.isXmlDefined()) {
                    style = ":::xmlBean";
                } else if (info.isAnnotationDefined()) {
                    style = ":::annotationBean";
                }
            }

            sb.append(String.format("    %s[\"%s\"]%s\n", nodeId, bean, style));
        }

        // Add dependencies
        for (String bean : beans) {
            Set<String> dependencies = beanGraph.getDependenciesOf(bean);
            for (String dependency : dependencies) {
                String sourceId = sanitizeNodeId(bean);
                String targetId = sanitizeNodeId(dependency);
                sb.append(String.format("    %s --> %s\n", sourceId, targetId));
            }
        }

        // Add styling classes
        sb.append("\n");
        sb.append("    classDef xmlBean fill:#E0E0FF\n");
        sb.append("    classDef annotationBean fill:#FFE0E0\n");

        return sb.toString();
    }

    /**
     * Generate an architecture diagram showing package layers.
     *
     * @param packages Map of layer name to packages
     * @return Mermaid diagram as string
     */
    public String generateArchitectureDiagram(Map<String, List<String>> packages) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TB\n");

        // Common layer order
        List<String> layerOrder = Arrays.asList(
            "controller", "api", "rest",
            "service", "business",
            "repository", "dao", "data",
            "model", "entity", "domain"
        );

        // Sort layers by conventional order
        List<String> sortedLayers = packages.keySet().stream()
                .sorted((a, b) -> {
                    int indexA = getLayerIndex(a, layerOrder);
                    int indexB = getLayerIndex(b, layerOrder);
                    return Integer.compare(indexA, indexB);
                })
                .collect(Collectors.toList());

        // Add subgraphs for each layer
        for (String layer : sortedLayers) {
            List<String> layerPackages = packages.get(layer);
            if (layerPackages != null && !layerPackages.isEmpty()) {
                String subgraphId = sanitizeNodeId(layer);
                sb.append(String.format("    subgraph %s[\"%s Layer\"]\n", subgraphId, capitalize(layer)));

                for (String pkg : layerPackages) {
                    String nodeId = sanitizeNodeId(pkg);
                    String label = formatPackageName(pkg);
                    sb.append(String.format("        %s[\"%s\"]\n", nodeId, label));
                }

                sb.append("    end\n");
            }
        }

        return sb.toString();
    }

    /**
     * Generate a circular dependency diagram highlighting cycles.
     *
     * @param cycles List of circular dependency cycles
     * @return Mermaid diagram as string
     */
    public String generateCircularDependencyDiagram(List<List<String>> cycles) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");

        Set<String> nodesInCycles = new HashSet<>();
        Set<String> edges = new HashSet<>();

        for (List<String> cycle : cycles) {
            nodesInCycles.addAll(cycle);

            // Add edges for the cycle
            for (int i = 0; i < cycle.size(); i++) {
                String source = cycle.get(i);
                String target = cycle.get((i + 1) % cycle.size());
                edges.add(source + "->" + target);
            }
        }

        // Add nodes
        for (String node : nodesInCycles) {
            String nodeId = sanitizeNodeId(node);
            String label = formatPackageName(node);
            sb.append(String.format("    %s[\"%s\"]:::cycle\n", nodeId, label));
        }

        // Add edges
        for (String edge : edges) {
            String[] parts = edge.split("->");
            String sourceId = sanitizeNodeId(parts[0]);
            String targetId = sanitizeNodeId(parts[1]);
            sb.append(String.format("    %s --> %s\n", sourceId, targetId));
        }

        // Add styling
        sb.append("\n");
        sb.append("    classDef cycle fill:#FF6B6B,stroke:#C92A2A,stroke-width:3px\n");

        return sb.toString();
    }

    /**
     * Generate a class diagram with methods and fields.
     *
     * @param className Class name
     * @param methods List of method signatures
     * @param fields List of field names
     * @return Mermaid class diagram as string
     */
    public String generateClassDiagram(String className, List<String> methods, List<String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("classDiagram\n");

        String classId = sanitizeNodeId(className);
        sb.append(String.format("    class %s {\n", classId));

        // Add fields
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields) {
                sb.append(String.format("        %s\n", field));
            }
        }

        // Add methods
        if (methods != null && !methods.isEmpty()) {
            for (String method : methods) {
                sb.append(String.format("        %s\n", method));
            }
        }

        sb.append("    }\n");

        return sb.toString();
    }

    /**
     * Generate a complexity heatmap diagram.
     *
     * @param complexities Map of component name to complexity score
     * @return Mermaid diagram as string
     */
    public String generateComplexityHeatmap(Map<String, Integer> complexities) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        for (Map.Entry<String, Integer> entry : complexities.entrySet()) {
            String component = entry.getKey();
            int complexity = entry.getValue();

            String nodeId = sanitizeNodeId(component);
            // Sanitize special characters but keep dots for readability
            String sanitizedComponent = component.replaceAll("[^a-zA-Z0-9_.]", "_");
            String label = formatPackageName(sanitizedComponent) + " [" + complexity + "]";
            String style = getComplexityStyle(complexity);

            sb.append(String.format("    %s[\"%s\"]%s\n", nodeId, label, style));
        }

        // Add styling classes
        sb.append("\n");
        sb.append("    classDef low fill:#90EE90\n");
        sb.append("    classDef medium fill:#FFE4B5\n");
        sb.append("    classDef high fill:#FFB6C1\n");
        sb.append("    classDef veryhigh fill:#FF6B6B\n");

        return sb.toString();
    }

    /**
     * Sanitize node ID for Mermaid (alphanumeric and underscores only).
     */
    private String sanitizeNodeId(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Format package name for display (show last 2-3 segments).
     */
    private String formatPackageName(String packageName) {
        String[] parts = packageName.split("\\.");
        if (parts.length <= 3) {
            return packageName;
        }

        // Show last 3 segments
        return "..." + parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /**
     * Get style class based on coupling metrics.
     */
    private String getMetricStyle(DependencyGraph.CouplingMetrics metric) {
        if (metric.getInstability() < 0.3) {
            return ":::stable";
        } else if (metric.getInstability() > 0.7) {
            return ":::unstable";
        } else {
            return ":::moderate";
        }
    }

    /**
     * Get style class based on complexity.
     */
    private String getComplexityStyle(int complexity) {
        if (complexity <= 5) {
            return ":::low";
        } else if (complexity <= 10) {
            return ":::medium";
        } else if (complexity <= 20) {
            return ":::high";
        } else {
            return ":::veryhigh";
        }
    }

    /**
     * Get layer index for sorting.
     */
    private int getLayerIndex(String layer, List<String> layerOrder) {
        String lowerLayer = layer.toLowerCase();
        for (int i = 0; i < layerOrder.size(); i++) {
            if (lowerLayer.contains(layerOrder.get(i))) {
                return i;
            }
        }
        return layerOrder.size(); // Unknown layers go last
    }

    /**
     * Capitalize first letter of string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
