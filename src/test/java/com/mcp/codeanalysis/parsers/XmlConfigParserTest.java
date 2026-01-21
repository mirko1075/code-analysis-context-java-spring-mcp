package com.mcp.codeanalysis.parsers;

import com.mcp.codeanalysis.types.XmlBeanDefinition;
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
 * Unit tests for XmlConfigParser.
 */
class XmlConfigParserTest {

    private XmlConfigParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new XmlConfigParser();
    }

    @Test
    void testParseSimpleBean() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="userService" class="com.example.service.UserService"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);
        assertEquals("userService", bean.getId());
        assertEquals("com.example.service.UserService", bean.getClassName());
        assertEquals("singleton", bean.getScope());
        assertFalse(bean.isLazy());
        assertFalse(bean.isAbstract());
    }

    @Test
    void testParseBeanWithProperties() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource">
                        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
                        <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
                        <property name="username" value="root"/>
                        <property name="password" value="secret"/>
                    </bean>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);
        assertEquals("dataSource", bean.getId());
        assertEquals("org.apache.commons.dbcp.BasicDataSource", bean.getClassName());

        assertEquals(4, bean.getProperties().size());

        XmlBeanDefinition.PropertyInjection driverProp = bean.getProperties().stream()
                .filter(p -> "driverClassName".equals(p.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(driverProp);
        assertEquals("com.mysql.jdbc.Driver", driverProp.getValue());
        assertFalse(driverProp.isReference());
    }

    @Test
    void testParseBeanWithRef() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="userRepository" class="com.example.repository.UserRepository"/>

                    <bean id="userService" class="com.example.service.UserService">
                        <property name="userRepository" ref="userRepository"/>
                    </bean>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(2, beans.size());

        XmlBeanDefinition userService = beans.stream()
                .filter(b -> "userService".equals(b.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(userService);

        assertEquals(1, userService.getProperties().size());
        XmlBeanDefinition.PropertyInjection prop = userService.getProperties().get(0);
        assertEquals("userRepository", prop.getName());
        assertEquals("userRepository", prop.getRef());
        assertTrue(prop.isReference());
    }

    @Test
    void testParseBeanWithConstructorArgs() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="userService" class="com.example.service.UserService">
                        <constructor-arg index="0" value="admin"/>
                        <constructor-arg index="1" ref="userRepository"/>
                    </bean>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);

        assertEquals(2, bean.getConstructorArgs().size());

        XmlBeanDefinition.ConstructorArgument arg0 = bean.getConstructorArgs().get(0);
        assertEquals(0, arg0.getIndex());
        assertEquals("admin", arg0.getValue());
        assertFalse(arg0.isReference());

        XmlBeanDefinition.ConstructorArgument arg1 = bean.getConstructorArgs().get(1);
        assertEquals(1, arg1.getIndex());
        assertEquals("userRepository", arg1.getRef());
        assertTrue(arg1.isReference());
    }

    @Test
    void testParseBeanWithScope() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="prototypeBean" class="com.example.PrototypeBean" scope="prototype"/>
                    <bean id="sessionBean" class="com.example.SessionBean" scope="session"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(2, beans.size());

        XmlBeanDefinition prototypeBean = beans.stream()
                .filter(b -> "prototypeBean".equals(b.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(prototypeBean);
        assertEquals("prototype", prototypeBean.getScope());

        XmlBeanDefinition sessionBean = beans.stream()
                .filter(b -> "sessionBean".equals(b.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(sessionBean);
        assertEquals("session", sessionBean.getScope());
    }

    @Test
    void testParseBeanWithLazyInit() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="lazyBean" class="com.example.LazyBean" lazy-init="true"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);
        assertTrue(bean.isLazy());
    }

    @Test
    void testParseBeanWithInitDestroyMethods() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="lifecycleBean" class="com.example.LifecycleBean"
                          init-method="init" destroy-method="cleanup"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);
        assertEquals("init", bean.getInitMethod());
        assertEquals("cleanup", bean.getDestroyMethod());
    }

    @Test
    void testParseComponentScan() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:context="http://www.springframework.org/schema/context"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd">

                    <context:component-scan base-package="com.example.service, com.example.repository"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<String> packages = parser.parseComponentScanPackages(xmlFile);

        assertEquals(2, packages.size());
        assertTrue(packages.contains("com.example.service"));
        assertTrue(packages.contains("com.example.repository"));
    }

    @Test
    void testParsePropertyPlaceholder() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:context="http://www.springframework.org/schema/context"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd">

                    <context:property-placeholder location="classpath:application.properties"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<String> locations = parser.parsePropertyPlaceholderLocations(xmlFile);

        assertEquals(1, locations.size());
        assertEquals("classpath:application.properties", locations.get(0));
    }

    @Test
    void testParseImports() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <import resource="datasource-context.xml"/>
                    <import resource="security-context.xml"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<String> imports = parser.parseImports(xmlFile);

        assertEquals(2, imports.size());
        assertTrue(imports.contains("datasource-context.xml"));
        assertTrue(imports.contains("security-context.xml"));
    }

    @Test
    void testDetectSpringNamespaces() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:context="http://www.springframework.org/schema/context"
                       xmlns:tx="http://www.springframework.org/schema/tx"
                       xmlns:aop="http://www.springframework.org/schema/aop"
                       xsi:schemaLocation="
                           http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/tx
                           http://www.springframework.org/schema/tx/spring-tx.xsd
                           http://www.springframework.org/schema/aop
                           http://www.springframework.org/schema/aop/spring-aop.xsd">

                    <context:component-scan base-package="com.example"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        Map<String, Boolean> namespaces = parser.detectSpringNamespaces(xmlFile);

        assertTrue(namespaces.get("beans"));
        assertTrue(namespaces.get("context"));
        assertTrue(namespaces.get("tx"));
        assertTrue(namespaces.get("aop"));
        assertFalse(namespaces.get("mvc"));
        assertFalse(namespaces.get("security"));
    }

    @Test
    void testParseInvalidXml() {
        Path invalidFile = tempDir.resolve("invalid.xml");

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(invalidFile);

        assertTrue(beans.isEmpty());
    }

    @Test
    void testParseBeanWithDependsOn() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd">

                    <bean id="beanA" class="com.example.BeanA" depends-on="beanB,beanC"/>

                </beans>
                """;

        Path xmlFile = tempDir.resolve("applicationContext.xml");
        Files.writeString(xmlFile, xml);

        List<XmlBeanDefinition> beans = parser.parseXmlConfig(xmlFile);

        assertEquals(1, beans.size());
        XmlBeanDefinition bean = beans.get(0);
        assertEquals(2, bean.getDependsOn().size());
        assertTrue(bean.getDependsOn().contains("beanB"));
        assertTrue(bean.getDependsOn().contains("beanC"));
    }
}
