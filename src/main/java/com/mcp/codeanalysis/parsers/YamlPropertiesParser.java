package com.mcp.codeanalysis.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser for Spring Boot application configuration files.
 * Supports YAML (.yml, .yaml) and Properties (.properties) formats.
 */
public class YamlPropertiesParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlPropertiesParser.class);

    private final ObjectMapper yamlMapper;

    public YamlPropertiesParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Parse a configuration file (YAML or Properties).
     *
     * @param filePath Path to configuration file
     * @return ConfigurationData containing parsed properties
     */
    public ConfigurationData parseConfigFile(Path filePath) {
        if (!Files.exists(filePath)) {
            logger.error("Configuration file does not exist: {}", filePath);
            return null;
        }

        String fileName = filePath.getFileName().toString();

        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return parseYaml(filePath);
        } else if (fileName.endsWith(".properties")) {
            return parseProperties(filePath);
        } else {
            logger.warn("Unsupported configuration file format: {}", fileName);
            return null;
        }
    }

    /**
     * Parse YAML configuration file.
     *
     * @param filePath Path to YAML file
     * @return ConfigurationData
     */
    public ConfigurationData parseYaml(Path filePath) {
        try {
            ConfigurationData config = new ConfigurationData(filePath.toString());
            config.setFormat("yaml");

            // Extract profile if present in filename (e.g., application-dev.yml)
            String fileName = filePath.getFileName().toString();
            String profile = extractProfile(fileName);
            if (profile != null) {
                config.setProfile(profile);
            }

            // Check if file is empty
            String content = Files.readString(filePath);
            if (content == null || content.trim().isEmpty()) {
                // Return empty config for empty files
                return config;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> yamlData = yamlMapper.readValue(filePath.toFile(), Map.class);

            // Handle null or empty YAML data
            if (yamlData == null) {
                return config;
            }

            // Flatten the nested YAML structure into dot-notation properties
            Map<String, String> flattenedProperties = flattenYaml(yamlData, "");
            config.setProperties(flattenedProperties);

            // Extract Spring-specific properties
            extractSpringProperties(flattenedProperties, config);

            return config;

        } catch (IOException e) {
            logger.error("Error parsing YAML file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Parse Properties configuration file.
     *
     * @param filePath Path to properties file
     * @return ConfigurationData
     */
    public ConfigurationData parseProperties(Path filePath) {
        try {
            Properties props = new Properties();

            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                props.load(fis);
            }

            ConfigurationData config = new ConfigurationData(filePath.toString());
            config.setFormat("properties");

            // Extract profile if present in filename
            String fileName = filePath.getFileName().toString();
            String profile = extractProfile(fileName);
            if (profile != null) {
                config.setProfile(profile);
            }

            // Convert Properties to Map
            Map<String, String> propertyMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                propertyMap.put(key, props.getProperty(key));
            }
            config.setProperties(propertyMap);

            // Extract Spring-specific properties
            extractSpringProperties(propertyMap, config);

            return config;

        } catch (IOException e) {
            logger.error("Error parsing properties file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Extract profile name from filename (e.g., "dev" from "application-dev.yml").
     *
     * @param fileName Configuration file name
     * @return Profile name or null if not profile-specific
     */
    private String extractProfile(String fileName) {
        // Pattern: application-{profile}.yml or application-{profile}.properties
        if (fileName.startsWith("application-")) {
            String profilePart = fileName.substring("application-".length());
            int dotIndex = profilePart.lastIndexOf('.');
            if (dotIndex > 0) {
                return profilePart.substring(0, dotIndex);
            }
        }
        return null;
    }

    /**
     * Flatten nested YAML structure into dot-notation properties.
     *
     * @param data YAML data structure
     * @param prefix Current property prefix
     * @return Flattened property map
     */
    private Map<String, String> flattenYaml(Map<String, Object> data, String prefix) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.putAll(flattenYaml(nestedMap, key));
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        result.putAll(flattenYaml(itemMap, key + "[" + i + "]"));
                    } else {
                        result.put(key + "[" + i + "]", String.valueOf(item));
                    }
                }
            } else {
                result.put(key, value != null ? String.valueOf(value) : "");
            }
        }

        return result;
    }

    /**
     * Extract commonly used Spring Boot properties.
     *
     * @param properties All properties
     * @param config Configuration data to populate
     */
    private void extractSpringProperties(Map<String, String> properties, ConfigurationData config) {
        // Server properties
        if (properties.containsKey("server.port")) {
            try {
                config.setServerPort(Integer.parseInt(properties.get("server.port")));
            } catch (NumberFormatException e) {
                logger.warn("Invalid server.port value: {}", properties.get("server.port"));
            }
        }
        if (properties.containsKey("server.servlet.context-path")) {
            config.setContextPath(properties.get("server.servlet.context-path"));
        }

        // Application name
        if (properties.containsKey("spring.application.name")) {
            config.setApplicationName(properties.get("spring.application.name"));
        }

        // Active profiles
        if (properties.containsKey("spring.profiles.active")) {
            String activeProfiles = properties.get("spring.profiles.active");
            config.setActiveProfiles(Arrays.asList(activeProfiles.split(",")));
        }

        // Datasource properties
        if (properties.containsKey("spring.datasource.url")) {
            config.setDatasourceUrl(properties.get("spring.datasource.url"));
        }
        if (properties.containsKey("spring.datasource.driver-class-name")) {
            config.setDatasourceDriver(properties.get("spring.datasource.driver-class-name"));
        }
    }

    /**
     * Data class representing parsed configuration.
     */
    public static class ConfigurationData {
        private final String filePath;
        private String format; // "yaml" or "properties"
        private String profile; // e.g., "dev", "prod"
        private Map<String, String> properties = new HashMap<>();

        // Common Spring Boot properties
        private String applicationName;
        private Integer serverPort;
        private String contextPath;
        private List<String> activeProfiles = new ArrayList<>();
        private String datasourceUrl;
        private String datasourceDriver;

        public ConfigurationData(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public Integer getServerPort() {
            return serverPort;
        }

        public void setServerPort(Integer serverPort) {
            this.serverPort = serverPort;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public List<String> getActiveProfiles() {
            return activeProfiles;
        }

        public void setActiveProfiles(List<String> activeProfiles) {
            this.activeProfiles = activeProfiles;
        }

        public String getDatasourceUrl() {
            return datasourceUrl;
        }

        public void setDatasourceUrl(String datasourceUrl) {
            this.datasourceUrl = datasourceUrl;
        }

        public String getDatasourceDriver() {
            return datasourceDriver;
        }

        public void setDatasourceDriver(String datasourceDriver) {
            this.datasourceDriver = datasourceDriver;
        }

        /**
         * Get a property value by key.
         *
         * @param key Property key (dot notation)
         * @return Property value or null if not found
         */
        public String getProperty(String key) {
            return properties.get(key);
        }

        /**
         * Get a property value with a default.
         *
         * @param key Property key
         * @param defaultValue Default value if property not found
         * @return Property value or default
         */
        public String getProperty(String key, String defaultValue) {
            return properties.getOrDefault(key, defaultValue);
        }

        /**
         * Check if a property exists.
         *
         * @param key Property key
         * @return true if property exists
         */
        public boolean hasProperty(String key) {
            return properties.containsKey(key);
        }

        /**
         * Get all properties with a specific prefix.
         *
         * @param prefix Property prefix (e.g., "spring.datasource")
         * @return Map of matching properties
         */
        public Map<String, String> getPropertiesWithPrefix(String prefix) {
            Map<String, String> result = new HashMap<>();
            String prefixWithDot = prefix.endsWith(".") ? prefix : prefix + ".";

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getKey().startsWith(prefixWithDot)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }

            return result;
        }

        @Override
        public String toString() {
            return "ConfigurationData{" +
                    "filePath='" + filePath + '\'' +
                    ", format='" + format + '\'' +
                    ", profile='" + profile + '\'' +
                    ", properties=" + properties.size() +
                    ", applicationName='" + applicationName + '\'' +
                    ", serverPort=" + serverPort +
                    '}';
        }
    }
}
