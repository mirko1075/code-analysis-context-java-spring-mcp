package com.mcp.codeanalysis.analyzers;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.parsers.MavenParser;
import com.mcp.codeanalysis.parsers.YamlPropertiesParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes Spring Boot applications.
 * Detects auto-configuration, starters, and Spring Boot patterns.
 */
public class SpringBootAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(SpringBootAnalyzer.class);

    private final JavaSourceParser javaParser;
    private final MavenParser mavenParser;
    private final YamlPropertiesParser yamlParser;

    // Spring Boot annotations
    private static final Set<String> SPRING_BOOT_ANNOTATIONS = Set.of(
            "SpringBootApplication",
            "EnableAutoConfiguration",
            "SpringBootConfiguration"
    );

    // Common Spring Boot component annotations
    private static final Set<String> COMPONENT_ANNOTATIONS = Set.of(
            "RestController",
            "Controller",
            "Service",
            "Repository",
            "Component",
            "Configuration"
    );

    public SpringBootAnalyzer() {
        this.javaParser = new JavaSourceParser();
        this.mavenParser = new MavenParser();
        this.yamlParser = new YamlPropertiesParser();
    }

    /**
     * Analyze Spring Boot usage in a project.
     *
     * @param javaFiles Java source files
     * @param pomFile pom.xml file (optional)
     * @param configFiles application.yml/properties files
     * @return Analysis result
     */
    public SpringBootAnalysisResult analyze(List<Path> javaFiles, Path pomFile, List<Path> configFiles) {
        SpringBootAnalysisResult result = new SpringBootAnalysisResult();

        // Analyze Java files for Spring Boot annotations
        for (Path javaFile : javaFiles) {
            analyzeJavaFile(javaFile, result);
        }

        // Analyze Maven dependencies
        if (pomFile != null) {
            analyzeMavenDependencies(pomFile, result);
        }

        // Analyze configuration files
        for (Path configFile : configFiles) {
            analyzeConfigFile(configFile, result);
        }

        return result;
    }

    /**
     * Analyze a Java file for Spring Boot patterns.
     */
    private void analyzeJavaFile(Path javaFile, SpringBootAnalysisResult result) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            // Check for Spring Boot application class
            for (String annotation : classInfo.getAnnotations()) {
                if (SPRING_BOOT_ANNOTATIONS.contains(annotation)) {
                    result.setSpringBootDetected(true);
                    String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                    result.setMainClass(fullClassName);
                    result.addAnnotation(annotation);
                }

                // Check for component annotations
                if (COMPONENT_ANNOTATIONS.contains(annotation)) {
                    result.incrementComponentCount();
                    String componentType = annotation;
                    result.addComponent(componentType, fileInfo.getPackageName() + "." + classInfo.getName());
                }

                // Check for specific Spring Boot features
                checkSpringBootFeatures(annotation, result);
            }
        }
    }

    /**
     * Check for specific Spring Boot features from annotations.
     */
    private void checkSpringBootFeatures(String annotation, SpringBootAnalysisResult result) {
        if (annotation.contains("EnableScheduling")) {
            result.addFeature("Scheduling");
        } else if (annotation.contains("EnableAsync")) {
            result.addFeature("Async Processing");
        } else if (annotation.contains("EnableCaching")) {
            result.addFeature("Caching");
        } else if (annotation.contains("EnableJpaRepositories")) {
            result.addFeature("Spring Data JPA");
        } else if (annotation.contains("EnableTransactionManagement")) {
            result.addFeature("Transaction Management");
        } else if (annotation.contains("EnableWebSecurity")) {
            result.addFeature("Spring Security");
        } else if (annotation.contains("EnableFeignClients")) {
            result.addFeature("Feign Clients");
        } else if (annotation.contains("EnableDiscoveryClient") || annotation.contains("EnableEurekaClient")) {
            result.addFeature("Service Discovery");
        } else if (annotation.contains("EnableConfigServer") || annotation.contains("EnableConfigClient")) {
            result.addFeature("Config Server");
        }
    }

    /**
     * Analyze Maven dependencies for Spring Boot starters.
     */
    private void analyzeMavenDependencies(Path pomFile, SpringBootAnalysisResult result) {
        MavenParser.MavenProject project = mavenParser.parsePom(pomFile);
        if (project == null) {
            return;
        }

        List<MavenParser.MavenDependency> dependencies = project.getDependencies();

        for (MavenParser.MavenDependency dep : dependencies) {
            String artifactId = dep.getArtifactId();

            // Detect Spring Boot parent/BOM
            if (dep.getGroupId().equals("org.springframework.boot")) {
                result.setSpringBootDetected(true);

                // Detect starters
                if (artifactId.startsWith("spring-boot-starter-")) {
                    String starter = artifactId.substring("spring-boot-starter-".length());
                    result.addStarter(starter);

                    // Map starters to features
                    mapStarterToFeature(starter, result);
                }
            }
        }
    }

    /**
     * Map starter to corresponding feature.
     */
    private void mapStarterToFeature(String starter, SpringBootAnalysisResult result) {
        switch (starter) {
            case "web":
                result.addFeature("Spring MVC / REST");
                break;
            case "webflux":
                result.addFeature("Reactive Web");
                break;
            case "data-jpa":
                result.addFeature("Spring Data JPA");
                break;
            case "data-mongodb":
                result.addFeature("MongoDB");
                break;
            case "data-redis":
                result.addFeature("Redis");
                break;
            case "security":
                result.addFeature("Spring Security");
                break;
            case "actuator":
                result.addFeature("Actuator");
                break;
            case "test":
                result.addFeature("Testing");
                break;
            case "validation":
                result.addFeature("Validation");
                break;
            case "aop":
                result.addFeature("AOP");
                break;
            case "cache":
                result.addFeature("Caching");
                break;
            case "amqp":
                result.addFeature("RabbitMQ");
                break;
            case "kafka":
                result.addFeature("Kafka");
                break;
            case "mail":
                result.addFeature("Email");
                break;
            case "thymeleaf":
                result.addFeature("Thymeleaf");
                break;
            case "oauth2-client":
            case "oauth2-resource-server":
                result.addFeature("OAuth2");
                break;
            case "cloud-config":
                result.addFeature("Config Server");
                break;
            case "cloud-netflix-eureka-client":
            case "cloud-netflix-eureka-server":
                result.addFeature("Service Discovery");
                break;
        }
    }

    /**
     * Analyze configuration files.
     */
    private void analyzeConfigFile(Path configFile, SpringBootAnalysisResult result) {
        String fileName = configFile.getFileName().toString();

        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            analyzeYamlConfig(configFile, result);
        } else if (fileName.endsWith(".properties")) {
            analyzePropertiesConfig(configFile, result);
        }
    }

    /**
     * Analyze YAML configuration.
     */
    private void analyzeYamlConfig(Path configFile, SpringBootAnalysisResult result) {
        YamlPropertiesParser.ConfigurationData config = yamlParser.parseYaml(configFile);
        if (config == null) {
            return;
        }

        result.addConfigFile(configFile.toString());
        extractConfigurationProperties(config, result);
    }

    /**
     * Analyze properties configuration.
     */
    private void analyzePropertiesConfig(Path configFile, SpringBootAnalysisResult result) {
        YamlPropertiesParser.ConfigurationData config = yamlParser.parseProperties(configFile);
        if (config == null) {
            return;
        }

        result.addConfigFile(configFile.toString());
        extractConfigurationProperties(config, result);
    }

    /**
     * Extract Spring Boot configuration properties.
     */
    private void extractConfigurationProperties(YamlPropertiesParser.ConfigurationData config, SpringBootAnalysisResult result) {
        // Server configuration
        if (config.getServerPort() != null) {
            result.setServerPort(config.getServerPort());
        }

        // Application name
        if (config.getApplicationName() != null) {
            result.setApplicationName(config.getApplicationName());
        }

        // Active profiles
        if (config.getActiveProfiles() != null && !config.getActiveProfiles().isEmpty()) {
            result.setActiveProfiles(config.getActiveProfiles());
        }

        // Datasource configuration
        if (config.getDatasourceUrl() != null) {
            result.setDatasourceConfigured(true);
        }
    }

    /**
     * Spring Boot analysis result.
     */
    public static class SpringBootAnalysisResult {
        private boolean springBootDetected = false;
        private String mainClass;
        private String applicationName;
        private Integer serverPort;
        private boolean datasourceConfigured = false;

        private final Set<String> annotations = new HashSet<>();
        private final Set<String> starters = new HashSet<>();
        private final Set<String> features = new HashSet<>();
        private final List<String> configFiles = new ArrayList<>();
        private final Map<String, List<String>> components = new HashMap<>();
        private List<String> activeProfiles = new ArrayList<>();

        private int componentCount = 0;

        public void setSpringBootDetected(boolean detected) {
            springBootDetected = detected;
        }

        public void setMainClass(String mainClass) {
            this.mainClass = mainClass;
        }

        public void setApplicationName(String name) {
            this.applicationName = name;
        }

        public void setServerPort(Integer port) {
            this.serverPort = port;
        }

        public void setDatasourceConfigured(boolean configured) {
            this.datasourceConfigured = configured;
        }

        public void setActiveProfiles(List<String> profiles) {
            this.activeProfiles = profiles;
        }

        public void addAnnotation(String annotation) {
            annotations.add(annotation);
        }

        public void addStarter(String starter) {
            starters.add(starter);
        }

        public void addFeature(String feature) {
            features.add(feature);
        }

        public void addConfigFile(String file) {
            configFiles.add(file);
        }

        public void addComponent(String type, String className) {
            components.computeIfAbsent(type, k -> new ArrayList<>()).add(className);
        }

        public void incrementComponentCount() {
            componentCount++;
        }

        // Getters

        public boolean isSpringBootDetected() {
            return springBootDetected;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public Integer getServerPort() {
            return serverPort;
        }

        public boolean isDatasourceConfigured() {
            return datasourceConfigured;
        }

        public Set<String> getAnnotations() {
            return new HashSet<>(annotations);
        }

        public Set<String> getStarters() {
            return new HashSet<>(starters);
        }

        public Set<String> getFeatures() {
            return new HashSet<>(features);
        }

        public List<String> getConfigFiles() {
            return new ArrayList<>(configFiles);
        }

        public Map<String, List<String>> getComponents() {
            return new HashMap<>(components);
        }

        public List<String> getActiveProfiles() {
            return new ArrayList<>(activeProfiles);
        }

        public int getComponentCount() {
            return componentCount;
        }

        @Override
        public String toString() {
            return "SpringBootAnalysisResult{" +
                    "detected=" + springBootDetected +
                    ", mainClass='" + mainClass + '\'' +
                    ", starters=" + starters.size() +
                    ", features=" + features.size() +
                    ", components=" + componentCount +
                    '}';
        }
    }
}
