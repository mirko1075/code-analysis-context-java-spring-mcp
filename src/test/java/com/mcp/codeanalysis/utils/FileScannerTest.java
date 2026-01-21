package com.mcp.codeanalysis.utils;

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
 * Unit tests for FileScanner.
 */
class FileScannerTest {

    @TempDir
    Path tempDir;

    private FileScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new FileScanner(tempDir);
    }

    @Test
    void testScanJavaFiles() throws IOException {
        // Create test files
        createFile("src/main/java/Test.java");
        createFile("src/test/java/TestTest.java");
        createFile("README.md");

        List<Path> javaFiles = scanner.scanJavaFiles();

        assertEquals(2, javaFiles.size());
        assertTrue(javaFiles.stream().allMatch(p -> p.toString().endsWith(".java")));
    }

    @Test
    void testScanXmlFiles() throws IOException {
        createFile("src/main/resources/applicationContext.xml");
        createFile("src/main/resources/beans.xml");
        createFile("pom.xml");

        List<Path> xmlFiles = scanner.scanXmlFiles();

        assertEquals(3, xmlFiles.size());
        assertTrue(xmlFiles.stream().allMatch(p -> p.toString().endsWith(".xml")));
    }

    @Test
    void testScanYamlFiles() throws IOException {
        createFile("src/main/resources/application.yml");
        createFile("src/main/resources/config.yaml");
        createFile("docker-compose.yml");

        List<Path> yamlFiles = scanner.scanYamlFiles();

        assertEquals(3, yamlFiles.size());
        assertTrue(yamlFiles.stream().anyMatch(p -> p.toString().endsWith(".yml")));
        assertTrue(yamlFiles.stream().anyMatch(p -> p.toString().endsWith(".yaml")));
    }

    @Test
    void testScanPropertiesFiles() throws IOException {
        createFile("src/main/resources/application.properties");
        createFile("src/main/resources/log4j.properties");

        List<Path> propFiles = scanner.scanPropertiesFiles();

        assertEquals(2, propFiles.size());
        assertTrue(propFiles.stream().allMatch(p -> p.toString().endsWith(".properties")));
    }

    @Test
    void testExcludeTargetDirectory() throws IOException {
        createFile("src/main/java/Main.java");
        createFile("target/classes/Main.class");
        createFile("target/generated-sources/Test.java");

        List<Path> javaFiles = scanner.scanJavaFiles();

        // Should only find Main.java, not the one in target/
        assertEquals(1, javaFiles.size());
        assertFalse(javaFiles.get(0).toString().contains("target"));
    }

    @Test
    void testExcludeBuildDirectory() throws IOException {
        createFile("src/main/java/Main.java");
        createFile("build/classes/Main.class");
        createFile("build/generated/Test.java");

        List<Path> javaFiles = scanner.scanJavaFiles();

        assertEquals(1, javaFiles.size());
        assertFalse(javaFiles.get(0).toString().contains("build"));
    }

    @Test
    void testExcludeGitDirectory() throws IOException {
        createFile("src/Main.java");
        createFile(".git/config");
        createFile(".git/objects/test.txt");

        List<Path> allFiles = scanner.scan();

        assertTrue(allFiles.stream().noneMatch(p -> p.toString().contains(".git")));
    }

    @Test
    void testExcludeIdeDirectories() throws IOException {
        createFile("src/Main.java");
        createFile(".idea/workspace.xml");
        createFile(".vscode/settings.json");
        createFile(".eclipse/config");

        List<Path> allFiles = scanner.scan();

        assertTrue(allFiles.stream().noneMatch(p -> p.toString().contains(".idea")));
        assertTrue(allFiles.stream().noneMatch(p -> p.toString().contains(".vscode")));
        assertTrue(allFiles.stream().noneMatch(p -> p.toString().contains(".eclipse")));
    }

    @Test
    void testCustomExcludedDir() throws IOException {
        createFile("src/Main.java");
        createFile("custom/Test.java");

        scanner.addExcludedDir("custom");
        List<Path> javaFiles = scanner.scanJavaFiles();

        assertEquals(1, javaFiles.size());
        assertFalse(javaFiles.get(0).toString().contains("custom"));
    }

    @Test
    void testCustomIncludedExtension() throws IOException {
        createFile("src/Script.groovy");
        createFile("src/Test.java");

        scanner.addIncludedExtension(".groovy");
        List<Path> allFiles = scanner.scan();

        assertTrue(allFiles.stream().anyMatch(p -> p.toString().endsWith(".groovy")));
        assertTrue(allFiles.stream().anyMatch(p -> p.toString().endsWith(".java")));
    }

    @Test
    void testClearExcludedDirs() throws IOException {
        createFile("src/Main.java");
        createFile("target/Test.java");

        scanner.clearExcludedDirs();
        List<Path> javaFiles = scanner.scanJavaFiles();

        // Now target should be included
        assertEquals(2, javaFiles.size());
        assertTrue(javaFiles.stream().anyMatch(p -> p.toString().contains("target")));
    }

    @Test
    void testIncludeGlobs() throws IOException {
        createFile("src/main/java/Main.java");
        createFile("src/test/java/Test.java");

        Set<String> includeGlobs = Set.of("**/main/**");
        FileScanner customScanner = new FileScanner(tempDir, includeGlobs, Set.of());

        List<Path> javaFiles = customScanner.scanJavaFiles();

        assertEquals(1, javaFiles.size());
        assertTrue(javaFiles.get(0).toString().contains("main"));
    }

    @Test
    void testExcludeGlobs() throws IOException {
        createFile("src/main/java/Main.java");
        createFile("src/test/java/Test.java");

        Set<String> excludeGlobs = Set.of("**/test/**");
        FileScanner customScanner = new FileScanner(tempDir, Set.of(), excludeGlobs);

        List<Path> javaFiles = customScanner.scanJavaFiles();

        assertEquals(1, javaFiles.size());
        assertFalse(javaFiles.get(0).toString().contains("test"));
    }

    @Test
    void testFindPomFiles() throws IOException {
        createFile("pom.xml");
        createFile("module1/pom.xml");
        createFile("module2/pom.xml");

        List<Path> pomFiles = scanner.findPomFiles();

        assertEquals(3, pomFiles.size());
        assertTrue(pomFiles.stream().allMatch(p -> p.getFileName().toString().equals("pom.xml")));
    }

    @Test
    void testFindGradleFiles() throws IOException {
        createFile("build.gradle");
        createFile("module/build.gradle");
        createFile("module2/build.gradle.kts");

        List<Path> gradleFiles = scanner.findGradleFiles();

        assertEquals(3, gradleFiles.size());
        assertTrue(gradleFiles.stream().anyMatch(p -> p.toString().endsWith("build.gradle")));
        assertTrue(gradleFiles.stream().anyMatch(p -> p.toString().endsWith("build.gradle.kts")));
    }

    @Test
    void testFindApplicationConfigFiles() throws IOException {
        createFile("src/main/resources/application.properties");
        createFile("src/main/resources/application.yml");
        createFile("src/main/resources/application-dev.properties");
        createFile("src/main/resources/application-prod.yml");

        List<Path> configFiles = scanner.findApplicationConfigFiles();

        assertEquals(4, configFiles.size());
        assertTrue(configFiles.stream().anyMatch(p -> p.toString().contains("application.properties")));
        assertTrue(configFiles.stream().anyMatch(p -> p.toString().contains("application.yml")));
        assertTrue(configFiles.stream().anyMatch(p -> p.toString().contains("application-dev")));
        assertTrue(configFiles.stream().anyMatch(p -> p.toString().contains("application-prod")));
    }

    @Test
    void testFindFilesByNamePattern() throws IOException {
        createFile("src/Controller.java");
        createFile("src/UserController.java");
        createFile("src/Service.java");

        List<Path> controllers = scanner.findFilesByName("*Controller.java");

        // Glob pattern *Controller.java matches both Controller.java and UserController.java
        assertEquals(2, controllers.size());
        assertTrue(controllers.stream().allMatch(p -> p.toString().endsWith("Controller.java")));
    }

    @Test
    void testGetScanStatistics() throws IOException {
        createFile("src/A.java");
        createFile("src/B.java");
        createFile("src/C.java");
        createFile("src/config.xml");
        createFile("src/config.yml");
        createFile("src/app.properties");

        Map<String, Integer> stats = scanner.getScanStatistics();

        assertEquals(3, stats.get(".java"));
        assertEquals(1, stats.get(".xml"));
        assertEquals(1, stats.get(".yml"));
        assertEquals(1, stats.get(".properties"));
    }

    @Test
    void testScanEmptyDirectory() throws IOException {
        List<Path> files = scanner.scan();

        assertTrue(files.isEmpty());
    }

    @Test
    void testScanNonExistentPath() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent");
        FileScanner badScanner = new FileScanner(nonExistent);

        List<Path> files = badScanner.scan();

        assertTrue(files.isEmpty());
    }

    @Test
    void testScanFileInsteadOfDirectory() throws IOException {
        Path file = createFile("test.txt");
        FileScanner badScanner = new FileScanner(file);

        List<Path> files = badScanner.scan();

        assertTrue(files.isEmpty());
    }

    @Test
    void testNestedDirectoryStructure() throws IOException {
        createFile("a/b/c/d/Test.java");
        createFile("a/b/Other.java");
        createFile("a/x/y/z/Deep.java");

        List<Path> javaFiles = scanner.scanJavaFiles();

        assertEquals(3, javaFiles.size());
    }

    @Test
    void testMixedExtensions() throws IOException {
        createFile("src/Test.java");
        createFile("src/test.xml");
        createFile("src/config.yml");
        createFile("src/app.properties");
        createFile("src/script.sh");  // Not in default extensions

        List<Path> allFiles = scanner.scan();

        // Should find 4 files (.java, .xml, .yml, .properties) but not .sh
        assertEquals(4, allFiles.size());
        assertTrue(allFiles.stream().noneMatch(p -> p.toString().endsWith(".sh")));
    }

    @Test
    void testClearIncludedExtensions() throws IOException {
        createFile("src/Test.java");
        createFile("src/script.groovy");

        scanner.clearIncludedExtensions();
        scanner.addIncludedExtension(".groovy");

        List<Path> files = scanner.scan();

        // Should only find .groovy files
        assertEquals(1, files.size());
        assertTrue(files.get(0).toString().endsWith(".groovy"));
    }

    /**
     * Helper method to create a file in the temp directory.
     */
    private Path createFile(String relativePath) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        return file;
    }
}
