package com.mcp.codeanalysis.analyzers;

import com.mcp.codeanalysis.parsers.XmlConfigParser;
import com.mcp.codeanalysis.types.XmlBeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes traditional Spring Framework configurations (XML-based).
 * Detects Spring MVC, transaction management, AOP, and other Spring patterns.
 */
public class SpringFrameworkAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(SpringFrameworkAnalyzer.class);

    private final XmlConfigParser xmlParser;

    public SpringFrameworkAnalyzer() {
        this.xmlParser = new XmlConfigParser();
    }

    /**
     * Analyze Spring Framework usage in a project.
     *
     * @param xmlFiles List of Spring XML configuration files
     * @return Analysis result
     */
    public SpringAnalysisResult analyze(List<Path> xmlFiles) {
        SpringAnalysisResult result = new SpringAnalysisResult();

        for (Path xmlFile : xmlFiles) {
            analyzeXmlConfig(xmlFile, result);
        }

        return result;
    }

    /**
     * Analyze a single XML configuration file.
     */
    private void analyzeXmlConfig(Path xmlFile, SpringAnalysisResult result) {
        List<XmlBeanDefinition> beans = xmlParser.parseXmlConfig(xmlFile);
        Map<String, Boolean> namespaces = xmlParser.detectSpringNamespaces(xmlFile);

        result.addConfigFile(xmlFile.toString());

        // Detect Spring MVC
        if (hasMvcConfiguration(beans, namespaces)) {
            result.setMvcEnabled(true);
            detectMvcComponents(beans, result);
        }

        // Detect transaction management
        if (hasTransactionManagement(beans, namespaces)) {
            result.setTransactionManagementEnabled(true);
            detectTransactionComponents(beans, result);
        }

        // Detect AOP
        if (hasAopConfiguration(beans, namespaces)) {
            result.setAopEnabled(true);
            detectAopComponents(beans, result);
        }

        // Detect data access
        if (hasDataAccessConfiguration(beans, namespaces)) {
            result.setDataAccessEnabled(true);
            detectDataAccessComponents(beans, result);
        }

        // Detect security
        if (hasSecurityConfiguration(namespaces)) {
            result.setSecurityEnabled(true);
        }

        // Count all beans
        result.incrementBeanCount(beans.size());
    }

    /**
     * Check if MVC is configured.
     */
    private boolean hasMvcConfiguration(List<XmlBeanDefinition> beans, Map<String, Boolean> namespaces) {
        // Check for mvc namespace
        if (namespaces.getOrDefault("mvc", false)) {
            return true;
        }

        // Check for DispatcherServlet or common MVC beans
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className != null && (
                    className.contains("DispatcherServlet") ||
                    className.contains("HandlerMapping") ||
                    className.contains("HandlerAdapter") ||
                    className.contains("ViewResolver") ||
                    className.contains("Controller"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect MVC components.
     */
    private void detectMvcComponents(List<XmlBeanDefinition> beans, SpringAnalysisResult result) {
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className == null) continue;

            if (className.contains("ViewResolver")) {
                result.addMvcComponent("ViewResolver: " + bean.getId());
            } else if (className.contains("HandlerMapping")) {
                result.addMvcComponent("HandlerMapping: " + bean.getId());
            } else if (className.contains("HandlerAdapter")) {
                result.addMvcComponent("HandlerAdapter: " + bean.getId());
            } else if (className.contains("Controller")) {
                result.addMvcComponent("Controller: " + bean.getId());
            }
        }
    }

    /**
     * Check if transaction management is configured.
     */
    private boolean hasTransactionManagement(List<XmlBeanDefinition> beans, Map<String, Boolean> namespaces) {
        // Check for tx namespace
        if (namespaces.getOrDefault("tx", false)) {
            return true;
        }

        // Check for transaction manager beans
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className != null && className.contains("TransactionManager")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect transaction components.
     */
    private void detectTransactionComponents(List<XmlBeanDefinition> beans, SpringAnalysisResult result) {
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className == null) continue;

            if (className.contains("TransactionManager")) {
                result.addTransactionComponent("TransactionManager: " + bean.getId() + " (" + getSimpleClassName(className) + ")");
            } else if (className.contains("TransactionTemplate")) {
                result.addTransactionComponent("TransactionTemplate: " + bean.getId());
            }
        }
    }

    /**
     * Check if AOP is configured.
     */
    private boolean hasAopConfiguration(List<XmlBeanDefinition> beans, Map<String, Boolean> namespaces) {
        // Check for aop namespace
        if (namespaces.getOrDefault("aop", false)) {
            return true;
        }

        // Check for AOP-related beans
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className != null && (
                    className.contains("Advisor") ||
                    className.contains("Pointcut") ||
                    className.contains("Aspect"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect AOP components.
     */
    private void detectAopComponents(List<XmlBeanDefinition> beans, SpringAnalysisResult result) {
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className == null) continue;

            if (className.contains("Advisor")) {
                result.addAopComponent("Advisor: " + bean.getId());
            } else if (className.contains("Aspect")) {
                result.addAopComponent("Aspect: " + bean.getId());
            } else if (className.contains("Pointcut")) {
                result.addAopComponent("Pointcut: " + bean.getId());
            }
        }
    }

    /**
     * Check if data access is configured.
     */
    private boolean hasDataAccessConfiguration(List<XmlBeanDefinition> beans, Map<String, Boolean> namespaces) {
        // Check for common data access beans
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className != null && (
                    className.contains("DataSource") ||
                    className.contains("JdbcTemplate") ||
                    className.contains("HibernateTemplate") ||
                    className.contains("SessionFactory") ||
                    className.contains("EntityManagerFactory"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect data access components.
     */
    private void detectDataAccessComponents(List<XmlBeanDefinition> beans, SpringAnalysisResult result) {
        for (XmlBeanDefinition bean : beans) {
            String className = bean.getClassName();
            if (className == null) continue;

            if (className.contains("DataSource")) {
                result.addDataAccessComponent("DataSource: " + bean.getId() + " (" + getSimpleClassName(className) + ")");
            } else if (className.contains("JdbcTemplate")) {
                result.addDataAccessComponent("JdbcTemplate: " + bean.getId());
            } else if (className.contains("SessionFactory")) {
                result.addDataAccessComponent("Hibernate SessionFactory: " + bean.getId());
            } else if (className.contains("EntityManagerFactory")) {
                result.addDataAccessComponent("JPA EntityManagerFactory: " + bean.getId());
            }
        }
    }

    /**
     * Check if security is configured.
     */
    private boolean hasSecurityConfiguration(Map<String, Boolean> namespaces) {
        return namespaces.getOrDefault("security", false);
    }

    /**
     * Get simple class name from fully qualified name.
     */
    private String getSimpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    /**
     * Spring Framework analysis result.
     */
    public static class SpringAnalysisResult {
        private final List<String> configFiles = new ArrayList<>();
        private int beanCount = 0;

        private boolean mvcEnabled = false;
        private boolean transactionManagementEnabled = false;
        private boolean aopEnabled = false;
        private boolean dataAccessEnabled = false;
        private boolean securityEnabled = false;

        private final List<String> mvcComponents = new ArrayList<>();
        private final List<String> transactionComponents = new ArrayList<>();
        private final List<String> aopComponents = new ArrayList<>();
        private final List<String> dataAccessComponents = new ArrayList<>();

        public void addConfigFile(String file) {
            configFiles.add(file);
        }

        public void incrementBeanCount(int count) {
            beanCount += count;
        }

        public void setMvcEnabled(boolean enabled) {
            mvcEnabled = enabled;
        }

        public void setTransactionManagementEnabled(boolean enabled) {
            transactionManagementEnabled = enabled;
        }

        public void setAopEnabled(boolean enabled) {
            aopEnabled = enabled;
        }

        public void setDataAccessEnabled(boolean enabled) {
            dataAccessEnabled = enabled;
        }

        public void setSecurityEnabled(boolean enabled) {
            securityEnabled = enabled;
        }

        public void addMvcComponent(String component) {
            mvcComponents.add(component);
        }

        public void addTransactionComponent(String component) {
            transactionComponents.add(component);
        }

        public void addAopComponent(String component) {
            aopComponents.add(component);
        }

        public void addDataAccessComponent(String component) {
            dataAccessComponents.add(component);
        }

        // Getters

        public List<String> getConfigFiles() {
            return new ArrayList<>(configFiles);
        }

        public int getBeanCount() {
            return beanCount;
        }

        public boolean isMvcEnabled() {
            return mvcEnabled;
        }

        public boolean isTransactionManagementEnabled() {
            return transactionManagementEnabled;
        }

        public boolean isAopEnabled() {
            return aopEnabled;
        }

        public boolean isDataAccessEnabled() {
            return dataAccessEnabled;
        }

        public boolean isSecurityEnabled() {
            return securityEnabled;
        }

        public List<String> getMvcComponents() {
            return new ArrayList<>(mvcComponents);
        }

        public List<String> getTransactionComponents() {
            return new ArrayList<>(transactionComponents);
        }

        public List<String> getAopComponents() {
            return new ArrayList<>(aopComponents);
        }

        public List<String> getDataAccessComponents() {
            return new ArrayList<>(dataAccessComponents);
        }

        public boolean isSpringFrameworkUsed() {
            return mvcEnabled || transactionManagementEnabled || aopEnabled ||
                   dataAccessEnabled || securityEnabled || beanCount > 0;
        }

        @Override
        public String toString() {
            return "SpringAnalysisResult{" +
                    "configFiles=" + configFiles.size() +
                    ", beanCount=" + beanCount +
                    ", mvc=" + mvcEnabled +
                    ", transaction=" + transactionManagementEnabled +
                    ", aop=" + aopEnabled +
                    ", dataAccess=" + dataAccessEnabled +
                    ", security=" + securityEnabled +
                    '}';
        }
    }
}
