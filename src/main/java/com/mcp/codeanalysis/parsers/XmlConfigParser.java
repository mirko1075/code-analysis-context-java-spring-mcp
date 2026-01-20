package com.mcp.codeanalysis.parsers;

import com.mcp.codeanalysis.types.XmlBeanDefinition;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for Spring XML configuration files using DOM4J.
 * Extracts bean definitions, dependencies, and Spring-specific configurations.
 */
public class XmlConfigParser {
    private static final Logger logger = LoggerFactory.getLogger(XmlConfigParser.class);

    // Spring namespaces
    private static final String BEANS_NS = "http://www.springframework.org/schema/beans";
    private static final String CONTEXT_NS = "http://www.springframework.org/schema/context";
    private static final String TX_NS = "http://www.springframework.org/schema/tx";
    private static final String AOP_NS = "http://www.springframework.org/schema/aop";
    private static final String MVC_NS = "http://www.springframework.org/schema/mvc";
    private static final String SECURITY_NS = "http://www.springframework.org/schema/security";

    /**
     * Parse a Spring XML configuration file and extract bean definitions.
     *
     * @param filePath Path to the XML file
     * @return List of XmlBeanDefinition objects
     */
    public List<XmlBeanDefinition> parseXmlConfig(Path filePath) {
        List<XmlBeanDefinition> beans = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(filePath.toFile());

            // Extract beans from <bean> elements
            List<Node> beanNodes = document.selectNodes("//bean | //*[local-name()='bean']");

            for (Node node : beanNodes) {
                if (node instanceof Element) {
                    Element beanElement = (Element) node;
                    XmlBeanDefinition bean = extractBeanDefinition(beanElement);
                    if (bean != null) {
                        beans.add(bean);
                    }
                }
            }

            logger.debug("Successfully parsed {} beans from {}", beans.size(), filePath);

        } catch (DocumentException e) {
            logger.error("Error parsing XML file: {}", filePath, e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing XML file: {}", filePath, e);
        }

        return beans;
    }

    /**
     * Extract bean definition from a <bean> element.
     */
    private XmlBeanDefinition extractBeanDefinition(Element beanElement) {
        try {
            String id = beanElement.attributeValue("id");
            String name = beanElement.attributeValue("name");
            String className = beanElement.attributeValue("class");

            // Skip if neither id nor name is present
            if (id == null && name == null) {
                logger.warn("Bean element without id or name, skipping");
                return null;
            }

            XmlBeanDefinition bean = new XmlBeanDefinition();
            bean.setId(id);
            bean.setName(name);
            bean.setClassName(className);

            // Extract scope
            String scope = beanElement.attributeValue("scope");
            if (scope != null) {
                bean.setScope(scope);
            }

            // Extract lazy-init
            String lazyInit = beanElement.attributeValue("lazy-init");
            if ("true".equals(lazyInit)) {
                bean.setLazy(true);
            }

            // Extract abstract
            String abstractAttr = beanElement.attributeValue("abstract");
            if ("true".equals(abstractAttr)) {
                bean.setAbstract(true);
            }

            // Extract parent
            String parent = beanElement.attributeValue("parent");
            if (parent != null) {
                bean.setParent(parent);
            }

            // Extract init-method
            String initMethod = beanElement.attributeValue("init-method");
            if (initMethod != null) {
                bean.setInitMethod(initMethod);
            }

            // Extract destroy-method
            String destroyMethod = beanElement.attributeValue("destroy-method");
            if (destroyMethod != null) {
                bean.setDestroyMethod(destroyMethod);
            }

            // Extract factory-bean
            String factoryBean = beanElement.attributeValue("factory-bean");
            if (factoryBean != null) {
                bean.setFactoryBean(factoryBean);
            }

            // Extract factory-method
            String factoryMethod = beanElement.attributeValue("factory-method");
            if (factoryMethod != null) {
                bean.setFactoryMethod(factoryMethod);
            }

            // Extract depends-on
            String dependsOn = beanElement.attributeValue("depends-on");
            if (dependsOn != null) {
                String[] deps = dependsOn.split("[,;\\s]+");
                for (String dep : deps) {
                    if (!dep.trim().isEmpty()) {
                        bean.addDependsOn(dep.trim());
                    }
                }
            }

            // Extract property injections
            extractProperties(beanElement, bean);

            // Extract constructor arguments
            extractConstructorArgs(beanElement, bean);

            return bean;

        } catch (Exception e) {
            logger.error("Error extracting bean definition", e);
            return null;
        }
    }

    /**
     * Extract property injections from <property> elements.
     */
    private void extractProperties(Element beanElement, XmlBeanDefinition bean) {
        List<Element> propertyElements = beanElement.elements("property");

        for (Element propElement : propertyElements) {
            String name = propElement.attributeValue("name");
            String value = propElement.attributeValue("value");
            String ref = propElement.attributeValue("ref");

            if (name != null) {
                XmlBeanDefinition.PropertyInjection property =
                        new XmlBeanDefinition.PropertyInjection(name, value, ref);
                bean.addProperty(property);
            }
        }
    }

    /**
     * Extract constructor arguments from <constructor-arg> elements.
     */
    private void extractConstructorArgs(Element beanElement, XmlBeanDefinition bean) {
        List<Element> constructorArgElements = beanElement.elements("constructor-arg");

        for (Element argElement : constructorArgElements) {
            String indexStr = argElement.attributeValue("index");
            String name = argElement.attributeValue("name");
            String value = argElement.attributeValue("value");
            String ref = argElement.attributeValue("ref");
            String type = argElement.attributeValue("type");

            XmlBeanDefinition.ConstructorArgument arg =
                    new XmlBeanDefinition.ConstructorArgument(value, ref);

            if (indexStr != null) {
                try {
                    arg.setIndex(Integer.parseInt(indexStr));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid constructor-arg index: {}", indexStr);
                }
            }

            if (name != null) {
                arg.setName(name);
            }

            if (type != null) {
                arg.setType(type);
            }

            bean.addConstructorArg(arg);
        }
    }

    /**
     * Parse component-scan configuration to detect base packages.
     */
    public List<String> parseComponentScanPackages(Path filePath) {
        List<String> packages = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(filePath.toFile());

            // XPath to find component-scan elements
            List<Node> scanNodes = document.selectNodes("//*[local-name()='component-scan']");

            for (Node node : scanNodes) {
                if (node instanceof Element) {
                    Element scanElement = (Element) node;
                    String basePackage = scanElement.attributeValue("base-package");
                    if (basePackage != null) {
                        String[] pkgs = basePackage.split("[,;\\s]+");
                        for (String pkg : pkgs) {
                            if (!pkg.trim().isEmpty()) {
                                packages.add(pkg.trim());
                            }
                        }
                    }
                }
            }

            logger.debug("Found {} component-scan packages in {}", packages.size(), filePath);

        } catch (Exception e) {
            logger.error("Error parsing component-scan from: {}", filePath, e);
        }

        return packages;
    }

    /**
     * Parse property placeholder configuration.
     */
    public List<String> parsePropertyPlaceholderLocations(Path filePath) {
        List<String> locations = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(filePath.toFile());

            // Find property-placeholder elements
            List<Node> placeholderNodes = document.selectNodes(
                    "//*[local-name()='property-placeholder'] | //*[local-name()='properties']");

            for (Node node : placeholderNodes) {
                if (node instanceof Element) {
                    Element element = (Element) node;
                    String location = element.attributeValue("location");
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }

            logger.debug("Found {} property-placeholder locations in {}", locations.size(), filePath);

        } catch (Exception e) {
            logger.error("Error parsing property-placeholder from: {}", filePath, e);
        }

        return locations;
    }

    /**
     * Parse import statements to find other Spring XML files.
     */
    public List<String> parseImports(Path filePath) {
        List<String> imports = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(filePath.toFile());

            // Find import elements
            List<Node> importNodes = document.selectNodes("//import | //*[local-name()='import']");

            for (Node node : importNodes) {
                if (node instanceof Element) {
                    Element importElement = (Element) node;
                    String resource = importElement.attributeValue("resource");
                    if (resource != null) {
                        imports.add(resource);
                    }
                }
            }

            logger.debug("Found {} imports in {}", imports.size(), filePath);

        } catch (Exception e) {
            logger.error("Error parsing imports from: {}", filePath, e);
        }

        return imports;
    }

    /**
     * Detect Spring namespaces used in the XML file.
     */
    public Map<String, Boolean> detectSpringNamespaces(Path filePath) {
        Map<String, Boolean> namespaces = new HashMap<>();
        namespaces.put("beans", false);
        namespaces.put("context", false);
        namespaces.put("tx", false);
        namespaces.put("aop", false);
        namespaces.put("mvc", false);
        namespaces.put("security", false);

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(filePath.toFile());
            Element root = document.getRootElement();

            // Check namespace declarations
            if (!root.getNamespacesForURI(BEANS_NS).isEmpty()) {
                namespaces.put("beans", true);
            }
            if (!root.getNamespacesForURI(CONTEXT_NS).isEmpty()) {
                namespaces.put("context", true);
            }
            if (!root.getNamespacesForURI(TX_NS).isEmpty()) {
                namespaces.put("tx", true);
            }
            if (!root.getNamespacesForURI(AOP_NS).isEmpty()) {
                namespaces.put("aop", true);
            }
            if (!root.getNamespacesForURI(MVC_NS).isEmpty()) {
                namespaces.put("mvc", true);
            }
            if (!root.getNamespacesForURI(SECURITY_NS).isEmpty()) {
                namespaces.put("security", true);
            }

        } catch (Exception e) {
            logger.error("Error detecting namespaces from: {}", filePath, e);
        }

        return namespaces;
    }
}
