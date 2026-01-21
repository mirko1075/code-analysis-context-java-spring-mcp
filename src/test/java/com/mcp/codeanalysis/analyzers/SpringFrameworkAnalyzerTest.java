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
 * Unit tests for SpringFrameworkAnalyzer.
 */
class SpringFrameworkAnalyzerTest {

    @TempDir
    Path tempDir;

    private SpringFrameworkAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SpringFrameworkAnalyzer();
    }

    @Test
    void testBasicSpringConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="myService" class="com.example.MyService"/>
                    <bean id="myRepository" class="com.example.MyRepository"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isSpringFrameworkUsed());
        assertEquals(2, result.getBeanCount());
        assertEquals(1, result.getConfigFiles().size());
        assertFalse(result.isMvcEnabled());
        assertFalse(result.isAopEnabled());
    }

    @Test
    void testSpringMvcConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:mvc="http://www.springframework.org/schema/mvc">
                    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
                        <property name="prefix" value="/WEB-INF/views/"/>
                        <property name="suffix" value=".jsp"/>
                    </bean>
                    <bean id="handlerMapping" class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("servlet-context.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isMvcEnabled());
        assertEquals(2, result.getMvcComponents().size());
        assertTrue(result.getMvcComponents().stream().anyMatch(c -> c.contains("ViewResolver")));
        assertTrue(result.getMvcComponents().stream().anyMatch(c -> c.contains("HandlerMapping")));
    }

    @Test
    void testTransactionManagement() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:tx="http://www.springframework.org/schema/tx">
                    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>
                    <bean id="dataSource" class="org.apache.commons.dbcp2.BasicDataSource"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isTransactionManagementEnabled());
        assertEquals(1, result.getTransactionComponents().size());
        assertTrue(result.getTransactionComponents().get(0).contains("TransactionManager"));
    }

    @Test
    void testAopConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:aop="http://www.springframework.org/schema/aop">
                    <bean id="loggingAspect" class="com.example.aspect.LoggingAspect"/>
                    <bean id="performanceAdvisor" class="com.example.aspect.PerformanceAdvisor"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("aop-config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isAopEnabled());
        assertEquals(2, result.getAopComponents().size());
    }

    @Test
    void testDataAccessConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
                        <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
                        <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
                    </bean>
                    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>
                    <bean id="sessionFactory" class="org.springframework.orm.hibernate5.LocalSessionFactoryBean">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>
                </beans>
                """;

        Path xmlFile = createXmlFile("datasource-config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isDataAccessEnabled());
        assertEquals(3, result.getDataAccessComponents().size());
        assertTrue(result.getDataAccessComponents().stream().anyMatch(c -> c.contains("DataSource")));
        assertTrue(result.getDataAccessComponents().stream().anyMatch(c -> c.contains("JdbcTemplate")));
        assertTrue(result.getDataAccessComponents().stream().anyMatch(c -> c.contains("SessionFactory")));
    }

    @Test
    void testSecurityConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:security="http://www.springframework.org/schema/security">
                    <bean id="myService" class="com.example.MyService"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("security-config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isSecurityEnabled());
    }

    @Test
    void testMultipleConfigFiles() throws IOException {
        String xml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="service1" class="com.example.Service1"/>
                </beans>
                """;

        String xml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:mvc="http://www.springframework.org/schema/mvc">
                    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"/>
                </beans>
                """;

        Path file1 = createXmlFile("config1.xml", xml1);
        Path file2 = createXmlFile("config2.xml", xml2);

        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(file1, file2));

        assertEquals(2, result.getConfigFiles().size());
        assertEquals(2, result.getBeanCount());
        assertTrue(result.isMvcEnabled());
    }

    @Test
    void testCompleteSpringApplication() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:mvc="http://www.springframework.org/schema/mvc"
                       xmlns:tx="http://www.springframework.org/schema/tx"
                       xmlns:aop="http://www.springframework.org/schema/aop">

                    <!-- MVC Configuration -->
                    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"/>

                    <!-- Data Access -->
                    <bean id="dataSource" class="org.apache.commons.dbcp2.BasicDataSource"/>
                    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>

                    <!-- Transaction Management -->
                    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>

                    <!-- AOP -->
                    <bean id="loggingAspect" class="com.example.aspect.LoggingAspect"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("applicationContext.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isSpringFrameworkUsed());
        assertTrue(result.isMvcEnabled());
        assertTrue(result.isTransactionManagementEnabled());
        assertTrue(result.isAopEnabled());
        assertTrue(result.isDataAccessEnabled());
        assertEquals(5, result.getBeanCount());
    }

    @Test
    void testJpaConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="entityManagerFactory" class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean">
                        <property name="dataSource" ref="dataSource"/>
                    </bean>
                    <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("jpa-config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isDataAccessEnabled());
        assertTrue(result.getDataAccessComponents().stream()
                .anyMatch(c -> c.contains("EntityManagerFactory")));
    }

    @Test
    void testEmptyConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                </beans>
                """;

        Path xmlFile = createXmlFile("empty.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertFalse(result.isSpringFrameworkUsed());
        assertEquals(0, result.getBeanCount());
        assertFalse(result.isMvcEnabled());
        assertFalse(result.isAopEnabled());
        assertFalse(result.isTransactionManagementEnabled());
        assertFalse(result.isDataAccessEnabled());
    }

    @Test
    void testResultToString() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:mvc="http://www.springframework.org/schema/mvc">
                    <bean id="service" class="com.example.Service"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        String toString = result.toString();
        assertTrue(toString.contains("configFiles=1"));
        assertTrue(toString.contains("beanCount=1"));
        assertTrue(toString.contains("mvc=true"));
    }

    @Test
    void testControllerDetection() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="userController" class="com.example.controller.UserController"/>
                    <bean id="productController" class="com.example.controller.ProductController"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("controllers.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isMvcEnabled());
        assertEquals(2, result.getMvcComponents().size());
        assertTrue(result.getMvcComponents().stream().allMatch(c -> c.contains("Controller")));
    }

    @Test
    void testTransactionTemplate() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="transactionTemplate" class="org.springframework.transaction.support.TransactionTemplate">
                        <property name="transactionManager" ref="txManager"/>
                    </bean>
                    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("tx-config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertTrue(result.isTransactionManagementEnabled());
        assertTrue(result.getTransactionComponents().stream()
                .anyMatch(c -> c.contains("TransactionTemplate")));
    }

    @Test
    void testAnalysisResultGetters() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:mvc="http://www.springframework.org/schema/mvc"
                       xmlns:tx="http://www.springframework.org/schema/tx">
                    <bean id="service" class="com.example.Service"/>
                    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"/>
                    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("config.xml", xml);
        SpringFrameworkAnalyzer.SpringAnalysisResult result = analyzer.analyze(List.of(xmlFile));

        assertNotNull(result.getConfigFiles());
        assertNotNull(result.getMvcComponents());
        assertNotNull(result.getTransactionComponents());
        assertNotNull(result.getAopComponents());
        assertNotNull(result.getDataAccessComponents());

        // Verify immutability (defensive copies)
        List<String> configFiles = result.getConfigFiles();
        configFiles.clear();
        assertEquals(1, result.getConfigFiles().size());
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
