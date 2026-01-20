package com.mcp.codeanalysis.utils;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility for detecting Spring Framework and Spring Boot in a project.
 * Detects project type and enabled Spring modules.
 */
public class FrameworkDetector {
    private static final Logger logger = LoggerFactory.getLogger(FrameworkDetector.class);

    private final Path projectRoot;
    private final FileScanner fileScanner;
    private final JavaSourceParser javaParser;

    // Spring Boot indicators
    private static final String SPRING_BOOT_APPLICATION_ANNOTATION = "SpringBootApplication";
    private static final Set<String> SPRING_BOOT_STARTER_ARTIFACTS = Set.of(
            "spring-boot-starter",
            "spring-boot-starter-web",
            "spring-boot-starter-data-jpa",
            "spring-boot-starter-security",
            "spring-boot-starter-test"
    );

    // Traditional Spring indicators
    private static final Set<String> SPRING_XML_CONFIG_FILES = Set.of(
            "applicationContext.xml",
            "servlet-context.xml",
            "spring-context.xml",
            "beans.xml"
    );

    // Spring modules annotations
    private static final Set<String> SPRING_MVC_ANNOTATIONS = Set.of(
            "Controller", "RestController", "RequestMapping",
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping"
    );

    private static final Set<String> SPRING_DATA_ANNOTATIONS = Set.of(
            "Entity", "Repository", "Table", "Column",
            "OneToMany", "ManyToOne", "ManyToMany", "OneToOne"
    );

    private static final Set<String> SPRING_SECURITY_ANNOTATIONS = Set.of(
            "EnableWebSecurity", "Secured", "PreAuthorize", "PostAuthorize",
            "RolesAllowed", "EnableGlobalMethodSecurity"
    );

    private static final Set<String> SPRING_AOP_ANNOTATIONS = Set.of(
            "Aspect", "Before", "After", "Around", "AfterReturning",
            "AfterThrowing", "Pointcut", "EnableAspectJAutoProxy"
    );

    private static final Set<String> SPRING_TRANSACTION_ANNOTATIONS = Set.of(
            "Transactional", "EnableTransactionManagement"
    );

    /**
     * Create a FrameworkDetector for a project.
     *
     * @param projectRoot Project root directory
     */
    public FrameworkDetector(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.fileScanner = new FileScanner(projectRoot);
        this.javaParser = new JavaSourceParser();
    }

    /**
     * Detect framework type and modules in the project.
     *
     * @return FrameworkInfo containing detection results
     */
    public FrameworkInfo detect() {
        FrameworkInfo info = new FrameworkInfo();

        try {
            // Detect Spring Boot
            boolean isSpringBoot = detectSpringBoot();
            info.setSpringBoot(isSpringBoot);

            // Detect Traditional Spring
            boolean isTraditionalSpring = detectTraditionalSpring();
            info.setTraditionalSpring(isTraditionalSpring);

            // Detect Spring modules
            info.setSpringMvc(detectSpringMvc());
            info.setSpringData(detectSpringData());
            info.setSpringSecurity(detectSpringSecurity());
            info.setSpringAop(detectSpringAop());
            info.setSpringTransactions(detectSpringTransactions());

            // Detect build tool
            info.setBuildTool(detectBuildTool());

            logger.info("Framework detection complete: {}", info);

        } catch (Exception e) {
            logger.error("Error detecting frameworks", e);
        }

        return info;
    }

    /**
     * Detect Spring Boot by looking for:
     * - @SpringBootApplication annotation
     * - spring-boot-starter dependencies in pom.xml
     * - application.properties or application.yml
     */
    private boolean detectSpringBoot() throws IOException {
        // Check for @SpringBootApplication annotation
        List<Path> javaFiles = fileScanner.scanJavaFiles();
        for (Path javaFile : javaFiles) {
            JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
            if (fileInfo != null && hasSpringBootApplication(fileInfo)) {
                logger.debug("Found @SpringBootApplication in {}", javaFile);
                return true;
            }
        }

        // Check for Spring Boot starters in pom.xml
        List<Path> pomFiles = fileScanner.findPomFiles();
        for (Path pomFile : pomFiles) {
            if (hasSpringBootStarters(pomFile)) {
                logger.debug("Found Spring Boot starters in {}", pomFile);
                return true;
            }
        }

        // Check for application.properties or application.yml
        List<Path> appConfigs = fileScanner.findApplicationConfigFiles();
        if (!appConfigs.isEmpty()) {
            logger.debug("Found application config files: {}", appConfigs.size());
            // Not definitive, but suggestive of Spring Boot
            return true;
        }

        return false;
    }

    /**
     * Detect Traditional Spring by looking for XML configuration files.
     */
    private boolean detectTraditionalSpring() throws IOException {
        List<Path> xmlFiles = fileScanner.scanXmlFiles();

        for (Path xmlFile : xmlFiles) {
            String fileName = xmlFile.getFileName().toString();
            if (SPRING_XML_CONFIG_FILES.contains(fileName)) {
                logger.debug("Found Spring XML config: {}", xmlFile);
                return true;
            }
        }

        return false;
    }

    /**
     * Detect Spring MVC module.
     */
    private boolean detectSpringMvc() throws IOException {
        return hasAnnotations(SPRING_MVC_ANNOTATIONS);
    }

    /**
     * Detect Spring Data (JPA) module.
     */
    private boolean detectSpringData() throws IOException {
        return hasAnnotations(SPRING_DATA_ANNOTATIONS);
    }

    /**
     * Detect Spring Security module.
     */
    private boolean detectSpringSecurity() throws IOException {
        return hasAnnotations(SPRING_SECURITY_ANNOTATIONS);
    }

    /**
     * Detect Spring AOP module.
     */
    private boolean detectSpringAop() throws IOException {
        return hasAnnotations(SPRING_AOP_ANNOTATIONS);
    }

    /**
     * Detect Spring Transactions.
     */
    private boolean detectSpringTransactions() throws IOException {
        return hasAnnotations(SPRING_TRANSACTION_ANNOTATIONS);
    }

    /**
     * Detect build tool (Maven or Gradle).
     */
    private String detectBuildTool() throws IOException {
        List<Path> pomFiles = fileScanner.findPomFiles();
        if (!pomFiles.isEmpty()) {
            return "Maven";
        }

        List<Path> gradleFiles = fileScanner.findGradleFiles();
        if (!gradleFiles.isEmpty()) {
            return "Gradle";
        }

        return "Unknown";
    }

    /**
     * Check if any Java file has @SpringBootApplication annotation.
     */
    private boolean hasSpringBootApplication(JavaFileInfo fileInfo) {
        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            if (classInfo.getAnnotations().contains(SPRING_BOOT_APPLICATION_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if pom.xml contains Spring Boot starter dependencies.
     */
    private boolean hasSpringBootStarters(Path pomFile) {
        try {
            String content = Files.readString(pomFile);
            for (String starter : SPRING_BOOT_STARTER_ARTIFACTS) {
                if (content.contains(starter)) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading pom.xml: {}", pomFile, e);
        }
        return false;
    }

    /**
     * Check if any Java file has annotations from the given set.
     */
    private boolean hasAnnotations(Set<String> annotations) throws IOException {
        List<Path> javaFiles = fileScanner.scanJavaFiles();

        for (Path javaFile : javaFiles) {
            JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
            if (fileInfo != null && hasAnyAnnotation(fileInfo, annotations)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if JavaFileInfo contains any of the specified annotations.
     */
    private boolean hasAnyAnnotation(JavaFileInfo fileInfo, Set<String> annotations) {
        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            // Check class annotations
            if (classInfo.getAnnotations().stream().anyMatch(annotations::contains)) {
                return true;
            }

            // Check method annotations
            for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                if (method.getAnnotations().stream().anyMatch(annotations::contains)) {
                    return true;
                }
            }

            // Check field annotations
            for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
                if (field.getAnnotations().stream().anyMatch(annotations::contains)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Information about detected frameworks and modules.
     */
    public static class FrameworkInfo {
        private boolean isSpringBoot;
        private boolean isTraditionalSpring;
        private boolean springMvc;
        private boolean springData;
        private boolean springSecurity;
        private boolean springAop;
        private boolean springTransactions;
        private String buildTool;

        public boolean isSpringBoot() {
            return isSpringBoot;
        }

        public void setSpringBoot(boolean springBoot) {
            isSpringBoot = springBoot;
        }

        public boolean isTraditionalSpring() {
            return isTraditionalSpring;
        }

        public void setTraditionalSpring(boolean traditionalSpring) {
            isTraditionalSpring = traditionalSpring;
        }

        public boolean isSpringMvc() {
            return springMvc;
        }

        public void setSpringMvc(boolean springMvc) {
            this.springMvc = springMvc;
        }

        public boolean isSpringData() {
            return springData;
        }

        public void setSpringData(boolean springData) {
            this.springData = springData;
        }

        public boolean isSpringSecurity() {
            return springSecurity;
        }

        public void setSpringSecurity(boolean springSecurity) {
            this.springSecurity = springSecurity;
        }

        public boolean isSpringAop() {
            return springAop;
        }

        public void setSpringAop(boolean springAop) {
            this.springAop = springAop;
        }

        public boolean isSpringTransactions() {
            return springTransactions;
        }

        public void setSpringTransactions(boolean springTransactions) {
            this.springTransactions = springTransactions;
        }

        public String getBuildTool() {
            return buildTool;
        }

        public void setBuildTool(String buildTool) {
            this.buildTool = buildTool;
        }

        @Override
        public String toString() {
            return "FrameworkInfo{" +
                    "springBoot=" + isSpringBoot +
                    ", traditionalSpring=" + isTraditionalSpring +
                    ", springMvc=" + springMvc +
                    ", springData=" + springData +
                    ", springSecurity=" + springSecurity +
                    ", springAop=" + springAop +
                    ", springTransactions=" + springTransactions +
                    ", buildTool='" + buildTool + '\'' +
                    '}';
        }
    }
}
