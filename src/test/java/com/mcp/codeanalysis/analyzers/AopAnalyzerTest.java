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
 * Unit tests for AopAnalyzer.
 */
class AopAnalyzerTest {

    @TempDir
    Path tempDir;

    private AopAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new AopAnalyzer();
    }

    @Test
    void testBasicAspect() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;

                @Aspect
                public class LoggingAspect {
                }
                """;

        Path javaFile = createJavaFile("LoggingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isAopEnabled());
        assertEquals(1, result.getAspectCount());
        assertEquals("LoggingAspect", result.getAspects().get(0).getClassName());
    }

    @Test
    void testAspectWithPointcut() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Pointcut;

                @Aspect
                public class LoggingAspect {
                    @Pointcut("execution(* com.example.service.*.*(..))")
                    public void serviceMethods() {}
                }
                """;

        Path javaFile = createJavaFile("LoggingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isAopEnabled());
        assertEquals(1, result.getAspectCount());
        assertEquals(1, result.getTotalPointcutCount());

        AopAnalyzer.AspectInfo aspect = result.getAspects().get(0);
        assertEquals(1, aspect.getPointcutCount());
        assertTrue(aspect.getPointcuts().containsKey("serviceMethods"));
    }

    @Test
    void testBeforeAdvice() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Before;

                @Aspect
                public class SecurityAspect {
                    @Before("execution(* com.example.service.*.*(..))")
                    public void checkSecurity() {
                        // Security check
                    }
                }
                """;

        Path javaFile = createJavaFile("SecurityAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isAopEnabled());
        assertEquals(1, result.getTotalAdviceCount());

        AopAnalyzer.AspectInfo aspect = result.getAspects().get(0);
        assertEquals(1, aspect.getAdviceCount());

        AopAnalyzer.AdviceInfo advice = aspect.getAdviceList().get(0);
        assertEquals("checkSecurity", advice.getMethodName());
        assertEquals("Before", advice.getAdviceType());
    }

    @Test
    void testAfterAdvice() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.After;

                @Aspect
                public class CleanupAspect {
                    @After("execution(* com.example.service.*.*(..))")
                    public void cleanup() {
                        // Cleanup logic
                    }
                }
                """;

        Path javaFile = createJavaFile("CleanupAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getTotalAdviceCount());
        assertEquals("After", result.getAspects().get(0).getAdviceList().get(0).getAdviceType());
    }

    @Test
    void testAroundAdvice() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Around;

                @Aspect
                public class PerformanceAspect {
                    @Around("execution(* com.example.service.*.*(..))")
                    public Object measurePerformance(ProceedingJoinPoint pjp) throws Throwable {
                        return pjp.proceed();
                    }
                }
                """;

        Path javaFile = createJavaFile("PerformanceAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getTotalAdviceCount());
        assertEquals("Around", result.getAspects().get(0).getAdviceList().get(0).getAdviceType());
    }

    @Test
    void testAfterReturningAdvice() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.AfterReturning;

                @Aspect
                public class AuditAspect {
                    @AfterReturning("execution(* com.example.service.*.*(..))")
                    public void auditSuccess() {
                        // Audit successful execution
                    }
                }
                """;

        Path javaFile = createJavaFile("AuditAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getTotalAdviceCount());
        assertEquals("AfterReturning", result.getAspects().get(0).getAdviceList().get(0).getAdviceType());
    }

    @Test
    void testAfterThrowingAdvice() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.AfterThrowing;

                @Aspect
                public class ErrorHandlingAspect {
                    @AfterThrowing("execution(* com.example.service.*.*(..))")
                    public void handleError() {
                        // Error handling
                    }
                }
                """;

        Path javaFile = createJavaFile("ErrorHandlingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getTotalAdviceCount());
        assertEquals("AfterThrowing", result.getAspects().get(0).getAdviceList().get(0).getAdviceType());
    }

    @Test
    void testMultipleAdviceInAspect() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Before;
                import org.aspectj.lang.annotation.After;
                import org.aspectj.lang.annotation.Around;

                @Aspect
                public class MultiAdviceAspect {
                    @Before("execution(* com.example.service.*.*(..))")
                    public void beforeMethod() {}

                    @After("execution(* com.example.service.*.*(..))")
                    public void afterMethod() {}

                    @Around("execution(* com.example.service.*.*(..))")
                    public Object aroundMethod(ProceedingJoinPoint pjp) throws Throwable {
                        return pjp.proceed();
                    }
                }
                """;

        Path javaFile = createJavaFile("MultiAdviceAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getAspectCount());
        assertEquals(3, result.getTotalAdviceCount());

        // Check advice type breakdown
        var breakdown = result.getAdviceTypeBreakdown();
        assertEquals(1, breakdown.get("Before"));
        assertEquals(1, breakdown.get("After"));
        assertEquals(1, breakdown.get("Around"));
    }

    @Test
    void testPointcutReferences() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Pointcut;
                import org.aspectj.lang.annotation.Before;

                @Aspect
                public class LoggingAspect {
                    @Pointcut("execution(* com.example.service.*.*(..))")
                    public void serviceMethods() {}

                    @Before("serviceMethods()")
                    public void logBefore() {
                        // Log before service methods
                    }
                }
                """;

        Path javaFile = createJavaFile("LoggingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertEquals(1, result.getTotalPointcutCount());
        assertEquals(1, result.getTotalAdviceCount());
    }

    @Test
    void testEnableAspectJAutoProxy() throws IOException {
        String configSource = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.EnableAspectJAutoProxy;

                @Configuration
                @EnableAspectJAutoProxy
                public class AopConfig {
                }
                """;

        Path javaFile = createJavaFile("AopConfig.java", configSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertTrue(result.isAopEnabled());
        assertTrue(result.isAspectJAutoProxyEnabled());
    }

    @Test
    void testXmlAopConfiguration() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:aop="http://www.springframework.org/schema/aop">

                    <bean id="loggingAspect" class="com.example.aspect.LoggingAspect"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("aop-config.xml", xml);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(), List.of(xmlFile));

        assertTrue(result.isAopEnabled());
        assertEquals(1, result.getXmlConfigs().size());
    }

    @Test
    void testAopBeansInXml() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <bean id="loggingAdvisor" class="com.example.aop.LoggingAdvisor"/>
                    <bean id="performanceInterceptor" class="com.example.aop.PerformanceMethodInterceptor"/>
                </beans>
                """;

        Path xmlFile = createXmlFile("aop-beans.xml", xml);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(), List.of(xmlFile));

        assertTrue(result.isAopEnabled());
        assertEquals(2, result.getAopBeans().size());
        assertTrue(result.getAopBeans().containsKey("loggingAdvisor"));
        assertTrue(result.getAopBeans().containsKey("performanceInterceptor"));
    }

    @Test
    void testMultipleAspects() throws IOException {
        String aspect1 = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Before;

                @Aspect
                public class LoggingAspect {
                    @Before("execution(* com.example.service.*.*(..))")
                    public void logBefore() {}
                }
                """;

        String aspect2 = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.After;

                @Aspect
                public class AuditAspect {
                    @After("execution(* com.example.service.*.*(..))")
                    public void auditAfter() {}
                }
                """;

        Path file1 = createJavaFile("LoggingAspect.java", aspect1);
        Path file2 = createJavaFile("AuditAspect.java", aspect2);

        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(file1, file2), List.of());

        assertEquals(2, result.getAspectCount());
        assertEquals(2, result.getTotalAdviceCount());
    }

    @Test
    void testCompleteAopSetup() throws IOException {
        String config = """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.EnableAspectJAutoProxy;

                @Configuration
                @EnableAspectJAutoProxy
                public class AopConfig {
                }
                """;

        String aspect = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Pointcut;
                import org.aspectj.lang.annotation.Before;
                import org.aspectj.lang.annotation.After;

                @Aspect
                public class TransactionAspect {
                    @Pointcut("execution(* com.example.service.*.*(..))")
                    public void serviceMethods() {}

                    @Before("serviceMethods()")
                    public void beginTransaction() {}

                    @After("serviceMethods()")
                    public void commitTransaction() {}
                }
                """;

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:aop="http://www.springframework.org/schema/aop">
                    <bean id="customAdvisor" class="com.example.aop.CustomAdvisor"/>
                </beans>
                """;

        Path configFile = createJavaFile("AopConfig.java", config);
        Path aspectFile = createJavaFile("TransactionAspect.java", aspect);
        Path xmlFile = createXmlFile("aop-config.xml", xml);

        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(
                List.of(configFile, aspectFile), List.of(xmlFile));

        assertTrue(result.isAopEnabled());
        assertTrue(result.isAspectJAutoProxyEnabled());
        assertEquals(1, result.getAspectCount());
        assertEquals(2, result.getTotalAdviceCount());
        assertEquals(1, result.getTotalPointcutCount());
        assertEquals(1, result.getXmlConfigs().size());
        assertEquals(1, result.getAopBeans().size());
    }

    @Test
    void testNoAopDetected() throws IOException {
        String regularClass = """
                package com.example;

                public class RegularClass {
                    public void doSomething() {
                    }
                }
                """;

        Path javaFile = createJavaFile("RegularClass.java", regularClass);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        assertFalse(result.isAopEnabled());
        assertEquals(0, result.getAspectCount());
        assertEquals(0, result.getTotalAdviceCount());
        assertEquals(0, result.getTotalPointcutCount());
    }

    @Test
    void testResultToString() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;
                import org.aspectj.lang.annotation.Before;

                @Aspect
                public class LoggingAspect {
                    @Before("execution(* com.example.service.*.*(..))")
                    public void logBefore() {}
                }
                """;

        Path javaFile = createJavaFile("LoggingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        String toString = result.toString();
        assertTrue(toString.contains("enabled=true"));
        assertTrue(toString.contains("aspects=1"));
        assertTrue(toString.contains("advice=1"));
    }

    @Test
    void testResultGettersReturnDefensiveCopies() throws IOException {
        String aspectSource = """
                package com.example.aspect;

                import org.aspectj.lang.annotation.Aspect;

                @Aspect
                public class LoggingAspect {
                }
                """;

        Path javaFile = createJavaFile("LoggingAspect.java", aspectSource);
        AopAnalyzer.AopAnalysisResult result = analyzer.analyze(List.of(javaFile), List.of());

        // Modify returned collections
        result.getAspects().clear();
        result.getXmlConfigs().clear();
        result.getAopBeans().clear();
        result.getAdviceTypeBreakdown().clear();

        // Verify original data is preserved
        assertEquals(1, result.getAspectCount());
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
