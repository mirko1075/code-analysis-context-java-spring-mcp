package com.mcp.codeanalysis.parsers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YamlPropertiesParser.
 */
class YamlPropertiesParserTest {

    private YamlPropertiesParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new YamlPropertiesParser();
    }

    @Test
    void testParseSimpleYaml() throws IOException {
        String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: my-app
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("yaml", config.getFormat());
        assertNull(config.getProfile());
        assertEquals("8080", config.getProperty("server.port"));
        assertEquals("my-app", config.getProperty("spring.application.name"));
        assertEquals(8080, config.getServerPort());
        assertEquals("my-app", config.getApplicationName());
    }

    @Test
    void testParseNestedYaml() throws IOException {
        String yaml = """
                spring:
                  datasource:
                    url: jdbc:mysql://localhost:3306/mydb
                    username: root
                    password: secret
                    driver-class-name: com.mysql.cj.jdbc.Driver
                  jpa:
                    hibernate:
                      ddl-auto: update
                    show-sql: true
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("jdbc:mysql://localhost:3306/mydb", config.getProperty("spring.datasource.url"));
        assertEquals("root", config.getProperty("spring.datasource.username"));
        assertEquals("com.mysql.cj.jdbc.Driver", config.getProperty("spring.datasource.driver-class-name"));
        assertEquals("update", config.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", config.getProperty("spring.jpa.show-sql"));

        // Test extracted datasource properties
        assertEquals("jdbc:mysql://localhost:3306/mydb", config.getDatasourceUrl());
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDatasourceDriver());
    }

    @Test
    void testParseYamlWithLists() throws IOException {
        String yaml = """
                spring:
                  profiles:
                    active: dev,test
                  security:
                    user:
                      roles:
                        - ADMIN
                        - USER
                        - GUEST
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("dev,test", config.getProperty("spring.profiles.active"));
        assertEquals("ADMIN", config.getProperty("spring.security.user.roles[0]"));
        assertEquals("USER", config.getProperty("spring.security.user.roles[1]"));
        assertEquals("GUEST", config.getProperty("spring.security.user.roles[2]"));

        // Test extracted active profiles
        List<String> activeProfiles = config.getActiveProfiles();
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.contains("dev"));
        assertTrue(activeProfiles.contains("test"));
    }

    @Test
    void testParseYamlWithComplexLists() throws IOException {
        String yaml = """
                logging:
                  level:
                    root: INFO
                    com.example: DEBUG
                  pattern:
                    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
                endpoints:
                  - name: api1
                    url: http://localhost:8081
                  - name: api2
                    url: http://localhost:8082
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("INFO", config.getProperty("logging.level.root"));
        assertEquals("DEBUG", config.getProperty("logging.level.com.example"));
        assertEquals("api1", config.getProperty("endpoints[0].name"));
        assertEquals("http://localhost:8081", config.getProperty("endpoints[0].url"));
        assertEquals("api2", config.getProperty("endpoints[1].name"));
        assertEquals("http://localhost:8082", config.getProperty("endpoints[1].url"));
    }

    @Test
    void testParseSimpleProperties() throws IOException {
        String props = """
                server.port=8080
                spring.application.name=my-app
                server.servlet.context-path=/api
                """;

        Path propsFile = createFile("application.properties", props);
        YamlPropertiesParser.ConfigurationData config = parser.parseProperties(propsFile);

        assertNotNull(config);
        assertEquals("properties", config.getFormat());
        assertNull(config.getProfile());
        assertEquals("8080", config.getProperty("server.port"));
        assertEquals("my-app", config.getProperty("spring.application.name"));
        assertEquals("/api", config.getProperty("server.servlet.context-path"));

        // Test extracted properties
        assertEquals(8080, config.getServerPort());
        assertEquals("my-app", config.getApplicationName());
        assertEquals("/api", config.getContextPath());
    }

    @Test
    void testParseDatasourceProperties() throws IOException {
        String props = """
                spring.datasource.url=jdbc:postgresql://localhost:5432/testdb
                spring.datasource.username=postgres
                spring.datasource.password=admin
                spring.datasource.driver-class-name=org.postgresql.Driver
                """;

        Path propsFile = createFile("application.properties", props);
        YamlPropertiesParser.ConfigurationData config = parser.parseProperties(propsFile);

        assertNotNull(config);
        assertEquals("jdbc:postgresql://localhost:5432/testdb", config.getDatasourceUrl());
        assertEquals("org.postgresql.Driver", config.getDatasourceDriver());
    }

    @Test
    void testParseProfileSpecificYaml() throws IOException {
        String yaml = """
                server:
                  port: 9090
                spring:
                  application:
                    name: dev-app
                """;

        Path yamlFile = createFile("application-dev.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("dev", config.getProfile());
        assertEquals("9090", config.getProperty("server.port"));
    }

    @Test
    void testParseProfileSpecificProperties() throws IOException {
        String props = """
                server.port=9090
                spring.application.name=prod-app
                """;

        Path propsFile = createFile("application-prod.properties", props);
        YamlPropertiesParser.ConfigurationData config = parser.parseProperties(propsFile);

        assertNotNull(config);
        assertEquals("prod", config.getProfile());
        assertEquals("9090", config.getProperty("server.port"));
        assertEquals("prod-app", config.getApplicationName());
    }

    @Test
    void testParseConfigFileYaml() throws IOException {
        String yaml = """
                server:
                  port: 8080
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseConfigFile(yamlFile);

        assertNotNull(config);
        assertEquals("yaml", config.getFormat());
        assertEquals("8080", config.getProperty("server.port"));
    }

    @Test
    void testParseConfigFileProperties() throws IOException {
        String props = "server.port=8080";

        Path propsFile = createFile("application.properties", props);
        YamlPropertiesParser.ConfigurationData config = parser.parseConfigFile(propsFile);

        assertNotNull(config);
        assertEquals("properties", config.getFormat());
        assertEquals("8080", config.getProperty("server.port"));
    }

    @Test
    void testParseNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.yml");
        YamlPropertiesParser.ConfigurationData config = parser.parseConfigFile(nonExistent);

        assertNull(config);
    }

    @Test
    void testParseInvalidYaml() throws IOException {
        String invalidYaml = """
                server:
                  port: not-a-number
                """;

        Path yamlFile = createFile("application.yml", invalidYaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        // Should parse YAML successfully but fail to parse port as integer
        assertNotNull(config);
        assertEquals("not-a-number", config.getProperty("server.port"));
        assertNull(config.getServerPort()); // Port extraction should fail gracefully
    }

    @Test
    void testParseMalformedYaml() throws IOException {
        String malformedYaml = """
                server:
                  port: [unclosed bracket
                """;

        Path yamlFile = createFile("application.yml", malformedYaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        // Malformed YAML should return null
        assertNull(config);
    }

    @Test
    void testParseUnsupportedFormat() throws IOException {
        Path jsonFile = createFile("application.json", "{}");
        YamlPropertiesParser.ConfigurationData config = parser.parseConfigFile(jsonFile);

        assertNull(config);
    }

    @Test
    void testGetPropertyWithDefault() throws IOException {
        String yaml = """
                server:
                  port: 8080
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertEquals("8080", config.getProperty("server.port", "3000"));
        assertEquals("3000", config.getProperty("server.missing", "3000"));
    }

    @Test
    void testHasProperty() throws IOException {
        String yaml = """
                server:
                  port: 8080
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertTrue(config.hasProperty("server.port"));
        assertFalse(config.hasProperty("server.missing"));
    }

    @Test
    void testGetPropertiesWithPrefix() throws IOException {
        String yaml = """
                spring:
                  datasource:
                    url: jdbc:mysql://localhost:3306/mydb
                    username: root
                    password: secret
                  application:
                    name: my-app
                server:
                  port: 8080
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        Map<String, String> datasourceProps = config.getPropertiesWithPrefix("spring.datasource");
        assertEquals(3, datasourceProps.size());
        assertTrue(datasourceProps.containsKey("spring.datasource.url"));
        assertTrue(datasourceProps.containsKey("spring.datasource.username"));
        assertTrue(datasourceProps.containsKey("spring.datasource.password"));
        assertFalse(datasourceProps.containsKey("spring.application.name"));

        Map<String, String> springProps = config.getPropertiesWithPrefix("spring");
        assertEquals(4, springProps.size());
    }

    @Test
    void testConfigurationDataToString() throws IOException {
        String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: my-app
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        String toString = config.toString();
        assertTrue(toString.contains("ConfigurationData"));
        assertTrue(toString.contains("format='yaml'"));
        assertTrue(toString.contains("applicationName='my-app'"));
        assertTrue(toString.contains("serverPort=8080"));
    }

    @Test
    void testEmptyYamlFile() throws IOException {
        String yaml = "";

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertEquals("yaml", config.getFormat());
        assertEquals(0, config.getProperties().size());
    }

    @Test
    void testEmptyPropertiesFile() throws IOException {
        String props = "";

        Path propsFile = createFile("application.properties", props);
        YamlPropertiesParser.ConfigurationData config = parser.parseProperties(propsFile);

        assertNotNull(config);
        assertEquals("properties", config.getFormat());
        assertEquals(0, config.getProperties().size());
    }

    @Test
    void testYamlWithNullValues() throws IOException {
        String yaml = """
                server:
                  port: 8080
                  address:
                spring:
                  application:
                    name: my-app
                """;

        Path yamlFile = createFile("application.yml", yaml);
        YamlPropertiesParser.ConfigurationData config = parser.parseYaml(yamlFile);

        assertNotNull(config);
        assertTrue(config.hasProperty("server.address"));
        assertEquals("", config.getProperty("server.address")); // null becomes empty string
    }

    /**
     * Helper method to create a file in the temp directory.
     */
    private Path createFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
