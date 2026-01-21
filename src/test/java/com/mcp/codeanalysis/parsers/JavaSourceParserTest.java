package com.mcp.codeanalysis.parsers;

import com.mcp.codeanalysis.types.JavaFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaSourceParser.
 */
class JavaSourceParserTest {

    private JavaSourceParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParser();
    }

    @Test
    void testParseSimpleClass() throws IOException {
        String javaCode = """
                package com.example.test;

                import java.util.List;
                import java.util.ArrayList;

                /**
                 * A simple test class.
                 */
                public class SimpleClass {
                    private String name;
                    private int count;

                    public SimpleClass(String name) {
                        this.name = name;
                        this.count = 0;
                    }

                    public String getName() {
                        return name;
                    }

                    public void setName(String name) {
                        this.name = name;
                    }
                }
                """;

        Path javaFile = tempDir.resolve("SimpleClass.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        assertEquals("com.example.test", fileInfo.getPackageName());
        assertEquals(2, fileInfo.getImports().size());
        assertTrue(fileInfo.getImports().contains("java.util.List"));
        assertTrue(fileInfo.getImports().contains("java.util.ArrayList"));

        assertEquals(1, fileInfo.getClasses().size());
        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);
        assertEquals("SimpleClass", classInfo.getName());
        assertEquals("class", classInfo.getType());
        assertTrue(classInfo.getModifiers().contains("public"));

        // Check fields
        assertEquals(2, classInfo.getFields().size());
        assertTrue(classInfo.getFields().stream()
                .anyMatch(f -> "name".equals(f.getName()) && "String".equals(f.getType())));
        assertTrue(classInfo.getFields().stream()
                .anyMatch(f -> "count".equals(f.getName()) && "int".equals(f.getType())));

        // Check methods (constructor + 2 methods)
        assertEquals(3, classInfo.getMethods().size());
        assertTrue(classInfo.getMethods().stream()
                .anyMatch(m -> "SimpleClass".equals(m.getName())));
        assertTrue(classInfo.getMethods().stream()
                .anyMatch(m -> "getName".equals(m.getName()) && "String".equals(m.getReturnType())));
        assertTrue(classInfo.getMethods().stream()
                .anyMatch(m -> "setName".equals(m.getName())));
    }

    @Test
    void testParseClassWithAnnotations() throws IOException {
        String javaCode = """
                package com.example.spring;

                import org.springframework.stereotype.Service;
                import org.springframework.beans.factory.annotation.Autowired;

                @Service
                public class UserService {

                    @Autowired
                    private UserRepository userRepository;

                    public void save(User user) {
                        userRepository.save(user);
                    }
                }
                """;

        Path javaFile = tempDir.resolve("UserService.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getClasses().size());

        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);
        assertEquals("UserService", classInfo.getName());

        // Check class annotation
        assertTrue(classInfo.getAnnotations().contains("Service"));

        // Check field annotation
        assertEquals(1, classInfo.getFields().size());
        JavaFileInfo.FieldInfo field = classInfo.getFields().get(0);
        assertEquals("userRepository", field.getName());
        assertTrue(field.getAnnotations().contains("Autowired"));
    }

    @Test
    void testParseInterface() throws IOException {
        String javaCode = """
                package com.example.repository;

                import org.springframework.data.jpa.repository.JpaRepository;

                public interface UserRepository extends JpaRepository<User, Long> {
                    User findByUsername(String username);
                }
                """;

        Path javaFile = tempDir.resolve("UserRepository.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getClasses().size());

        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);
        assertEquals("UserRepository", classInfo.getName());
        assertEquals("interface", classInfo.getType());
        assertEquals("JpaRepository", classInfo.getExtendsClass());

        // Check method
        assertEquals(1, classInfo.getMethods().size());
        JavaFileInfo.MethodInfo method = classInfo.getMethods().get(0);
        assertEquals("findByUsername", method.getName());
        assertEquals("User", method.getReturnType());
        assertEquals(1, method.getParameters().size());
        assertEquals("username", method.getParameters().get(0).getName());
        assertEquals("String", method.getParameters().get(0).getType());
    }

    @Test
    void testParseEnum() throws IOException {
        String javaCode = """
                package com.example.enums;

                public enum Status {
                    ACTIVE,
                    INACTIVE,
                    PENDING;

                    public boolean isActive() {
                        return this == ACTIVE;
                    }
                }
                """;

        Path javaFile = tempDir.resolve("Status.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getClasses().size());

        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);
        assertEquals("Status", classInfo.getName());
        assertEquals("enum", classInfo.getType());

        // Check method
        assertEquals(1, classInfo.getMethods().size());
        assertEquals("isActive", classInfo.getMethods().get(0).getName());
    }

    @Test
    void testParseRecord() throws IOException {
        String javaCode = """
                package com.example.dto;

                public record UserDTO(String username, String email, int age) {
                    public boolean isAdult() {
                        return age >= 18;
                    }
                }
                """;

        Path javaFile = tempDir.resolve("UserDTO.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        assertEquals(1, fileInfo.getClasses().size());

        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);
        assertEquals("UserDTO", classInfo.getName());
        assertEquals("record", classInfo.getType());

        // Check record components (as fields)
        assertEquals(3, classInfo.getFields().size());
        assertTrue(classInfo.getFields().stream()
                .anyMatch(f -> "username".equals(f.getName()) && "String".equals(f.getType())));
        assertTrue(classInfo.getFields().stream()
                .anyMatch(f -> "email".equals(f.getName()) && "String".equals(f.getType())));
        assertTrue(classInfo.getFields().stream()
                .anyMatch(f -> "age".equals(f.getName()) && "int".equals(f.getType())));

        // Check method
        assertEquals(1, classInfo.getMethods().size());
        assertEquals("isAdult", classInfo.getMethods().get(0).getName());
    }

    @Test
    void testParseRestController() throws IOException {
        String javaCode = """
                package com.example.controller;

                import org.springframework.web.bind.annotation.*;

                @RestController
                @RequestMapping("/api/users")
                public class UserController {

                    @GetMapping("/{id}")
                    public User getUser(@PathVariable Long id) {
                        return null;
                    }

                    @PostMapping
                    public User createUser(@RequestBody User user) {
                        return null;
                    }
                }
                """;

        Path javaFile = tempDir.resolve("UserController.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);

        // Check class annotations
        assertTrue(classInfo.getAnnotations().contains("RestController"));
        assertTrue(classInfo.getAnnotations().contains("RequestMapping"));

        // Check methods with annotations
        assertEquals(2, classInfo.getMethods().size());

        JavaFileInfo.MethodInfo getMethod = classInfo.getMethods().stream()
                .filter(m -> "getUser".equals(m.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(getMethod);
        assertTrue(getMethod.getAnnotations().contains("GetMapping"));
        assertEquals(1, getMethod.getParameters().size());
        assertTrue(getMethod.getParameters().get(0).getAnnotations().contains("PathVariable"));

        JavaFileInfo.MethodInfo postMethod = classInfo.getMethods().stream()
                .filter(m -> "createUser".equals(m.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(postMethod);
        assertTrue(postMethod.getAnnotations().contains("PostMapping"));
        assertTrue(postMethod.getParameters().get(0).getAnnotations().contains("RequestBody"));
    }

    @Test
    void testParseInvalidFile() {
        Path invalidFile = tempDir.resolve("NonExistent.java");

        JavaFileInfo fileInfo = parser.parseFile(invalidFile);

        assertNull(fileInfo);
    }

    @Test
    void testParseSyntaxError() throws IOException {
        String invalidJavaCode = """
                package com.example;

                public class Invalid {
                    this is not valid java code
                }
                """;

        Path javaFile = tempDir.resolve("Invalid.java");
        Files.writeString(javaFile, invalidJavaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        // Parser should still create JavaFileInfo but with limited data
        assertNotNull(fileInfo);
        // Package name will be null when parsing fails completely
        assertNull(fileInfo.getPackageName());
        // Classes list will be empty due to syntax errors
        assertTrue(fileInfo.getClasses().isEmpty());
    }

    @Test
    void testLineNumbers() throws IOException {
        String javaCode = """
                package com.example;

                public class LineTest {
                    public void method1() {
                        // line 5
                    }

                    public void method2() {
                        // line 9
                    }
                }
                """;

        Path javaFile = tempDir.resolve("LineTest.java");
        Files.writeString(javaFile, javaCode);

        JavaFileInfo fileInfo = parser.parseFile(javaFile);

        assertNotNull(fileInfo);
        JavaFileInfo.ClassInfo classInfo = fileInfo.getClasses().get(0);

        // Check class line numbers
        assertTrue(classInfo.getStartLine() > 0);
        assertTrue(classInfo.getEndLine() > classInfo.getStartLine());

        // Check method line numbers
        for (JavaFileInfo.MethodInfo method : classInfo.getMethods()) {
            assertTrue(method.getStartLine() > 0);
            assertTrue(method.getEndLine() >= method.getStartLine());
        }
    }
}
