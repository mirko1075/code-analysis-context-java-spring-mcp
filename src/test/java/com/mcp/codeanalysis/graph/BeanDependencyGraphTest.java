package com.mcp.codeanalysis.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BeanDependencyGraph.
 */
class BeanDependencyGraphTest {

    @TempDir
    Path tempDir;

    private BeanDependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new BeanDependencyGraph();
    }

    @Test
    void testAddXmlBeans() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userService" class="com.example.service.UserService">
                        <property name="userRepository" ref="userRepository"/>
                    </bean>
                    <bean id="userRepository" class="com.example.repository.UserRepository"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        graph.addXmlConfig(xmlFile);

        Set<String> beans = graph.getBeans();
        assertTrue(beans.contains("userService"));
        assertTrue(beans.contains("userRepository"));

        assertEquals(2, graph.getBeanCount());
        assertEquals(1, graph.getDependencyCount()); // userService -> userRepository
    }

    @Test
    void testXmlBeanDependencies() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="controller" class="com.example.controller.UserController">
                        <property name="service" ref="userService"/>
                    </bean>
                    <bean id="userService" class="com.example.service.UserService">
                        <property name="repository" ref="userRepository"/>
                    </bean>
                    <bean id="userRepository" class="com.example.repository.UserRepository"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        graph.addXmlConfig(xmlFile);

        Set<String> serviceDeps = graph.getDependenciesOf("userService");
        assertTrue(serviceDeps.contains("userRepository"));

        Set<String> controllerDeps = graph.getDependenciesOf("controller");
        assertTrue(controllerDeps.contains("userService"));

        Set<String> repositoryDependents = graph.getDependentsOf("userRepository");
        assertTrue(repositoryDependents.contains("userService"));
    }

    @Test
    void testAnnotatedBeans() throws IOException {
        String serviceSource = """
                package com.example.service;

                import org.springframework.stereotype.Service;
                import org.springframework.beans.factory.annotation.Autowired;
                import com.example.repository.UserRepository;

                @Service
                public class UserService {
                    @Autowired
                    private UserRepository userRepository;
                }
                """;

        String repositorySource = """
                package com.example.repository;

                import org.springframework.stereotype.Repository;

                @Repository
                public class UserRepository {
                }
                """;

        graph.addJavaFile(createJavaFile("UserService.java", serviceSource));
        graph.addJavaFile(createJavaFile("UserRepository.java", repositorySource));

        Set<String> beans = graph.getBeans();
        assertTrue(beans.contains("userService"));
        assertTrue(beans.contains("userRepository"));

        Set<String> deps = graph.getDependenciesOf("userService");
        assertTrue(deps.contains("userRepository"));
    }

    @Test
    void testComponentAnnotations() throws IOException {
        String componentSource = """
                package com.example.util;

                import org.springframework.stereotype.Component;

                @Component
                public class MyComponent {
                }
                """;

        String controllerSource = """
                package com.example.controller;

                import org.springframework.stereotype.Controller;

                @Controller
                public class MyController {
                }
                """;

        String restControllerSource = """
                package com.example.api;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class MyRestController {
                }
                """;

        graph.addJavaFile(createJavaFile("MyComponent.java", componentSource));
        graph.addJavaFile(createJavaFile("MyController.java", controllerSource));
        graph.addJavaFile(createJavaFile("MyRestController.java", restControllerSource));

        Set<String> beans = graph.getBeans();
        assertTrue(beans.contains("myComponent"));
        assertTrue(beans.contains("myController"));
        assertTrue(beans.contains("myRestController"));
    }

    @Test
    void testSetterInjection() throws IOException {
        String source = """
                package com.example.service;

                import org.springframework.stereotype.Service;
                import org.springframework.beans.factory.annotation.Autowired;
                import com.example.repository.UserRepository;

                @Service
                public class UserService {
                    private UserRepository userRepository;

                    @Autowired
                    public void setUserRepository(UserRepository userRepository) {
                        this.userRepository = userRepository;
                    }
                }
                """;

        graph.addJavaFile(createJavaFile("UserService.java", source));

        Set<String> deps = graph.getDependenciesOf("userService");
        assertTrue(deps.contains("userRepository"));
    }

    @Test
    void testCircularDependenciesXml() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="beanA" class="com.example.BeanA">
                        <property name="beanB" ref="beanB"/>
                    </bean>
                    <bean id="beanB" class="com.example.BeanB">
                        <property name="beanA" ref="beanA"/>
                    </bean>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        graph.addXmlConfig(xmlFile);

        List<List<String>> cycles = graph.detectCircularDependencies();

        assertFalse(cycles.isEmpty());
        // Should detect a cycle between beanA and beanB
    }

    @Test
    void testNoCircularDependencies() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="controller" class="com.example.controller.UserController">
                        <property name="service" ref="userService"/>
                    </bean>
                    <bean id="userService" class="com.example.service.UserService">
                        <property name="repository" ref="userRepository"/>
                    </bean>
                    <bean id="userRepository" class="com.example.repository.UserRepository"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        graph.addXmlConfig(xmlFile);

        List<List<String>> cycles = graph.detectCircularDependencies();
        assertTrue(cycles.isEmpty());
    }

    @Test
    void testBeanInfo() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userService" class="com.example.service.UserService"/>
                </beans>
                """;

        String annotatedSource = """
                package com.example.repository;

                import org.springframework.stereotype.Repository;

                @Repository
                public class UserRepository {
                }
                """;

        graph.addXmlConfig(createXmlFile("applicationContext.xml", xml));
        graph.addJavaFile(createJavaFile("UserRepository.java", annotatedSource));

        BeanDependencyGraph.BeanInfo xmlBean = graph.getBeanInfo("userService");
        assertNotNull(xmlBean);
        assertEquals("userService", xmlBean.getBeanId());
        assertEquals("com.example.service.UserService", xmlBean.getClassName());
        assertTrue(xmlBean.isXmlDefined());
        assertFalse(xmlBean.isAnnotationDefined());

        BeanDependencyGraph.BeanInfo annotatedBean = graph.getBeanInfo("userRepository");
        assertNotNull(annotatedBean);
        assertEquals("userRepository", annotatedBean.getBeanId());
        assertTrue(annotatedBean.getClassName().contains("UserRepository"));
        assertTrue(annotatedBean.isAnnotationDefined());
        assertFalse(annotatedBean.isXmlDefined());
    }

    @Test
    void testGetAllBeanInfo() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="bean1" class="com.example.Bean1"/>
                    <bean id="bean2" class="com.example.Bean2"/>
                </beans>
                """;

        graph.addXmlConfig(createXmlFile("applicationContext.xml", xml));

        Map<String, BeanDependencyGraph.BeanInfo> allBeans = graph.getAllBeanInfo();
        assertEquals(2, allBeans.size());
        assertTrue(allBeans.containsKey("bean1"));
        assertTrue(allBeans.containsKey("bean2"));
    }

    @Test
    void testMixedXmlAndAnnotations() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userService" class="com.example.service.UserService">
                        <property name="repository" ref="userRepository"/>
                    </bean>
                </beans>
                """;

        String repositorySource = """
                package com.example.repository;

                import org.springframework.stereotype.Repository;

                @Repository
                public class UserRepository {
                }
                """;

        graph.addXmlConfig(createXmlFile("applicationContext.xml", xml));
        graph.addJavaFile(createJavaFile("UserRepository.java", repositorySource));

        assertTrue(graph.getBeans().contains("userService"));
        assertTrue(graph.getBeans().contains("userRepository"));

        Set<String> deps = graph.getDependenciesOf("userService");
        assertTrue(deps.contains("userRepository"));
    }

    @Test
    void testEmptyXmlFile() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                </beans>
                """;

        graph.addXmlConfig(createXmlFile("applicationContext.xml", xml));

        assertEquals(0, graph.getBeanCount());
        assertEquals(0, graph.getDependencyCount());
    }

    @Test
    void testNonBeanJavaClass() throws IOException {
        String source = """
                package com.example.model;

                public class User {
                    private String name;
                }
                """;

        graph.addJavaFile(createJavaFile("User.java", source));

        // Should not add non-annotated classes as beans
        assertFalse(graph.getBeans().contains("user"));
        assertEquals(0, graph.getBeanCount());
    }

    @Test
    void testBeanInfoToString() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="myBean" class="com.example.MyBean"/>
                </beans>
                """;

        graph.addXmlConfig(createXmlFile("applicationContext.xml", xml));

        BeanDependencyGraph.BeanInfo beanInfo = graph.getBeanInfo("myBean");
        String toString = beanInfo.toString();

        assertTrue(toString.contains("myBean"));
        assertTrue(toString.contains("com.example.MyBean"));
        assertTrue(toString.contains("XML"));
    }

    @Test
    void testMultipleXmlFiles() throws IOException {
        String xml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="bean1" class="com.example.Bean1"/>
                </beans>
                """;

        String xml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="bean2" class="com.example.Bean2"/>
                </beans>
                """;

        List<Path> xmlFiles = List.of(
                createXmlFile("context1.xml", xml1),
                createXmlFile("context2.xml", xml2)
        );

        graph.addXmlConfigs(xmlFiles);

        assertEquals(2, graph.getBeanCount());
        assertTrue(graph.getBeans().contains("bean1"));
        assertTrue(graph.getBeans().contains("bean2"));
    }

    @Test
    void testMultipleJavaFiles() throws IOException {
        List<Path> javaFiles = List.of(
                createJavaFile("Service1.java", """
                        package com.example.service;
                        import org.springframework.stereotype.Service;
                        @Service
                        public class Service1 {}
                        """),
                createJavaFile("Service2.java", """
                        package com.example.service;
                        import org.springframework.stereotype.Service;
                        @Service
                        public class Service2 {}
                        """)
        );

        graph.addJavaFiles(javaFiles);

        assertEquals(2, graph.getBeanCount());
        assertTrue(graph.getBeans().contains("service1"));
        assertTrue(graph.getBeans().contains("service2"));
    }

    @Test
    void testGetDependenciesOfNonExistentBean() {
        Set<String> deps = graph.getDependenciesOf("nonExistent");
        assertTrue(deps.isEmpty());
    }

    @Test
    void testGetDependentsOfNonExistentBean() {
        Set<String> dependents = graph.getDependentsOf("nonExistent");
        assertTrue(dependents.isEmpty());
    }

    /**
     * Helper method to create an XML file.
     */
    private Path createXmlFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    /**
     * Helper method to create a Java file.
     */
    private Path createJavaFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
