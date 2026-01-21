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
 * Unit tests for SecurityAnalyzer.
 */
class SecurityAnalyzerTest {

    @TempDir
    Path tempDir;

    private SecurityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SecurityAnalyzer();
    }

    @Test
    void testBasicSecurityConfiguration() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

                @Configuration
                @EnableWebSecurity
                public class SecurityConfig {
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isSecurityEnabled());
        assertEquals(1, result.getSecurityConfigs().size());
        assertTrue(result.getSecurityAnnotations().contains("EnableWebSecurity"));
    }

    @Test
    void testWebSecurityConfigurerAdapter() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

                public class SecurityConfig extends WebSecurityConfigurerAdapter {
                    @Override
                    protected void configure(HttpSecurity http) throws Exception {
                        http.authorizeRequests()
                            .antMatchers("/admin/**").hasRole("ADMIN")
                            .antMatchers("/user/**").hasRole("USER")
                            .anyRequest().authenticated();
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isSecurityEnabled());
        assertEquals(1, result.getSecurityConfigs().size());
        assertTrue(result.getAuthorizationRules().stream()
                .anyMatch(rule -> rule.contains("HttpSecurity")));
    }

    @Test
    void testSecurityFilterChain() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.web.SecurityFilterChain;

                @Configuration
                public class SecurityConfig {
                    @Bean
                    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                        return http.build();
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isSecurityEnabled());
        assertEquals(1, result.getSecurityConfigs().size());
    }

    @Test
    void testUserDetailsService() throws IOException {
        String userService = """
                package com.example.service;

                import org.springframework.security.core.userdetails.UserDetailsService;
                import org.springframework.security.core.userdetails.UserDetails;
                import org.springframework.stereotype.Service;

                @Service
                public class CustomUserDetailsService implements UserDetailsService {
                    @Override
                    public UserDetails loadUserByUsername(String username) {
                        return null;
                    }
                }
                """;

        Path javaFile = createJavaFile("CustomUserDetailsService.java", userService);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getUserDetailsServices().size());
        assertTrue(result.getUserDetailsServices().get(0).contains("CustomUserDetailsService"));
    }

    @Test
    void testPasswordEncoder() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
                import org.springframework.security.crypto.password.PasswordEncoder;

                @Configuration
                public class SecurityConfig {
                    @Bean
                    public PasswordEncoder passwordEncoder() {
                        return new BCryptPasswordEncoder();
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getPasswordEncoders().size());
    }

    @Test
    void testAuthenticationManager() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Bean;
                import org.springframework.security.authentication.AuthenticationManager;

                public class SecurityConfig {
                    @Bean
                    public AuthenticationManager authenticationManager() {
                        return null;
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.getAuthenticationMechanisms().contains("AuthenticationManager"));
    }

    @Test
    void testMethodLevelSecurity() throws IOException {
        String controller = """
                package com.example.controller;

                import org.springframework.security.access.annotation.Secured;
                import org.springframework.security.access.prepost.PreAuthorize;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;

                @RestController
                public class AdminController {
                    @GetMapping("/admin/users")
                    @Secured("ROLE_ADMIN")
                    public String getUsers() {
                        return "users";
                    }

                    @GetMapping("/admin/settings")
                    @PreAuthorize("hasRole('ADMIN')")
                    public String getSettings() {
                        return "settings";
                    }
                }
                """;

        Path javaFile = createJavaFile("AdminController.java", controller);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(2, result.getSecuredMethods().size());
        assertTrue(result.getSecuredMethods().containsValue("Secured"));
        assertTrue(result.getSecuredMethods().containsValue("PreAuthorize"));
    }

    @Test
    void testGlobalMethodSecurity() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

                @Configuration
                @EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
                public class MethodSecurityConfig {
                }
                """;

        Path javaFile = createJavaFile("MethodSecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isSecurityEnabled());
        assertTrue(result.getSecurityAnnotations().contains("EnableGlobalMethodSecurity"));
    }

    @Test
    void testInMemoryAuthentication() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class SecurityConfig {
                    public void configureInMemoryAuthentication() {
                        // Configure in-memory auth
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.getAuthenticationMechanisms().contains("In-Memory Authentication"));
    }

    @Test
    void testJdbcAuthentication() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class SecurityConfig {
                    public void configureJdbcAuthentication() {
                        // Configure JDBC auth
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.getAuthenticationMechanisms().contains("JDBC Authentication"));
    }

    @Test
    void testOAuth2Configuration() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Bean;
                import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

                public class OAuth2Config {
                    @Bean
                    public OAuth2AuthorizedClientService clientService() {
                        return null;
                    }
                }
                """;

        Path javaFile = createJavaFile("OAuth2Config.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.getAuthenticationMechanisms().contains("OAuth2/OIDC"));
    }

    @Test
    void testXmlSecurityConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:security="http://www.springframework.org/schema/security">

                    <bean id="userDetailsService" class="com.example.security.CustomUserDetailsService"/>
                    <bean id="passwordEncoder" class="org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"/>
                    <bean id="authenticationManager" class="org.springframework.security.authentication.ProviderManager"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("security-config.xml", xml);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(), List.of(xmlFile));

        assertTrue(result.isSecurityEnabled());
        assertEquals(1, result.getUserDetailsServices().size());
        assertEquals(1, result.getPasswordEncoders().size());
        assertTrue(result.getAuthenticationMechanisms().contains("AuthenticationManager (XML)"));
    }

    @Test
    void testCompleteSecuritySetup() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
                import org.springframework.security.crypto.password.PasswordEncoder;
                import org.springframework.security.authentication.AuthenticationManager;

                @Configuration
                @EnableWebSecurity
                public class SecurityConfig {
                    @Bean
                    public PasswordEncoder passwordEncoder() {
                        return null;
                    }

                    @Bean
                    public AuthenticationManager authenticationManager() {
                        return null;
                    }

                    protected void configure(HttpSecurity http) {
                    }
                }
                """;

        String userService = """
                package com.example.service;

                import org.springframework.security.core.userdetails.UserDetailsService;
                import org.springframework.stereotype.Service;

                @Service
                public class CustomUserDetailsService implements UserDetailsService {
                    public UserDetails loadUserByUsername(String username) {
                        return null;
                    }
                }
                """;

        String controller = """
                package com.example.controller;

                import org.springframework.security.access.prepost.PreAuthorize;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;

                @RestController
                public class SecureController {
                    @GetMapping("/secure")
                    @PreAuthorize("hasRole('USER')")
                    public String secure() {
                        return "secure";
                    }
                }
                """;

        Path config = createJavaFile("SecurityConfig.java", securityConfig);
        Path service = createJavaFile("CustomUserDetailsService.java", userService);
        Path ctrl = createJavaFile("SecureController.java", controller);

        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(
                List.of(config, service, ctrl), List.of());

        assertTrue(result.isSecurityEnabled());
        assertEquals(1, result.getSecurityConfigs().size());
        assertEquals(1, result.getUserDetailsServices().size());
        assertEquals(1, result.getPasswordEncoders().size());
        assertTrue(result.getAuthenticationMechanisms().contains("AuthenticationManager"));
        assertEquals(1, result.getSecuredMethods().size());
        assertTrue(result.getAuthorizationRules().stream().anyMatch(r -> r.contains("HttpSecurity")));
    }

    @Test
    void testNoSecurityDetected() throws IOException {
        String regularClass = """
                package com.example;

                public class RegularClass {
                    public void doSomething() {
                    }
                }
                """;

        Path javaFile = createJavaFile("RegularClass.java", regularClass);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertFalse(result.isSecurityEnabled());
        assertEquals(0, result.getSecurityConfigs().size());
        assertEquals(0, result.getAuthenticationMechanisms().size());
        assertEquals(0, result.getUserDetailsServices().size());
    }

    @Test
    void testResultToString() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

                @EnableWebSecurity
                public class SecurityConfig {
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        String toString = result.toString();
        assertTrue(toString.contains("enabled=true"));
    }

    @Test
    void testResultGettersReturnDefensiveCopies() throws IOException {
        String securityConfig = """
                package com.example.config;

                import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

                @EnableWebSecurity
                public class SecurityConfig {
                }
                """;

        Path javaFile = createJavaFile("SecurityConfig.java", securityConfig);
        SecurityAnalyzer.SecurityAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        // Modify returned collections
        result.getSecurityConfigs().clear();
        result.getSecurityAnnotations().clear();
        result.getAuthenticationMechanisms().clear();
        result.getAuthorizationRules().clear();
        result.getUserDetailsServices().clear();
        result.getPasswordEncoders().clear();
        result.getSecuredMethods().clear();

        // Verify original data is preserved
        assertEquals(1, result.getSecurityConfigs().size());
        assertEquals(1, result.getSecurityAnnotations().size());
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
}
