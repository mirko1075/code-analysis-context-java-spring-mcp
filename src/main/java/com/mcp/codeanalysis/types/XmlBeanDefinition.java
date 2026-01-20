package com.mcp.codeanalysis.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a bean definition from Spring XML configuration.
 */
public class XmlBeanDefinition {
    private String id;
    private String name;
    private String className;
    private String scope;
    private String initMethod;
    private String destroyMethod;
    private boolean lazy;
    private boolean abstract_;
    private String parent;
    private String factoryBean;
    private String factoryMethod;
    private List<String> dependsOn;
    private List<PropertyInjection> properties;
    private List<ConstructorArgument> constructorArgs;
    private int lineNumber;

    public XmlBeanDefinition() {
        this.dependsOn = new ArrayList<>();
        this.properties = new ArrayList<>();
        this.constructorArgs = new ArrayList<>();
        this.scope = "singleton"; // Default scope
        this.lazy = false;
        this.abstract_ = false;
    }

    public XmlBeanDefinition(String id, String className) {
        this();
        this.id = id;
        this.className = className;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(String initMethod) {
        this.initMethod = initMethod;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public void setDestroyMethod(String destroyMethod) {
        this.destroyMethod = destroyMethod;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isAbstract() {
        return abstract_;
    }

    public void setAbstract(boolean abstract_) {
        this.abstract_ = abstract_;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getFactoryBean() {
        return factoryBean;
    }

    public void setFactoryBean(String factoryBean) {
        this.factoryBean = factoryBean;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public void addDependsOn(String beanId) {
        this.dependsOn.add(beanId);
    }

    public List<PropertyInjection> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyInjection> properties) {
        this.properties = properties;
    }

    public void addProperty(PropertyInjection property) {
        this.properties.add(property);
    }

    public List<ConstructorArgument> getConstructorArgs() {
        return constructorArgs;
    }

    public void setConstructorArgs(List<ConstructorArgument> constructorArgs) {
        this.constructorArgs = constructorArgs;
    }

    public void addConstructorArg(ConstructorArgument arg) {
        this.constructorArgs.add(arg);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Represents a property injection (setter injection).
     */
    public static class PropertyInjection {
        private String name;
        private String value;
        private String ref;
        private String type;

        public PropertyInjection() {
        }

        public PropertyInjection(String name, String value, String ref) {
            this.name = name;
            this.value = value;
            this.ref = ref;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isReference() {
            return ref != null;
        }
    }

    /**
     * Represents a constructor argument injection.
     */
    public static class ConstructorArgument {
        private int index;
        private String name;
        private String value;
        private String ref;
        private String type;

        public ConstructorArgument() {
            this.index = -1; // -1 means not specified
        }

        public ConstructorArgument(String value, String ref) {
            this();
            this.value = value;
            this.ref = ref;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isReference() {
            return ref != null;
        }
    }
}
