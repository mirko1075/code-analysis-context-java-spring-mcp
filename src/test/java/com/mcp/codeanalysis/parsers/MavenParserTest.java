package com.mcp.codeanalysis.parsers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenParser.
 */
class MavenParserTest {

    private MavenParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new MavenParser();
    }

    @Test
    void testParseSimplePom() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>
                    <packaging>jar</packaging>

                    <name>My Application</name>
                    <description>A simple application</description>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertNotNull(project);
        assertEquals("com.example", project.getGroupId());
        assertEquals("my-app", project.getArtifactId());
        assertEquals("1.0.0", project.getVersion());
        assertEquals("jar", project.getPackaging());
        assertEquals("My Application", project.getName());
        assertEquals("A simple application", project.getDescription());
        assertEquals("com.example:my-app:1.0.0", project.getCoordinates());
    }

    @Test
    void testParsePomWithDependencies() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>3.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.10.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertNotNull(project);
        assertEquals(2, project.getDependencies().size());

        MavenParser.MavenDependency springBoot = project.getDependencies().get(0);
        assertEquals("org.springframework.boot", springBoot.getGroupId());
        assertEquals("spring-boot-starter-web", springBoot.getArtifactId());
        assertEquals("3.2.0", springBoot.getVersion());
        assertEquals("compile", springBoot.getScope());

        MavenParser.MavenDependency junit = project.getDependencies().get(1);
        assertEquals("org.junit.jupiter", junit.getGroupId());
        assertEquals("junit-jupiter", junit.getArtifactId());
        assertEquals("test", junit.getScope());
    }

    @Test
    void testParsePomWithParent() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>

                    <artifactId>my-spring-app</artifactId>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertNotNull(project);
        assertEquals("org.springframework.boot", project.getGroupId()); // Inherited from parent
        assertEquals("my-spring-app", project.getArtifactId());
        assertEquals("3.2.0", project.getVersion()); // Inherited from parent
        assertEquals("org.springframework.boot", project.getParentGroupId());
        assertEquals("spring-boot-starter-parent", project.getParentArtifactId());
        assertEquals("3.2.0", project.getParentVersion());
    }

    @Test
    void testGetDependenciesByScope() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-core</artifactId>
                            <version>6.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.mockito</groupId>
                            <artifactId>mockito-core</artifactId>
                            <version>5.0.0</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>servlet-api</artifactId>
                            <version>2.5</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        List<MavenParser.MavenDependency> compileDeps = project.getDependenciesByScope("compile");
        assertEquals(1, compileDeps.size());
        assertEquals("spring-core", compileDeps.get(0).getArtifactId());

        List<MavenParser.MavenDependency> testDeps = project.getDependenciesByScope("test");
        assertEquals(2, testDeps.size());

        List<MavenParser.MavenDependency> providedDeps = project.getDependenciesByScope("provided");
        assertEquals(1, providedDeps.size());
        assertEquals("servlet-api", providedDeps.get(0).getArtifactId());
    }

    @Test
    void testGetDependenciesByGroupId() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>3.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                            <version>3.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.h2database</groupId>
                            <artifactId>h2</artifactId>
                            <version>2.2.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        List<MavenParser.MavenDependency> springDeps = project.getDependenciesByGroupId("org.springframework.boot");
        assertEquals(2, springDeps.size());

        List<MavenParser.MavenDependency> allSpringDeps = project.getDependenciesByGroupId("org.springframework.*");
        assertEquals(2, allSpringDeps.size());

        List<MavenParser.MavenDependency> h2Deps = project.getDependenciesByGroupId("com.h2database");
        assertEquals(1, h2Deps.size());
    }

    @Test
    void testHasDependency() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>3.2.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertTrue(project.hasDependency("org.springframework.boot", "spring-boot-starter-web"));
        assertFalse(project.hasDependency("org.springframework.boot", "spring-boot-starter-data-jpa"));
        assertFalse(project.hasDependency("junit", "junit"));
    }

    @Test
    void testDependencyWithOptional() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>

                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertEquals(1, project.getDependencies().size());
        MavenParser.MavenDependency lombok = project.getDependencies().get(0);
        assertTrue(lombok.isOptional());
    }

    @Test
    void testDefaultPackaging() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertEquals("jar", project.getPackaging()); // Default packaging
    }

    @Test
    void testWarPackaging() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-webapp</artifactId>
                    <version>1.0.0</version>
                    <packaging>war</packaging>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertEquals("war", project.getPackaging());
    }

    @Test
    void testDependencyCoordinates() {
        MavenParser.MavenDependency dep = new MavenParser.MavenDependency();
        dep.setGroupId("org.springframework");
        dep.setArtifactId("spring-core");
        dep.setVersion("6.0.0");
        dep.setScope("compile");

        assertEquals("org.springframework:spring-core:6.0.0", dep.getCoordinates());
        assertEquals("org.springframework:spring-core:6.0.0", dep.toString());
    }

    @Test
    void testDependencyCoordinatesWithNonCompileScope() {
        MavenParser.MavenDependency dep = new MavenParser.MavenDependency();
        dep.setGroupId("junit");
        dep.setArtifactId("junit");
        dep.setVersion("4.13.2");
        dep.setScope("test");

        assertEquals("junit:junit:4.13.2:test", dep.getCoordinates());
    }

    @Test
    void testParseInvalidPom() throws IOException {
        String invalidPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <this is not valid xml
                </project>
                """;

        Path pomFile = createPomFile(invalidPom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertNull(project);
    }

    @Test
    void testParseNonExistentPom() {
        Path nonExistent = tempDir.resolve("nonexistent-pom.xml");
        MavenParser.MavenProject project = parser.parsePom(nonExistent);

        assertNull(project);
    }

    @Test
    void testEmptyDependencies() throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Path pomFile = createPomFile(pom);
        MavenParser.MavenProject project = parser.parsePom(pomFile);

        assertNotNull(project);
        assertEquals(0, project.getDependencies().size());
    }

    /**
     * Helper method to create a pom.xml file.
     */
    private Path createPomFile(String content) throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, content);
        return pomFile;
    }
}
