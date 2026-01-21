package com.mcp.codeanalysis.utils;

import com.mcp.codeanalysis.graph.BeanDependencyGraph;
import com.mcp.codeanalysis.graph.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiagramGenerator.
 */
class DiagramGeneratorTest {

    @TempDir
    Path tempDir;

    private DiagramGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DiagramGenerator();
    }

    @Test
    void testGenerateDependencyDiagram() throws IOException {
        DependencyGraph graph = new DependencyGraph();

        // Create simple dependency graph
        graph.addFile(createJavaFile("UserService.java", """
                package com.example.service;
                import com.example.model.User;
                import com.example.repository.UserRepository;
                public class UserService {}
                """));

        String diagram = generator.generateDependencyDiagram(graph);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TD"));
        assertTrue(diagram.contains("com_example_service"));
        assertTrue(diagram.contains("com_example_model"));
        assertTrue(diagram.contains("com_example_repository"));
        assertTrue(diagram.contains("-->"));
    }

    @Test
    void testGenerateDependencyDiagramWithMetrics() throws IOException {
        DependencyGraph graph = new DependencyGraph();

        graph.addFile(createJavaFile("UserController.java", """
                package com.example.controller;
                import com.example.service.UserService;
                public class UserController {}
                """));

        graph.addFile(createJavaFile("UserService.java", """
                package com.example.service;
                import com.example.repository.UserRepository;
                public class UserService {}
                """));

        graph.addFile(createJavaFile("UserRepository.java", """
                package com.example.repository;
                public class UserRepository {}
                """));

        Map<String, DependencyGraph.CouplingMetrics> metrics = graph.calculateCouplingMetrics();
        String diagram = generator.generateDependencyDiagram(graph, metrics);

        assertNotNull(diagram);
        assertTrue(diagram.contains("graph TD"));
        assertTrue(diagram.contains("classDef stable"));
        assertTrue(diagram.contains("classDef unstable"));
        assertTrue(diagram.contains("classDef moderate"));
    }

    @Test
    void testGenerateBeanDiagram() throws IOException {
        BeanDependencyGraph beanGraph = new BeanDependencyGraph();

        // Add XML beans
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userService" class="com.example.service.UserService">
                        <property name="repository" ref="userRepository"/>
                    </bean>
                    <bean id="userRepository" class="com.example.repository.UserRepository"/>
                </beans>
                """;

        beanGraph.addXmlConfig(createXmlFile("applicationContext.xml", xml));

        String diagram = generator.generateBeanDiagram(beanGraph);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TD"));
        assertTrue(diagram.contains("userService"));
        assertTrue(diagram.contains("userRepository"));
        assertTrue(diagram.contains("-->"));
        assertTrue(diagram.contains("classDef xmlBean"));
    }

    @Test
    void testGenerateBeanDiagramWithAnnotations() throws IOException {
        BeanDependencyGraph beanGraph = new BeanDependencyGraph();

        beanGraph.addJavaFile(createJavaFile("UserService.java", """
                package com.example.service;
                import org.springframework.stereotype.Service;
                import org.springframework.beans.factory.annotation.Autowired;
                import com.example.repository.UserRepository;
                @Service
                public class UserService {
                    @Autowired
                    private UserRepository userRepository;
                }
                """));

        beanGraph.addJavaFile(createJavaFile("UserRepository.java", """
                package com.example.repository;
                import org.springframework.stereotype.Repository;
                @Repository
                public class UserRepository {}
                """));

        String diagram = generator.generateBeanDiagram(beanGraph);

        assertNotNull(diagram);
        assertTrue(diagram.contains("userService"));
        assertTrue(diagram.contains("userRepository"));
        assertTrue(diagram.contains("classDef annotationBean"));
    }

    @Test
    void testGenerateArchitectureDiagram() {
        Map<String, List<String>> packages = new HashMap<>();
        packages.put("controller", Arrays.asList("com.example.controller", "com.example.api"));
        packages.put("service", Arrays.asList("com.example.service"));
        packages.put("repository", Arrays.asList("com.example.repository", "com.example.dao"));
        packages.put("model", Arrays.asList("com.example.model", "com.example.entity"));

        String diagram = generator.generateArchitectureDiagram(packages);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TB"));
        assertTrue(diagram.contains("subgraph"));
        assertTrue(diagram.contains("Controller Layer"));
        assertTrue(diagram.contains("Service Layer"));
        assertTrue(diagram.contains("Repository Layer"));
        assertTrue(diagram.contains("Model Layer"));
    }

    @Test
    void testGenerateCircularDependencyDiagram() {
        List<List<String>> cycles = new ArrayList<>();

        // Add a circular dependency: A -> B -> C -> A
        cycles.add(Arrays.asList(
            "com.example.service.ServiceA",
            "com.example.service.ServiceB",
            "com.example.service.ServiceC"
        ));

        String diagram = generator.generateCircularDependencyDiagram(cycles);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph LR"));
        assertTrue(diagram.contains("ServiceA"));
        assertTrue(diagram.contains("ServiceB"));
        assertTrue(diagram.contains("ServiceC"));
        assertTrue(diagram.contains("-->"));
        assertTrue(diagram.contains("classDef cycle"));
        assertTrue(diagram.contains("fill:#FF6B6B"));
    }

    @Test
    void testGenerateCircularDependencyWithMultipleCycles() {
        List<List<String>> cycles = new ArrayList<>();

        cycles.add(Arrays.asList("com.example.A", "com.example.B"));
        cycles.add(Arrays.asList("com.example.C", "com.example.D", "com.example.E"));

        String diagram = generator.generateCircularDependencyDiagram(cycles);

        assertNotNull(diagram);
        assertTrue(diagram.contains("com_example_A"));
        assertTrue(diagram.contains("com_example_B"));
        assertTrue(diagram.contains("com_example_C"));
        assertTrue(diagram.contains("com_example_D"));
        assertTrue(diagram.contains("com_example_E"));
    }

    @Test
    void testGenerateClassDiagram() {
        String className = "com.example.User";
        List<String> methods = Arrays.asList(
            "+getName(): String",
            "+setName(String): void",
            "+isActive(): boolean"
        );
        List<String> fields = Arrays.asList(
            "-id: Long",
            "-name: String",
            "-active: boolean"
        );

        String diagram = generator.generateClassDiagram(className, methods, fields);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("classDiagram"));
        assertTrue(diagram.contains("class com_example_User"));
        assertTrue(diagram.contains("-id: Long"));
        assertTrue(diagram.contains("+getName(): String"));
    }

    @Test
    void testGenerateClassDiagramWithoutFields() {
        String className = "com.example.Service";
        List<String> methods = Arrays.asList("+execute(): void");

        String diagram = generator.generateClassDiagram(className, methods, null);

        assertNotNull(diagram);
        assertTrue(diagram.contains("class com_example_Service"));
        assertTrue(diagram.contains("+execute(): void"));
    }

    @Test
    void testGenerateClassDiagramWithoutMethods() {
        String className = "com.example.Model";
        List<String> fields = Arrays.asList("-data: String");

        String diagram = generator.generateClassDiagram(className, null, fields);

        assertNotNull(diagram);
        assertTrue(diagram.contains("class com_example_Model"));
        assertTrue(diagram.contains("-data: String"));
    }

    @Test
    void testGenerateComplexityHeatmap() {
        Map<String, Integer> complexities = new HashMap<>();
        complexities.put("com.example.SimpleService", 3);
        complexities.put("com.example.ModerateService", 8);
        complexities.put("com.example.ComplexService", 15);
        complexities.put("com.example.VeryComplexService", 25);

        String diagram = generator.generateComplexityHeatmap(complexities);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TD"));
        assertTrue(diagram.contains("[3]"));
        assertTrue(diagram.contains("[8]"));
        assertTrue(diagram.contains("[15]"));
        assertTrue(diagram.contains("[25]"));
        assertTrue(diagram.contains("classDef low"));
        assertTrue(diagram.contains("classDef medium"));
        assertTrue(diagram.contains("classDef high"));
        assertTrue(diagram.contains("classDef veryhigh"));
    }

    @Test
    void testPackageNameFormatting() {
        // Package names should be shortened for long packages
        Map<String, Integer> complexities = new HashMap<>();
        complexities.put("com.example.verylongpackagename.subpackage.service.UserService", 5);

        String diagram = generator.generateComplexityHeatmap(complexities);

        // Should show abbreviated package name
        assertTrue(diagram.contains("..."));
    }

    @Test
    void testShortPackageNameNoFormatting() {
        // Short package names should not be abbreviated
        Map<String, Integer> complexities = new HashMap<>();
        complexities.put("com.example.Service", 5);

        String diagram = generator.generateComplexityHeatmap(complexities);

        assertTrue(diagram.contains("com.example.Service"));
        assertFalse(diagram.contains("..."));
    }

    @Test
    void testNodeIdSanitization() {
        // Special characters should be replaced with underscores
        Map<String, Integer> complexities = new HashMap<>();
        complexities.put("com.example.service$inner.Class", 5);

        String diagram = generator.generateComplexityHeatmap(complexities);

        assertTrue(diagram.contains("com_example_service_inner_Class"));
        assertFalse(diagram.contains("$"));
    }

    @Test
    void testEmptyDependencyGraph() throws IOException {
        DependencyGraph graph = new DependencyGraph();

        String diagram = generator.generateDependencyDiagram(graph);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TD"));
        // Should have minimal content (just the graph declaration)
    }

    @Test
    void testEmptyBeanGraph() {
        BeanDependencyGraph beanGraph = new BeanDependencyGraph();

        String diagram = generator.generateBeanDiagram(beanGraph);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TD"));
        assertTrue(diagram.contains("classDef xmlBean"));
        assertTrue(diagram.contains("classDef annotationBean"));
    }

    @Test
    void testEmptyArchitectureDiagram() {
        Map<String, List<String>> packages = new HashMap<>();

        String diagram = generator.generateArchitectureDiagram(packages);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph TB"));
    }

    @Test
    void testEmptyCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();

        String diagram = generator.generateCircularDependencyDiagram(cycles);

        assertNotNull(diagram);
        assertTrue(diagram.startsWith("graph LR"));
    }

    @Test
    void testLayerOrdering() {
        Map<String, List<String>> packages = new LinkedHashMap<>();
        // Add in random order
        packages.put("model", Arrays.asList("com.example.model"));
        packages.put("controller", Arrays.asList("com.example.controller"));
        packages.put("repository", Arrays.asList("com.example.repository"));
        packages.put("service", Arrays.asList("com.example.service"));

        String diagram = generator.generateArchitectureDiagram(packages);

        // Verify controller comes before service, service before repository, repository before model
        int controllerIndex = diagram.indexOf("Controller Layer");
        int serviceIndex = diagram.indexOf("Service Layer");
        int repositoryIndex = diagram.indexOf("Repository Layer");
        int modelIndex = diagram.indexOf("Model Layer");

        assertTrue(controllerIndex < serviceIndex);
        assertTrue(serviceIndex < repositoryIndex);
        assertTrue(repositoryIndex < modelIndex);
    }

    /**
     * Helper method to create a Java file.
     */
    private Path createJavaFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    /**
     * Helper method to create an XML file.
     */
    private Path createXmlFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
