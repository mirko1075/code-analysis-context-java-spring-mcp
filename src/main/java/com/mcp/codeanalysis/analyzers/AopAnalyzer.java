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
 * Analyzes Spring AOP (Aspect-Oriented Programming) configurations and patterns.
 * Detects aspects, pointcuts, advice, and cross-cutting concerns.
 */
public class AopAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(AopAnalyzer.class);

    private final JavaSourceParser javaParser;
    private final XmlConfigParser xmlParser;

    // AOP annotations
    private static final Set<String> AOP_ANNOTATIONS = Set.of(
            "Aspect",
            "Before",
            "After",
            "Around",
            "AfterReturning",
            "AfterThrowing",
            "Pointcut",
            "DeclareParents",
            "EnableAspectJAutoProxy"
    );

    // Advice types
    private static final Set<String> ADVICE_ANNOTATIONS = Set.of(
            "Before",
            "After",
            "Around",
            "AfterReturning",
            "AfterThrowing"
    );

    public AopAnalyzer() {
        this.javaParser = new JavaSourceParser();
        this.xmlParser = new XmlConfigParser();
    }

    /**
     * Analyze AOP usage in a project.
     *
     * @param javaFiles Java source files
     * @param xmlFiles  Spring XML configuration files
     * @return Analysis result
     */
    public AopAnalysisResult analyze(List<Path> javaFiles, List<Path> xmlFiles) {
        AopAnalysisResult result = new AopAnalysisResult();

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
     * Analyze a Java file for AOP patterns.
     */
    private void analyzeJavaFile(Path javaFile, AopAnalysisResult result) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            // Check for @Aspect annotation
            if (classInfo.getAnnotations().contains("Aspect")) {
                result.setAopEnabled(true);
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                AspectInfo aspectInfo = new AspectInfo(fullClassName, classInfo.getName());

                // Analyze pointcuts and advice in this aspect
                analyzeAspectMethods(classInfo, aspectInfo);

                result.addAspect(aspectInfo);
            }

            // Check for @EnableAspectJAutoProxy
            if (classInfo.getAnnotations().contains("EnableAspectJAutoProxy")) {
                result.setAopEnabled(true);
                result.setAspectJAutoProxyEnabled(true);
            }

            // Check for standalone pointcuts (outside @Aspect classes)
            for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
                if (method.getAnnotations().contains("Pointcut")) {
                    result.setAopEnabled(true);
                }
            }
        }
    }

    /**
     * Analyze methods in an @Aspect class.
     */
    private void analyzeAspectMethods(JavaFileInfo.ClassInfo classInfo, AspectInfo aspectInfo) {
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            // Check for pointcut definitions
            if (method.getAnnotations().contains("Pointcut")) {
                String pointcutExpression = extractPointcutExpression(method);
                aspectInfo.addPointcut(method.getName(), pointcutExpression);
            }

            // Check for advice annotations
            for (String annotation : method.getAnnotations()) {
                if (ADVICE_ANNOTATIONS.contains(annotation)) {
                    String pointcut = extractPointcutFromAdvice(method, annotation);
                    AdviceInfo advice = new AdviceInfo(
                            method.getName(),
                            annotation,
                            pointcut
                    );
                    aspectInfo.addAdvice(advice);
                }
            }
        }
    }

    /**
     * Extract pointcut expression from @Pointcut annotation.
     */
    private String extractPointcutExpression(JavaFileInfo.MethodInfo method) {
        // Try to extract from annotation details
        Map<String, String> annotationDetails = method.getAnnotationDetails();
        if (annotationDetails != null && annotationDetails.containsKey("Pointcut")) {
            String fullAnnotation = annotationDetails.get("Pointcut");
            // Extract expression from @Pointcut("expression")
            int start = fullAnnotation.indexOf("(\"");
            int end = fullAnnotation.lastIndexOf("\")");
            if (start != -1 && end != -1 && end > start) {
                return fullAnnotation.substring(start + 2, end);
            }
        }
        return "Unknown";
    }

    /**
     * Extract pointcut expression from advice annotation.
     */
    private String extractPointcutFromAdvice(JavaFileInfo.MethodInfo method, String adviceType) {
        // Try to extract from annotation details
        Map<String, String> annotationDetails = method.getAnnotationDetails();
        if (annotationDetails != null && annotationDetails.containsKey(adviceType)) {
            String fullAnnotation = annotationDetails.get(adviceType);
            // Extract expression from @Before("expression") or @Before(value = "expression")
            int start = fullAnnotation.indexOf("(\"");
            int end = fullAnnotation.lastIndexOf("\")");
            if (start != -1 && end != -1 && end > start) {
                return fullAnnotation.substring(start + 2, end);
            }
        }
        return "Unknown";
    }

    /**
     * Analyze XML configuration for AOP.
     */
    private void analyzeXmlConfig(Path xmlFile, AopAnalysisResult result) {
        Map<String, Boolean> namespaces = xmlParser.detectSpringNamespaces(xmlFile);

        // Check for aop namespace
        if (namespaces.getOrDefault("aop", false)) {
            result.setAopEnabled(true);
            result.addXmlConfig(xmlFile.toString());
        }

        // Parse AOP-related beans
        List<XmlBeanDefinition> beans = xmlParser.parseXmlConfig(xmlFile);
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className != null) {
                // Check for Advisor, Aspect, or Pointcut in class name
                if (className.contains("Advisor") ||
                    className.contains("Aspect") ||
                    className.contains("Pointcut") ||
                    className.contains("MethodInterceptor")) {
                    result.setAopEnabled(true);
                    result.addAopBean(bean.getId(), className);
                }
            }
        }
    }

    /**
     * AOP analysis result.
     */
    public static class AopAnalysisResult {
        private boolean aopEnabled = false;
        private boolean aspectJAutoProxyEnabled = false;
        private final List<AspectInfo> aspects = new ArrayList<>();
        private final List<String> xmlConfigs = new ArrayList<>();
        private final Map<String, String> aopBeans = new HashMap<>();

        public void setAopEnabled(boolean enabled) {
            this.aopEnabled = enabled;
        }

        public void setAspectJAutoProxyEnabled(boolean enabled) {
            this.aspectJAutoProxyEnabled = enabled;
        }

        public void addAspect(AspectInfo aspect) {
            this.aspects.add(aspect);
        }

        public void addXmlConfig(String config) {
            this.xmlConfigs.add(config);
        }

        public void addAopBean(String beanId, String className) {
            this.aopBeans.put(beanId, className);
        }

        // Getters

        public boolean isAopEnabled() {
            return aopEnabled;
        }

        public boolean isAspectJAutoProxyEnabled() {
            return aspectJAutoProxyEnabled;
        }

        public List<AspectInfo> getAspects() {
            return new ArrayList<>(aspects);
        }

        public int getAspectCount() {
            return aspects.size();
        }

        public int getTotalAdviceCount() {
            return aspects.stream()
                    .mapToInt(AspectInfo::getAdviceCount)
                    .sum();
        }

        public int getTotalPointcutCount() {
            return aspects.stream()
                    .mapToInt(AspectInfo::getPointcutCount)
                    .sum();
        }

        public List<String> getXmlConfigs() {
            return new ArrayList<>(xmlConfigs);
        }

        public Map<String, String> getAopBeans() {
            return new HashMap<>(aopBeans);
        }

        public Map<String, Integer> getAdviceTypeBreakdown() {
            Map<String, Integer> breakdown = new HashMap<>();
            for (AspectInfo aspect : aspects) {
                for (AdviceInfo advice : aspect.getAdviceList()) {
                    String type = advice.getAdviceType();
                    breakdown.put(type, breakdown.getOrDefault(type, 0) + 1);
                }
            }
            return breakdown;
        }

        @Override
        public String toString() {
            return "AopAnalysisResult{" +
                    "enabled=" + aopEnabled +
                    ", aspects=" + aspects.size() +
                    ", advice=" + getTotalAdviceCount() +
                    ", pointcuts=" + getTotalPointcutCount() +
                    '}';
        }
    }

    /**
     * Information about an aspect.
     */
    public static class AspectInfo {
        private final String fullClassName;
        private final String className;
        private final Map<String, String> pointcuts = new HashMap<>();
        private final List<AdviceInfo> adviceList = new ArrayList<>();

        public AspectInfo(String fullClassName, String className) {
            this.fullClassName = fullClassName;
            this.className = className;
        }

        public void addPointcut(String name, String expression) {
            this.pointcuts.put(name, expression);
        }

        public void addAdvice(AdviceInfo advice) {
            this.adviceList.add(advice);
        }

        public String getFullClassName() {
            return fullClassName;
        }

        public String getClassName() {
            return className;
        }

        public Map<String, String> getPointcuts() {
            return new HashMap<>(pointcuts);
        }

        public int getPointcutCount() {
            return pointcuts.size();
        }

        public List<AdviceInfo> getAdviceList() {
            return new ArrayList<>(adviceList);
        }

        public int getAdviceCount() {
            return adviceList.size();
        }

        @Override
        public String toString() {
            return "AspectInfo{" +
                    "className='" + className + '\'' +
                    ", pointcuts=" + pointcuts.size() +
                    ", advice=" + adviceList.size() +
                    '}';
        }
    }

    /**
     * Information about advice.
     */
    public static class AdviceInfo {
        private final String methodName;
        private final String adviceType;
        private final String pointcut;

        public AdviceInfo(String methodName, String adviceType, String pointcut) {
            this.methodName = methodName;
            this.adviceType = adviceType;
            this.pointcut = pointcut;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getAdviceType() {
            return adviceType;
        }

        public String getPointcut() {
            return pointcut;
        }

        @Override
        public String toString() {
            return "AdviceInfo{" +
                    "method='" + methodName + '\'' +
                    ", type=" + adviceType +
                    ", pointcut='" + pointcut + '\'' +
                    '}';
        }
    }
}
