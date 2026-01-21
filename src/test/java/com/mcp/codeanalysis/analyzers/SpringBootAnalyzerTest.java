package com.mcp.codeanalysis.analyzers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpringBootAnalyzer.
 */
class SpringBootAnalyzerTest {

    @TempDir
    Path tempDir;

    private SpringBootAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SpringBootAnalyzer();
    }

    @Test
    void testSpringBootApplicationDetection() throws IOException {
        String javaSource = """
                package com.example;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class DemoApplication {
                    public static void main(String[] args) {
                        SpringApplication.run(DemoApplication.class, args);
                    }
                }
                """;

        Path javaFile = createJavaFile("DemoApplication.java", javaSource);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(javaFile), null, List.of());

        assertTrue(result.isSpringBootDetected());
        assertEquals("com.example.DemoApplication", result.getMainClass());
        assertTrue(result.getAnnotations().contains("SpringBootApplication"));
    }

    @Test
    void testComponentDetection() throws IOException {
        String controllerSource = """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;

                @RestController
                public class UserController {
                    @GetMapping("/users")
                    public String getUsers() {
                        return "users";
                    }
                }
                """;

        String serviceSource = """
                package com.example.service;

                import org.springframework.stereotype.Service;

                @Service
                public class UserService {
                }
                """;

        Path controller = createJavaFile("UserController.java", controllerSource);
        Path service = createJavaFile("UserService.java", serviceSource);

        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(controller, service), null, List.of());

        assertEquals(2, result.getComponentCount());
        assertTrue(result.getComponents().containsKey("RestController"));
        assertTrue(result.getComponents().containsKey("Service"));
    }

    @Test
    void testSpringBootFeatureDetection() throws IOException {
        String configSource = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.scheduling.annotation.EnableScheduling;
                import org.springframework.scheduling.annotation.EnableAsync;
                import org.springframework.cache.annotation.EnableCaching;

                @Configuration
                @EnableScheduling
                @EnableAsync
                @EnableCaching
                public class AppConfig {
                }
                """;

        Path configFile = createJavaFile("AppConfig.java", configSource);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(configFile), null, List.of());

        assertTrue(result.getFeatures().contains("Scheduling"));
        assertTrue(result.getFeatures().contains("Async Processing"));
        assertTrue(result.getFeatures().contains("Caching"));
    }

    @Test
    void testMavenStarterDetection() throws IOException {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-security</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createXmlFile("pom.xml", pomXml);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(), pomFile, List.of());

        assertTrue(result.isSpringBootDetected());
        assertTrue(result.getStarters().contains("web"));
        assertTrue(result.getStarters().contains("data-jpa"));
        assertTrue(result.getStarters().contains("security"));

        assertTrue(result.getFeatures().contains("Spring MVC / REST"));
        assertTrue(result.getFeatures().contains("Spring Data JPA"));
        assertTrue(result.getFeatures().contains("Spring Security"));
    }

    @Test
    void testYamlConfigurationParsing() throws IOException {
        String yamlConfig = """
                server:
                  port: 8080
                spring:
                  application:
                    name: demo-app
                  profiles:
                    active: dev,mysql
                  datasource:
                    url: jdbc:mysql://localhost:3306/demo
                    username: root
                """;

        Path yamlFile = createYamlFile("application.yml", yamlConfig);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(), null, List.of(yamlFile));

        assertEquals(8080, result.getServerPort());
        assertEquals("demo-app", result.getApplicationName());
        assertTrue(result.isDatasourceConfigured());
        assertEquals(2, result.getActiveProfiles().size());
        assertTrue(result.getActiveProfiles().contains("dev"));
        assertTrue(result.getActiveProfiles().contains("mysql"));
    }

    @Test
    void testPropertiesConfigurationParsing() throws IOException {
        String propsConfig = """
                server.port=9090
                spring.application.name=my-app
                spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
                """;

        Path propsFile = createTextFile("application.properties", propsConfig);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(), null, List.of(propsFile));

        assertEquals(9090, result.getServerPort());
        assertEquals("my-app", result.getApplicationName());
        assertTrue(result.isDatasourceConfigured());
    }

    @Test
    void testCompleteSpringBootAnalysis() throws IOException {
        // Java file with @SpringBootApplication
        String mainClass = """
                package com.example;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """;

        // Configuration with features
        String configClass = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

                @Configuration
                @EnableJpaRepositories
                public class DatabaseConfig {
                }
                """;

        // pom.xml with starters
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-actuator</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        // application.yml
        String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: complete-app
                """;

        Path main = createJavaFile("Application.java", mainClass);
        Path config = createJavaFile("DatabaseConfig.java", configClass);
        Path pomFile = createXmlFile("pom.xml", pom);
        Path yamlFile = createYamlFile("application.yml", yaml);

        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(main, config), pomFile, List.of(yamlFile));

        assertTrue(result.isSpringBootDetected());
        assertEquals("com.example.Application", result.getMainClass());
        assertTrue(result.getStarters().contains("web"));
        assertTrue(result.getStarters().contains("actuator"));
        assertTrue(result.getFeatures().contains("Spring Data JPA"));
        assertEquals(8080, result.getServerPort());
        assertEquals("complete-app", result.getApplicationName());
    }

    @Test
    void testCloudNativeFeatures() throws IOException {
        String cloudConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
                import org.springframework.cloud.openfeign.EnableFeignClients;

                @Configuration
                @EnableDiscoveryClient
                @EnableFeignClients
                public class CloudConfig {
                }
                """;

        Path configFile = createJavaFile("CloudConfig.java", cloudConfig);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(configFile), null, List.of());

        assertTrue(result.getFeatures().contains("Service Discovery"));
        assertTrue(result.getFeatures().contains("Feign Clients"));
    }

    @Test
    void testMultipleConfigFiles() throws IOException {
        String yaml1 = """
                server:
                  port: 8080
                """;

        String yaml2 = """
                spring:
                  application:
                    name: multi-config-app
                """;

        Path yamlFile1 = createYamlFile("application.yml", yaml1);
        Path yamlFile2 = createYamlFile("application-dev.yml", yaml2);

        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(), null, List.of(yamlFile1, yamlFile2));

        assertEquals(2, result.getConfigFiles().size());
    }

    @Test
    void testNoSpringBootDetected() throws IOException {
        String regularClass = """
                package com.example;

                public class RegularClass {
                    public void doSomething() {
                    }
                }
                """;

        Path javaFile = createJavaFile("RegularClass.java", regularClass);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(javaFile), null, List.of());

        assertFalse(result.isSpringBootDetected());
        assertNull(result.getMainClass());
        assertEquals(0, result.getStarters().size());
        assertEquals(0, result.getFeatures().size());
    }

    @Test
    void testAdditionalStarters() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-mongodb</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-redis</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-amqp</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-cache</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createXmlFile("pom.xml", pom);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(), pomFile, List.of());

        assertTrue(result.getFeatures().contains("MongoDB"));
        assertTrue(result.getFeatures().contains("Redis"));
        assertTrue(result.getFeatures().contains("RabbitMQ"));
        assertTrue(result.getFeatures().contains("Caching"));
    }

    @Test
    void testResultToString() throws IOException {
        String javaSource = """
                package com.example;

                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class App {
                }
                """;

        Path javaFile = createJavaFile("App.java", javaSource);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(javaFile), null, List.of());

        String toString = result.toString();
        assertTrue(toString.contains("detected=true"));
        assertTrue(toString.contains("mainClass='com.example.App'"));
    }

    @Test
    void testResultGettersReturnDefensiveCopies() throws IOException {
        String javaSource = """
                package com.example;

                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class App {
                }
                """;

        Path javaFile = createJavaFile("App.java", javaSource);
        SpringBootAnalyzer.SpringBootAnalysisResult result = analyzer.analyze(
                List.of(javaFile), null, List.of());

        // Modify returned collections
        result.getAnnotations().clear();
        result.getStarters().clear();
        result.getFeatures().clear();
        result.getConfigFiles().clear();
        result.getComponents().clear();

        // Verify original data is preserved
        assertEquals(1, result.getAnnotations().size());
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

    /**
     * Helper method to create a YAML file.
     */
    private Path createYamlFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    /**
     * Helper method to create a text file.
     */
    private Path createTextFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
