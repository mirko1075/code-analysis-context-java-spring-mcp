package com.mcp.codeanalysis.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents information extracted from a Java source file.
 */
public class JavaFileInfo {
    private String filePath;
    private String packageName;
    private List<String> imports;
    private List<ClassInfo> classes;
    private int totalLines;
    private int codeLines;

    public JavaFileInfo() {
        this.imports = new ArrayList<>();
        this.classes = new ArrayList<>();
    }

    public JavaFileInfo(String filePath) {
        this();
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void addImport(String importDecl) {
        this.imports.add(importDecl);
    }

    public List<ClassInfo> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassInfo> classes) {
        this.classes = classes;
    }

    public void addClass(ClassInfo classInfo) {
        this.classes.add(classInfo);
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public void setCodeLines(int codeLines) {
        this.codeLines = codeLines;
    }

    /**
     * Represents information about a class, interface, enum, or record.
     */
    public static class ClassInfo {
        private String name;
        private String type; // class, interface, enum, record, annotation
        private List<String> annotations;
        private List<String> modifiers;
        private String extendsClass;
        private List<String> implementsInterfaces;
        private List<FieldInfo> fields;
        private List<MethodInfo> methods;
        private int startLine;
        private int endLine;

        public ClassInfo() {
            this.annotations = new ArrayList<>();
            this.modifiers = new ArrayList<>();
            this.implementsInterfaces = new ArrayList<>();
            this.fields = new ArrayList<>();
            this.methods = new ArrayList<>();
        }

        public ClassInfo(String name, String type) {
            this();
            this.name = name;
            this.type = type;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations;
        }

        public void addAnnotation(String annotation) {
            this.annotations.add(annotation);
        }

        public List<String> getModifiers() {
            return modifiers;
        }

        public void setModifiers(List<String> modifiers) {
            this.modifiers = modifiers;
        }

        public void addModifier(String modifier) {
            this.modifiers.add(modifier);
        }

        public String getExtendsClass() {
            return extendsClass;
        }

        public void setExtendsClass(String extendsClass) {
            this.extendsClass = extendsClass;
        }

        public List<String> getImplementsInterfaces() {
            return implementsInterfaces;
        }

        public void setImplementsInterfaces(List<String> implementsInterfaces) {
            this.implementsInterfaces = implementsInterfaces;
        }

        public void addImplementsInterface(String interfaceName) {
            this.implementsInterfaces.add(interfaceName);
        }

        public List<FieldInfo> getFields() {
            return fields;
        }

        public void setFields(List<FieldInfo> fields) {
            this.fields = fields;
        }

        public void addField(FieldInfo field) {
            this.fields.add(field);
        }

        public List<MethodInfo> getMethods() {
            return methods;
        }

        public void setMethods(List<MethodInfo> methods) {
            this.methods = methods;
        }

        public void addMethod(MethodInfo method) {
            this.methods.add(method);
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }
    }

    /**
     * Represents information about a field.
     */
    public static class FieldInfo {
        private String name;
        private String type;
        private List<String> annotations;
        private Map<String, String> annotationDetails;  // Full annotation strings with parameters
        private List<String> modifiers;
        private int line;

        public FieldInfo() {
            this.annotations = new ArrayList<>();
            this.annotationDetails = new HashMap<>();
            this.modifiers = new ArrayList<>();
        }

        public FieldInfo(String name, String type) {
            this();
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations;
        }

        public void addAnnotation(String annotation) {
            this.annotations.add(annotation);
        }

        public Map<String, String> getAnnotationDetails() {
            return annotationDetails;
        }

        public void setAnnotationDetails(Map<String, String> annotationDetails) {
            this.annotationDetails = annotationDetails;
        }

        public void addAnnotationDetail(String name, String fullAnnotation) {
            this.annotationDetails.put(name, fullAnnotation);
        }

        public List<String> getModifiers() {
            return modifiers;
        }

        public void setModifiers(List<String> modifiers) {
            this.modifiers = modifiers;
        }

        public void addModifier(String modifier) {
            this.modifiers.add(modifier);
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }
    }

    /**
     * Represents information about a method.
     */
    public static class MethodInfo {
        private String name;
        private String returnType;
        private List<ParameterInfo> parameters;
        private List<String> annotations;
        private List<String> modifiers;
        private int startLine;
        private int endLine;
        private int complexity;

        public MethodInfo() {
            this.parameters = new ArrayList<>();
            this.annotations = new ArrayList<>();
            this.modifiers = new ArrayList<>();
        }

        public MethodInfo(String name, String returnType) {
            this();
            this.name = name;
            this.returnType = returnType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public List<ParameterInfo> getParameters() {
            return parameters;
        }

        public void setParameters(List<ParameterInfo> parameters) {
            this.parameters = parameters;
        }

        public void addParameter(ParameterInfo parameter) {
            this.parameters.add(parameter);
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations;
        }

        public void addAnnotation(String annotation) {
            this.annotations.add(annotation);
        }

        public List<String> getModifiers() {
            return modifiers;
        }

        public void setModifiers(List<String> modifiers) {
            this.modifiers = modifiers;
        }

        public void addModifier(String modifier) {
            this.modifiers.add(modifier);
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public int getComplexity() {
            return complexity;
        }

        public void setComplexity(int complexity) {
            this.complexity = complexity;
        }

        /**
         * Get method signature for display.
         */
        public String getSignature() {
            StringBuilder sig = new StringBuilder();
            sig.append(returnType).append(" ").append(name).append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(parameters.get(i).getType()).append(" ").append(parameters.get(i).getName());
            }
            sig.append(")");
            return sig.toString();
        }
    }

    /**
     * Represents information about a method parameter.
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        private List<String> annotations;

        public ParameterInfo() {
            this.annotations = new ArrayList<>();
        }

        public ParameterInfo(String name, String type) {
            this();
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<String> annotations) {
            this.annotations = annotations;
        }

        public void addAnnotation(String annotation) {
            this.annotations.add(annotation);
        }
    }
}
