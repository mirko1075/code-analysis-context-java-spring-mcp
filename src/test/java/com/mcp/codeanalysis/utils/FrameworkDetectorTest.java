package com.mcp.codeanalysis.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FrameworkDetector.
 */
class FrameworkDetectorTest {

    @TempDir
    Path tempDir;

    private FrameworkDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FrameworkDetector(tempDir);
    }

    @Test
    void testDetectSpringBootWithAnnotation() throws IOException {
        createSpringBootApplication();

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
    }

    @Test
    void testDetectSpringBootWithPomStarters() throws IOException {
        createPomWithSpringBootStarters();

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
    }

    @Test
    void testDetectSpringBootWithApplicationProperties() throws IOException {
        createFile("src/main/resources/application.properties", "server.port=8080");

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
    }

    @Test
    void testDetectSpringBootWithApplicationYml() throws IOException {
        createFile("src/main/resources/application.yml", "server:\n  port: 8080");

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
    }

    @Test
    void testDetectTraditionalSpringWithXmlConfig() throws IOException {
        createFile("src/main/resources/applicationContext.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userService" class="com.example.UserService"/>
                </beans>
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isTraditionalSpring());
    }

    @Test
    void testDetectSpringMvc() throws IOException {
        createFile("src/main/java/com/example/UserController.java", """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;

                @RestController
                public class UserController {
                    @GetMapping("/users")
                    public String getUsers() {
                        return "users";
                    }
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringMvc());
    }

    @Test
    void testDetectSpringData() throws IOException {
        createFile("src/main/java/com/example/User.java", """
                package com.example;

                import javax.persistence.Entity;
                import javax.persistence.Table;
                import javax.persistence.Id;

                @Entity
                @Table(name = "users")
                public class User {
                    @Id
                    private Long id;
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringData());
    }

    @Test
    void testDetectSpringSecurity() throws IOException {
        createFile("src/main/java/com/example/SecurityConfig.java", """
                package com.example;

                import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                @EnableWebSecurity
                public class SecurityConfig {
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringSecurity());
    }

    @Test
    void testDetectSpringAop() throws IOException {
        createFile("src/main/java/com/example/LoggingAspect.java", """
                package com.example;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Before;

                @Aspect
                public class LoggingAspect {
                    @Before("execution(* com.example.*.*(..))")
                    public void logBefore() {
                    }
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringAop());
    }

    @Test
    void testDetectSpringTransactions() throws IOException {
        createFile("src/main/java/com/example/UserService.java", """
                package com.example;

                import org.springframework.transaction.annotation.Transactional;

                public class UserService {
                    @Transactional
                    public void saveUser() {
                    }
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringTransactions());
    }

    @Test
    void testDetectMavenBuildTool() throws IOException {
        createFile("pom.xml", """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                </project>
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertEquals("Maven", info.getBuildTool());
    }

    @Test
    void testDetectGradleBuildTool() throws IOException {
        createFile("build.gradle", """
                plugins {
                    id 'java'
                }
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertEquals("Gradle", info.getBuildTool());
    }

    @Test
    void testDetectUnknownBuildTool() {
        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertEquals("Unknown", info.getBuildTool());
    }

    @Test
    void testDetectMultipleModules() throws IOException {
        // Create Spring Boot app with multiple modules
        createSpringBootApplication();
        createFile("src/main/java/com/example/UserController.java", """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                }
                """);
        createFile("src/main/java/com/example/User.java", """
                package com.example;

                import javax.persistence.Entity;

                @Entity
                public class User {
                }
                """);
        createFile("pom.xml", "<project></project>");

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
        assertTrue(info.isSpringMvc());
        assertTrue(info.isSpringData());
        assertEquals("Maven", info.getBuildTool());
    }

    @Test
    void testDetectMixedSpringBootAndTraditional() throws IOException {
        // Project with both Spring Boot and XML config (migration scenario)
        createSpringBootApplication();
        createFile("src/main/resources/applicationContext.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                </beans>
                """);

        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertTrue(info.isSpringBoot());
        assertTrue(info.isTraditionalSpring());
    }

    @Test
    void testNoFrameworkDetected() {
        FrameworkDetector.FrameworkInfo info = detector.detect();

        assertFalse(info.isSpringBoot());
        assertFalse(info.isTraditionalSpring());
        assertFalse(info.isSpringMvc());
        assertFalse(info.isSpringData());
        assertFalse(info.isSpringSecurity());
        assertFalse(info.isSpringAop());
        assertFalse(info.isSpringTransactions());
        assertEquals("Unknown", info.getBuildTool());
    }

    @Test
    void testFrameworkInfoToString() throws IOException {
        createSpringBootApplication();

        FrameworkDetector.FrameworkInfo info = detector.detect();

        String toString = info.toString();
        assertTrue(toString.contains("springBoot=true"));
        assertTrue(toString.contains("FrameworkInfo"));
    }

    /**
     * Helper method to create a Spring Boot application class.
     */
    private void createSpringBootApplication() throws IOException {
        createFile("src/main/java/com/example/Application.java", """
                package com.example;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """);
    }

    /**
     * Helper method to create pom.xml with Spring Boot starters.
     */
    private void createPomWithSpringBootStarters() throws IOException {
        createFile("pom.xml", """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);
    }

    /**
     * Helper method to create a file in the temp directory.
     */
    private void createFile(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
