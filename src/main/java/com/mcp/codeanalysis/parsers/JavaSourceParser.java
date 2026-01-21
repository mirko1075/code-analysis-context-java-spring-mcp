package com.mcp.codeanalysis.parsers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.ComplexityAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Parser for Java source files using JavaParser library.
 * Extracts classes, methods, fields, annotations, and imports.
 */
public class JavaSourceParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaSourceParser.class);
    private final JavaParser javaParser;
    private final ComplexityAnalyzer complexityAnalyzer;

    public JavaSourceParser() {
        // Configure JavaParser for Java 17 to support records and other modern features
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.javaParser = new JavaParser(config);
        this.complexityAnalyzer = new ComplexityAnalyzer();
    }

    /**
     * Parse a Java source file and extract information.
     *
     * @param filePath Path to the Java file
     * @return JavaFileInfo containing extracted information, or null if parsing fails
     */
    public JavaFileInfo parseFile(Path filePath) {
        JavaFileInfo fileInfo = new JavaFileInfo(filePath.toString());

        try {
            // Read file content
            String content = Files.readString(filePath);

            // Count total lines
            long totalLines = content.lines().count();
            fileInfo.setTotalLines((int) totalLines);

            // Count code lines (non-empty, non-comment)
            long codeLines = content.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .filter(line -> !line.trim().startsWith("//"))
                    .filter(line -> !line.trim().startsWith("/*"))
                    .filter(line -> !line.trim().startsWith("*"))
                    .count();
            fileInfo.setCodeLines((int) codeLines);

            // Parse the Java file
            ParseResult<CompilationUnit> parseResult = javaParser.parse(filePath);

            if (!parseResult.isSuccessful()) {
                logger.warn("Failed to parse file: {}", filePath);
                parseResult.getProblems().forEach(problem ->
                        logger.warn("Parse problem: {}", problem.getMessage()));
                return fileInfo;
            }

            Optional<CompilationUnit> cuOptional = parseResult.getResult();
            if (cuOptional.isEmpty()) {
                logger.warn("No compilation unit found for file: {}", filePath);
                return fileInfo;
            }

            CompilationUnit cu = cuOptional.get();

            // Extract package name
            cu.getPackageDeclaration().ifPresent(pkg ->
                    fileInfo.setPackageName(pkg.getNameAsString()));

            // Extract imports
            for (ImportDeclaration importDecl : cu.getImports()) {
                fileInfo.addImport(importDecl.getNameAsString());
            }

            // Extract classes, interfaces, enums, records, annotations
            extractTypes(cu, fileInfo);

            logger.debug("Successfully parsed file: {} ({} classes, {} imports)",
                    filePath, fileInfo.getClasses().size(), fileInfo.getImports().size());

            return fileInfo;

        } catch (IOException e) {
            logger.error("Error reading file: {}", filePath, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Extract all type declarations (classes, interfaces, enums, etc.) from compilation unit.
     */
    private void extractTypes(CompilationUnit cu, JavaFileInfo fileInfo) {
        // Extract classes
        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (classDecl.isTopLevelType() || classDecl.isNestedType()) {
                JavaFileInfo.ClassInfo classInfo = extractClassInfo(classDecl);
                fileInfo.addClass(classInfo);
            }
        }

        // Extract enums
        for (EnumDeclaration enumDecl : cu.findAll(EnumDeclaration.class)) {
            if (enumDecl.isTopLevelType() || enumDecl.isNestedType()) {
                JavaFileInfo.ClassInfo classInfo = extractEnumInfo(enumDecl);
                fileInfo.addClass(classInfo);
            }
        }

        // Extract records (Java 14+)
        for (RecordDeclaration recordDecl : cu.findAll(RecordDeclaration.class)) {
            if (recordDecl.isTopLevelType() || recordDecl.isNestedType()) {
                JavaFileInfo.ClassInfo classInfo = extractRecordInfo(recordDecl);
                fileInfo.addClass(classInfo);
            }
        }

        // Extract annotation declarations
        for (AnnotationDeclaration annotationDecl : cu.findAll(AnnotationDeclaration.class)) {
            if (annotationDecl.isTopLevelType() || annotationDecl.isNestedType()) {
                JavaFileInfo.ClassInfo classInfo = extractAnnotationInfo(annotationDecl);
                fileInfo.addClass(classInfo);
            }
        }
    }

    /**
     * Extract information from a class or interface declaration.
     */
    private JavaFileInfo.ClassInfo extractClassInfo(ClassOrInterfaceDeclaration classDecl) {
        String type = classDecl.isInterface() ? "interface" : "class";
        JavaFileInfo.ClassInfo classInfo = new JavaFileInfo.ClassInfo(classDecl.getNameAsString(), type);

        // Extract modifiers
        classDecl.getModifiers().forEach(mod ->
                classInfo.addModifier(mod.getKeyword().asString()));

        // Extract annotations
        extractAnnotations(classDecl.getAnnotations(), classInfo);

        // Extract extends
        if (!classDecl.getExtendedTypes().isEmpty()) {
            classInfo.setExtendsClass(classDecl.getExtendedTypes().get(0).getNameAsString());
        }

        // Extract implements
        for (ClassOrInterfaceType implementedType : classDecl.getImplementedTypes()) {
            classInfo.addImplementsInterface(implementedType.getNameAsString());
        }

        // Extract line numbers
        classDecl.getRange().ifPresent(range -> {
            classInfo.setStartLine(range.begin.line);
            classInfo.setEndLine(range.end.line);
        });

        // Extract fields
        for (FieldDeclaration field : classDecl.getFields()) {
            extractFields(field, classInfo);
        }

        // Extract methods
        for (MethodDeclaration method : classDecl.getMethods()) {
            JavaFileInfo.MethodInfo methodInfo = extractMethodInfo(method);
            classInfo.addMethod(methodInfo);
        }

        // Extract constructors
        for (ConstructorDeclaration constructor : classDecl.getConstructors()) {
            JavaFileInfo.MethodInfo methodInfo = extractConstructorInfo(constructor);
            classInfo.addMethod(methodInfo);
        }

        return classInfo;
    }

    /**
     * Extract information from an enum declaration.
     */
    private JavaFileInfo.ClassInfo extractEnumInfo(EnumDeclaration enumDecl) {
        JavaFileInfo.ClassInfo classInfo = new JavaFileInfo.ClassInfo(enumDecl.getNameAsString(), "enum");

        // Extract modifiers
        enumDecl.getModifiers().forEach(mod ->
                classInfo.addModifier(mod.getKeyword().asString()));

        // Extract annotations
        extractAnnotations(enumDecl.getAnnotations(), classInfo);

        // Extract implements
        for (ClassOrInterfaceType implementedType : enumDecl.getImplementedTypes()) {
            classInfo.addImplementsInterface(implementedType.getNameAsString());
        }

        // Extract line numbers
        enumDecl.getRange().ifPresent(range -> {
            classInfo.setStartLine(range.begin.line);
            classInfo.setEndLine(range.end.line);
        });

        // Extract methods
        for (MethodDeclaration method : enumDecl.getMethods()) {
            JavaFileInfo.MethodInfo methodInfo = extractMethodInfo(method);
            classInfo.addMethod(methodInfo);
        }

        return classInfo;
    }

    /**
     * Extract information from a record declaration.
     */
    private JavaFileInfo.ClassInfo extractRecordInfo(RecordDeclaration recordDecl) {
        JavaFileInfo.ClassInfo classInfo = new JavaFileInfo.ClassInfo(recordDecl.getNameAsString(), "record");

        // Extract modifiers
        recordDecl.getModifiers().forEach(mod ->
                classInfo.addModifier(mod.getKeyword().asString()));

        // Extract annotations
        extractAnnotations(recordDecl.getAnnotations(), classInfo);

        // Extract implements
        for (ClassOrInterfaceType implementedType : recordDecl.getImplementedTypes()) {
            classInfo.addImplementsInterface(implementedType.getNameAsString());
        }

        // Extract line numbers
        recordDecl.getRange().ifPresent(range -> {
            classInfo.setStartLine(range.begin.line);
            classInfo.setEndLine(range.end.line);
        });

        // Extract record components as fields
        for (Parameter param : recordDecl.getParameters()) {
            JavaFileInfo.FieldInfo fieldInfo = new JavaFileInfo.FieldInfo(
                    param.getNameAsString(),
                    param.getTypeAsString()
            );
            param.getRange().ifPresent(range -> fieldInfo.setLine(range.begin.line));
            extractAnnotations(param.getAnnotations(), fieldInfo);
            classInfo.addField(fieldInfo);
        }

        // Extract methods
        for (MethodDeclaration method : recordDecl.getMethods()) {
            JavaFileInfo.MethodInfo methodInfo = extractMethodInfo(method);
            classInfo.addMethod(methodInfo);
        }

        return classInfo;
    }

    /**
     * Extract information from an annotation declaration.
     */
    private JavaFileInfo.ClassInfo extractAnnotationInfo(AnnotationDeclaration annotationDecl) {
        JavaFileInfo.ClassInfo classInfo = new JavaFileInfo.ClassInfo(annotationDecl.getNameAsString(), "annotation");

        // Extract modifiers
        annotationDecl.getModifiers().forEach(mod ->
                classInfo.addModifier(mod.getKeyword().asString()));

        // Extract line numbers
        annotationDecl.getRange().ifPresent(range -> {
            classInfo.setStartLine(range.begin.line);
            classInfo.setEndLine(range.end.line);
        });

        return classInfo;
    }

    /**
     * Extract field information.
     */
    private void extractFields(FieldDeclaration field, JavaFileInfo.ClassInfo classInfo) {
        for (VariableDeclarator variable : field.getVariables()) {
            JavaFileInfo.FieldInfo fieldInfo = new JavaFileInfo.FieldInfo(
                    variable.getNameAsString(),
                    variable.getTypeAsString()
            );

            // Extract modifiers
            field.getModifiers().forEach(mod ->
                    fieldInfo.addModifier(mod.getKeyword().asString()));

            // Extract annotations
            extractAnnotations(field.getAnnotations(), fieldInfo);

            // Extract line number
            variable.getRange().ifPresent(range ->
                    fieldInfo.setLine(range.begin.line));

            classInfo.addField(fieldInfo);
        }
    }

    /**
     * Extract method information.
     */
    private JavaFileInfo.MethodInfo extractMethodInfo(MethodDeclaration method) {
        JavaFileInfo.MethodInfo methodInfo = new JavaFileInfo.MethodInfo(
                method.getNameAsString(),
                method.getTypeAsString()
        );

        // Extract modifiers
        method.getModifiers().forEach(mod ->
                methodInfo.addModifier(mod.getKeyword().asString()));

        // Extract annotations
        extractAnnotations(method.getAnnotations(), methodInfo);

        // Extract parameters
        for (Parameter param : method.getParameters()) {
            JavaFileInfo.ParameterInfo paramInfo = new JavaFileInfo.ParameterInfo(
                    param.getNameAsString(),
                    param.getTypeAsString()
            );
            extractAnnotations(param.getAnnotations(), paramInfo);
            methodInfo.addParameter(paramInfo);
        }

        // Extract line numbers
        method.getRange().ifPresent(range -> {
            methodInfo.setStartLine(range.begin.line);
            methodInfo.setEndLine(range.end.line);
        });

        // Calculate cyclomatic complexity
        int complexity = complexityAnalyzer.calculateComplexity(method);
        methodInfo.setComplexity(complexity);

        return methodInfo;
    }

    /**
     * Extract constructor information.
     */
    private JavaFileInfo.MethodInfo extractConstructorInfo(ConstructorDeclaration constructor) {
        JavaFileInfo.MethodInfo methodInfo = new JavaFileInfo.MethodInfo(
                constructor.getNameAsString(),
                "void" // Constructors don't have return type
        );

        // Extract modifiers
        constructor.getModifiers().forEach(mod ->
                methodInfo.addModifier(mod.getKeyword().asString()));

        // Extract annotations
        extractAnnotations(constructor.getAnnotations(), methodInfo);

        // Extract parameters
        for (Parameter param : constructor.getParameters()) {
            JavaFileInfo.ParameterInfo paramInfo = new JavaFileInfo.ParameterInfo(
                    param.getNameAsString(),
                    param.getTypeAsString()
            );
            extractAnnotations(param.getAnnotations(), paramInfo);
            methodInfo.addParameter(paramInfo);
        }

        // Extract line numbers
        constructor.getRange().ifPresent(range -> {
            methodInfo.setStartLine(range.begin.line);
            methodInfo.setEndLine(range.end.line);
        });

        return methodInfo;
    }

    /**
     * Extract annotations from a node.
     */
    private void extractAnnotations(NodeList<AnnotationExpr> annotations, JavaFileInfo.ClassInfo classInfo) {
        for (AnnotationExpr annotation : annotations) {
            classInfo.addAnnotation(annotation.getNameAsString());
        }
    }

    private void extractAnnotations(NodeList<AnnotationExpr> annotations, JavaFileInfo.FieldInfo fieldInfo) {
        for (AnnotationExpr annotation : annotations) {
            String name = annotation.getNameAsString();
            fieldInfo.addAnnotation(name);
            // Store full annotation string with parameters (e.g., "@OneToMany(mappedBy = \"author\")")
            fieldInfo.addAnnotationDetail(name, annotation.toString());
        }
    }

    private void extractAnnotations(NodeList<AnnotationExpr> annotations, JavaFileInfo.MethodInfo methodInfo) {
        for (AnnotationExpr annotation : annotations) {
            String name = annotation.getNameAsString();
            methodInfo.addAnnotation(name);
            // Store full annotation string with parameters (e.g., "@Before(\"execution(* com.example.*.*(..))\")")
            methodInfo.addAnnotationDetail(name, annotation.toString());
        }
    }

    private void extractAnnotations(NodeList<AnnotationExpr> annotations, JavaFileInfo.ParameterInfo paramInfo) {
        for (AnnotationExpr annotation : annotations) {
            paramInfo.addAnnotation(annotation.getNameAsString());
        }
    }
}
