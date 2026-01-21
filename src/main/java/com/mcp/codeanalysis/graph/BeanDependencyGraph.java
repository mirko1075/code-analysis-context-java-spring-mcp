package com.mcp.codeanalysis.graph;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.parsers.XmlConfigParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.types.XmlBeanDefinition;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds and analyzes Spring bean dependency graphs.
 * Combines XML bean definitions and annotation-based beans.
 */
public class BeanDependencyGraph {
    private static final Logger logger = LoggerFactory.getLogger(BeanDependencyGraph.class);

    private final Graph<String, DefaultEdge> graph;
    private final XmlConfigParser xmlParser;
    private final JavaSourceParser javaParser;
    private final Map<String, BeanInfo> beanRegistry;

    // Spring component annotations
    private static final Set<String> COMPONENT_ANNOTATIONS = Set.of(
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "Bean"
    );

    // Dependency injection annotations
    private static final Set<String> INJECTION_ANNOTATIONS = Set.of(
            "Autowired", "Inject", "Resource", "Value"
    );

    public BeanDependencyGraph() {
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.xmlParser = new XmlConfigParser();
        this.javaParser = new JavaSourceParser();
        this.beanRegistry = new HashMap<>();
    }

    /**
     * Add XML configuration files to the bean graph.
     *
     * @param xmlFiles List of Spring XML configuration files
     */
    public void addXmlConfigs(List<Path> xmlFiles) {
        for (Path xmlFile : xmlFiles) {
            addXmlConfig(xmlFile);
        }
    }

    /**
     * Add a single XML configuration file.
     *
     * @param xmlFile Spring XML configuration file
     */
    public void addXmlConfig(Path xmlFile) {
        List<XmlBeanDefinition> beans = xmlParser.parseXmlConfig(xmlFile);

        for (XmlBeanDefinition bean : beans) {
            String beanId = bean.getId();
            if (beanId == null || beanId.isEmpty()) {
                beanId = bean.getClassName();
            }

            if (beanId != null) {
                // Add bean as node
                graph.addVertex(beanId);

                // Register bean info
                BeanInfo beanInfo = new BeanInfo(beanId, bean.getClassName(), BeanSource.XML);
                beanRegistry.put(beanId, beanInfo);

                // Add edges for dependencies from property injections
                for (XmlBeanDefinition.PropertyInjection property : bean.getProperties()) {
                    if (property.isReference()) {
                        String dependency = property.getRef();
                        if (dependency != null && !dependency.isEmpty()) {
                            graph.addVertex(dependency);
                            try {
                                graph.addEdge(beanId, dependency);
                            } catch (IllegalArgumentException e) {
                                // Edge already exists, ignore
                            }
                        }
                    }
                }

                // Add edges for dependencies from constructor arguments
                for (XmlBeanDefinition.ConstructorArgument arg : bean.getConstructorArgs()) {
                    if (arg.isReference()) {
                        String dependency = arg.getRef();
                        if (dependency != null && !dependency.isEmpty()) {
                            graph.addVertex(dependency);
                            try {
                                graph.addEdge(beanId, dependency);
                            } catch (IllegalArgumentException e) {
                                // Edge already exists, ignore
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Add Java source files to find annotated beans.
     *
     * @param javaFiles List of Java source files
     */
    public void addJavaFiles(List<Path> javaFiles) {
        for (Path javaFile : javaFiles) {
            addJavaFile(javaFile);
        }
    }

    /**
     * Add a single Java file to find annotated beans.
     *
     * @param javaFile Java source file
     */
    public void addJavaFile(Path javaFile) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            String beanId = deriveAnnotatedBeanId(classInfo, fileInfo.getPackageName());

            if (beanId != null) {
                // Add bean as node
                graph.addVertex(beanId);

                // Register bean info
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                BeanInfo beanInfo = new BeanInfo(beanId, fullClassName, BeanSource.ANNOTATION);
                beanRegistry.put(beanId, beanInfo);

                // Add dependencies from constructor, fields, and setters
                addAnnotatedDependencies(beanId, classInfo);
            }
        }
    }

    /**
     * Derive bean ID from annotated class.
     *
     * @param classInfo Class information
     * @param packageName Package name
     * @return Bean ID or null if not a bean
     */
    private String deriveAnnotatedBeanId(JavaFileInfo.ClassInfo classInfo, String packageName) {
        // Check if class has component annotation
        for (String annotation : classInfo.getAnnotations()) {
            if (COMPONENT_ANNOTATIONS.contains(annotation)) {
                // Default bean ID is class name with first letter lowercase
                String className = classInfo.getName();
                return Character.toLowerCase(className.charAt(0)) + className.substring(1);
            }
        }
        return null;
    }

    /**
     * Add dependencies from annotated fields and constructors.
     */
    private void addAnnotatedDependencies(String beanId, JavaFileInfo.ClassInfo classInfo) {
        // Check constructor parameters (assume autowired if only one constructor)
        if (classInfo.getMethods().stream().anyMatch(m -> m.getName().equals(classInfo.getName()))) {
            // Has explicit constructor - would need parameter type analysis
            // For now, we'll rely on field injection detection
        }

        // Check autowired fields
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            if (hasInjectionAnnotation(field.getAnnotations())) {
                String dependencyBeanId = deriveBeanIdFromFieldType(field.getType());
                if (dependencyBeanId != null) {
                    graph.addVertex(dependencyBeanId);
                    try {
                        graph.addEdge(beanId, dependencyBeanId);
                    } catch (IllegalArgumentException e) {
                        // Edge already exists, ignore
                    }
                }
            }
        }

        // Check autowired setters
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            if (hasInjectionAnnotation(method.getAnnotations()) &&
                method.getName().startsWith("set") &&
                method.getParameters().size() == 1) {

                String paramType = method.getParameters().get(0).getType();
                String dependencyBeanId = deriveBeanIdFromFieldType(paramType);
                if (dependencyBeanId != null) {
                    graph.addVertex(dependencyBeanId);
                    try {
                        graph.addEdge(beanId, dependencyBeanId);
                    } catch (IllegalArgumentException e) {
                        // Edge already exists, ignore
                    }
                }
            }
        }
    }

    /**
     * Check if annotations contain injection annotation.
     */
    private boolean hasInjectionAnnotation(List<String> annotations) {
        return annotations.stream().anyMatch(INJECTION_ANNOTATIONS::contains);
    }

    /**
     * Derive bean ID from field type.
     */
    private String deriveBeanIdFromFieldType(String fieldType) {
        // Remove generic types
        if (fieldType.contains("<")) {
            fieldType = fieldType.substring(0, fieldType.indexOf('<'));
        }

        // Extract class name from fully qualified name
        int lastDotIndex = fieldType.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fieldType = fieldType.substring(lastDotIndex + 1);
        }

        // Convert to bean ID (first letter lowercase)
        if (!fieldType.isEmpty()) {
            return Character.toLowerCase(fieldType.charAt(0)) + fieldType.substring(1);
        }

        return null;
    }

    /**
     * Detect circular bean dependencies.
     *
     * @return List of circular dependency cycles
     */
    public List<List<String>> detectCircularDependencies() {
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);

        if (!cycleDetector.detectCycles()) {
            return Collections.emptyList();
        }

        Set<String> beansInCycles = cycleDetector.findCycles();
        List<List<String>> cycles = new ArrayList<>();

        // Find actual cycles
        for (String bean : beansInCycles) {
            List<String> cycle = findCycleContaining(bean, cycleDetector);
            if (cycle != null && !cycles.contains(cycle)) {
                cycles.add(cycle);
            }
        }

        return cycles;
    }

    /**
     * Find a cycle containing the given bean.
     */
    private List<String> findCycleContaining(String bean, CycleDetector<String, DefaultEdge> detector) {
        List<String> cycle = new ArrayList<>();
        cycle.add(bean);

        Set<String> visited = new HashSet<>();
        visited.add(bean);

        String current = bean;

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

            if (next.equals(bean)) {
                return cycle;
            }

            if (visited.contains(next)) {
                break;
            }

            cycle.add(next);
            visited.add(next);
            current = next;
        }

        return null;
    }

    /**
     * Get all beans in the graph.
     *
     * @return Set of bean IDs
     */
    public Set<String> getBeans() {
        return new HashSet<>(graph.vertexSet());
    }

    /**
     * Get dependencies of a specific bean.
     *
     * @param beanId Bean ID
     * @return Set of bean IDs that this bean depends on
     */
    public Set<String> getDependenciesOf(String beanId) {
        if (!graph.containsVertex(beanId)) {
            return Collections.emptySet();
        }

        Set<DefaultEdge> outgoingEdges = graph.outgoingEdgesOf(beanId);
        return outgoingEdges.stream()
                .map(graph::getEdgeTarget)
                .collect(Collectors.toSet());
    }

    /**
     * Get beans that depend on a specific bean.
     *
     * @param beanId Bean ID
     * @return Set of bean IDs that depend on this bean
     */
    public Set<String> getDependentsOf(String beanId) {
        if (!graph.containsVertex(beanId)) {
            return Collections.emptySet();
        }

        Set<DefaultEdge> incomingEdges = graph.incomingEdgesOf(beanId);
        return incomingEdges.stream()
                .map(graph::getEdgeSource)
                .collect(Collectors.toSet());
    }

    /**
     * Get bean information.
     *
     * @param beanId Bean ID
     * @return BeanInfo or null if not found
     */
    public BeanInfo getBeanInfo(String beanId) {
        return beanRegistry.get(beanId);
    }

    /**
     * Get all registered beans with their info.
     *
     * @return Map of bean ID to BeanInfo
     */
    public Map<String, BeanInfo> getAllBeanInfo() {
        return new HashMap<>(beanRegistry);
    }

    /**
     * Get the number of beans in the graph.
     *
     * @return Number of beans
     */
    public int getBeanCount() {
        return graph.vertexSet().size();
    }

    /**
     * Get the number of dependencies in the graph.
     *
     * @return Number of dependencies (edges)
     */
    public int getDependencyCount() {
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
     * Bean source type.
     */
    public enum BeanSource {
        XML,        // Defined in XML configuration
        ANNOTATION  // Defined via annotations
    }

    /**
     * Information about a Spring bean.
     */
    public static class BeanInfo {
        private final String beanId;
        private final String className;
        private final BeanSource source;

        public BeanInfo(String beanId, String className, BeanSource source) {
            this.beanId = beanId;
            this.className = className;
            this.source = source;
        }

        public String getBeanId() {
            return beanId;
        }

        public String getClassName() {
            return className;
        }

        public BeanSource getSource() {
            return source;
        }

        public boolean isXmlDefined() {
            return source == BeanSource.XML;
        }

        public boolean isAnnotationDefined() {
            return source == BeanSource.ANNOTATION;
        }

        @Override
        public String toString() {
            return "BeanInfo{" +
                    "beanId='" + beanId + '\'' +
                    ", className='" + className + '\'' +
                    ", source=" + source +
                    '}';
        }
    }
}
