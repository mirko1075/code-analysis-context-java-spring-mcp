package com.mcp.codeanalysis.analyzers;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.parsers.XmlConfigParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.types.XmlBeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes Spring Security configurations and patterns.
 * Detects authentication, authorization, security filters, and user management.
 */
public class SecurityAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(SecurityAnalyzer.class);

    private final JavaSourceParser javaParser;
    private final XmlConfigParser xmlParser;

    // Spring Security annotations
    private static final Set<String> SECURITY_ANNOTATIONS = Set.of(
            "EnableWebSecurity",
            "EnableGlobalMethodSecurity",
            "EnableMethodSecurity",
            "Secured",
            "PreAuthorize",
            "PostAuthorize",
            "PreFilter",
            "PostFilter",
            "RolesAllowed"
    );

    // Spring Security configuration classes
    private static final Set<String> SECURITY_CONFIG_TYPES = Set.of(
            "WebSecurityConfigurerAdapter",
            "SecurityFilterChain",
            "AuthenticationManager",
            "UserDetailsService",
            "PasswordEncoder"
    );

    public SecurityAnalyzer() {
        this.javaParser = new JavaSourceParser();
        this.xmlParser = new XmlConfigParser();
    }

    /**
     * Analyze Spring Security usage in a project.
     *
     * @param javaFiles Java source files
     * @param xmlFiles  Spring XML configuration files
     * @return Analysis result
     */
    public SecurityAnalysisResult analyze(List<Path> javaFiles, List<Path> xmlFiles) {
        SecurityAnalysisResult result = new SecurityAnalysisResult();

        // Analyze Java files
        for (Path javaFile : javaFiles) {
            analyzeJavaFile(javaFile, result);
        }

        // Analyze XML files
        for (Path xmlFile : xmlFiles) {
            analyzeXmlConfig(xmlFile, result);
        }

        return result;
    }

    /**
     * Analyze a Java file for Spring Security patterns.
     */
    private void analyzeJavaFile(Path javaFile, SecurityAnalysisResult result) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            // Check for security configuration classes
            boolean isSecConfig = isSecurityConfig(classInfo);
            if (isSecConfig) {
                result.setSecurityEnabled(true);
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                result.addSecurityConfig(fullClassName);
            }

            // Always detect authentication mechanisms and authorization rules
            // (not just for security config classes)
            detectAuthenticationMechanisms(classInfo, result);
            detectAuthorizationRules(classInfo, result);

            // Check for custom user details service
            if (implementsUserDetailsService(classInfo)) {
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                result.addUserDetailsService(fullClassName);
            }

            // Check for password encoder beans
            if (hasPasswordEncoderBean(classInfo)) {
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                result.addPasswordEncoder(fullClassName);
            }

            // Check for security annotations on methods
            for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                for (String annotation : method.getAnnotations()) {
                    if (SECURITY_ANNOTATIONS.contains(annotation)) {
                        result.addSecuredMethod(
                                fileInfo.getPackageName() + "." + classInfo.getName() + "." + method.getName(),
                                annotation
                        );
                    }
                }
            }

            // Check for class-level security annotations
            for (String annotation : classInfo.getAnnotations()) {
                if (SECURITY_ANNOTATIONS.contains(annotation)) {
                    result.setSecurityEnabled(true);
                    result.addSecurityAnnotation(annotation);
                }
            }
        }
    }

    /**
     * Check if class is a Spring Security configuration.
     */
    private boolean isSecurityConfig(JavaFileInfo.ClassInfo classInfo) {
        // Check for @EnableWebSecurity or @EnableGlobalMethodSecurity
        for (String annotation : classInfo.getAnnotations()) {
            if (annotation.equals("EnableWebSecurity") ||
                annotation.equals("EnableGlobalMethodSecurity") ||
                annotation.equals("EnableMethodSecurity")) {
                return true;
            }
        }

        // Check if extends WebSecurityConfigurerAdapter
        if (classInfo.getExtendsClass() != null &&
            classInfo.getExtendsClass().contains("WebSecurityConfigurerAdapter")) {
            return true;
        }

        // Check for SecurityFilterChain beans
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            if (method.getReturnType() != null &&
                method.getReturnType().contains("SecurityFilterChain") &&
                method.getAnnotations().contains("Bean")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect authentication mechanisms.
     */
    private void detectAuthenticationMechanisms(JavaFileInfo.ClassInfo classInfo, SecurityAnalysisResult result) {
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            String methodName = method.getName();
            String returnType = method.getReturnType();

            // Check for authentication manager
            if (returnType != null && returnType.contains("AuthenticationManager")) {
                result.addAuthenticationMechanism("AuthenticationManager");
            }

            // Check for in-memory authentication
            if (methodName.contains("inMemory") || methodName.contains("InMemory")) {
                result.addAuthenticationMechanism("In-Memory Authentication");
            }

            // Check for JDBC authentication
            if (methodName.contains("jdbc") || methodName.contains("Jdbc")) {
                result.addAuthenticationMechanism("JDBC Authentication");
            }

            // Check for LDAP authentication
            if (methodName.contains("ldap") || methodName.contains("Ldap")) {
                result.addAuthenticationMechanism("LDAP Authentication");
            }

            // Check for OAuth2/OIDC
            if (returnType != null && (returnType.contains("OAuth2") || returnType.contains("Oidc"))) {
                result.addAuthenticationMechanism("OAuth2/OIDC");
            }
        }
    }

    /**
     * Detect authorization rules.
     */
    private void detectAuthorizationRules(JavaFileInfo.ClassInfo classInfo, SecurityAnalysisResult result) {
        // Check method-level security
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            String methodSignature = classInfo.getName() + "." + method.getName();

            for (String annotation : method.getAnnotations()) {
                if (annotation.equals("Secured") || annotation.equals("RolesAllowed")) {
                    result.addAuthorizationRule("Role-based: " + methodSignature);
                } else if (annotation.equals("PreAuthorize") || annotation.equals("PostAuthorize")) {
                    result.addAuthorizationRule("Expression-based: " + methodSignature);
                }
            }
        }

        // Check for HttpSecurity configuration
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            if (method.getName().contains("configure") &&
                method.getParameters().stream().anyMatch(p -> p.getType().contains("HttpSecurity"))) {
                result.addAuthorizationRule("HttpSecurity configuration in " + classInfo.getName());
            }
        }
    }

    /**
     * Check if class implements UserDetailsService.
     */
    private boolean implementsUserDetailsService(JavaFileInfo.ClassInfo classInfo) {
        return classInfo.getImplementsInterfaces().stream()
                .anyMatch(iface -> iface.contains("UserDetailsService"));
    }

    /**
     * Check if class has password encoder bean.
     */
    private boolean hasPasswordEncoderBean(JavaFileInfo.ClassInfo classInfo) {
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            if (method.getReturnType() != null &&
                method.getReturnType().contains("PasswordEncoder") &&
                method.getAnnotations().contains("Bean")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyze XML configuration for Spring Security.
     */
    private void analyzeXmlConfig(Path xmlFile, SecurityAnalysisResult result) {
        Map<String, Boolean> namespaces = xmlParser.detectSpringNamespaces(xmlFile);

        // Check for security namespace
        if (namespaces.getOrDefault("security", false)) {
            result.setSecurityEnabled(true);
            result.addSecurityConfig(xmlFile.toString());
        }

        // Parse security beans (always parse, not just when security namespace present)
        List<XmlBeanDefinition> beans = xmlParser.parseXmlConfig(xmlFile);
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            String beanId = bean.getId();

            if (className != null) {
                if (className.contains("UserDetailsService")) {
                    result.addUserDetailsService(beanId);
                    result.setSecurityEnabled(true);  // Security-related bean detected
                } else if (className.contains("PasswordEncoder")) {
                    result.addPasswordEncoder(beanId);
                    result.setSecurityEnabled(true);  // Security-related bean detected
                } else if (className.contains("AuthenticationManager") ||
                           className.contains("ProviderManager") ||
                           (beanId != null && beanId.toLowerCase().contains("authenticationmanager"))) {
                    result.addAuthenticationMechanism("AuthenticationManager (XML)");
                    result.setSecurityEnabled(true);  // Security-related bean detected
                }
            }
        }
    }

    /**
     * Spring Security analysis result.
     */
    public static class SecurityAnalysisResult {
        private boolean securityEnabled = false;
        private final List<String> securityConfigs = new ArrayList<>();
        private final Set<String> securityAnnotations = new HashSet<>();
        private final Set<String> authenticationMechanisms = new HashSet<>();
        private final List<String> authorizationRules = new ArrayList<>();
        private final List<String> userDetailsServices = new ArrayList<>();
        private final List<String> passwordEncoders = new ArrayList<>();
        private final Map<String, String> securedMethods = new HashMap<>();

        public void setSecurityEnabled(boolean enabled) {
            this.securityEnabled = enabled;
        }

        public void addSecurityConfig(String config) {
            this.securityConfigs.add(config);
        }

        public void addSecurityAnnotation(String annotation) {
            this.securityAnnotations.add(annotation);
        }

        public void addAuthenticationMechanism(String mechanism) {
            this.authenticationMechanisms.add(mechanism);
        }

        public void addAuthorizationRule(String rule) {
            this.authorizationRules.add(rule);
        }

        public void addUserDetailsService(String service) {
            this.userDetailsServices.add(service);
        }

        public void addPasswordEncoder(String encoder) {
            this.passwordEncoders.add(encoder);
        }

        public void addSecuredMethod(String method, String annotation) {
            this.securedMethods.put(method, annotation);
        }

        // Getters

        public boolean isSecurityEnabled() {
            return securityEnabled;
        }

        public List<String> getSecurityConfigs() {
            return new ArrayList<>(securityConfigs);
        }

        public Set<String> getSecurityAnnotations() {
            return new HashSet<>(securityAnnotations);
        }

        public Set<String> getAuthenticationMechanisms() {
            return new HashSet<>(authenticationMechanisms);
        }

        public List<String> getAuthorizationRules() {
            return new ArrayList<>(authorizationRules);
        }

        public List<String> getUserDetailsServices() {
            return new ArrayList<>(userDetailsServices);
        }

        public List<String> getPasswordEncoders() {
            return new ArrayList<>(passwordEncoders);
        }

        public Map<String, String> getSecuredMethods() {
            return new HashMap<>(securedMethods);
        }

        @Override
        public String toString() {
            return "SecurityAnalysisResult{" +
                    "enabled=" + securityEnabled +
                    ", configs=" + securityConfigs.size() +
                    ", authMechanisms=" + authenticationMechanisms.size() +
                    ", authzRules=" + authorizationRules.size() +
                    ", securedMethods=" + securedMethods.size() +
                    '}';
        }
    }
}
