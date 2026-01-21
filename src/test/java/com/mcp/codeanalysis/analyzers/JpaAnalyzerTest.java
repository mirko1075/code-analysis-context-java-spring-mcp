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
 * Unit tests for JpaAnalyzer.
 */
class JpaAnalyzerTest {

    @TempDir
    Path tempDir;

    private JpaAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JpaAnalyzer();
    }

    @Test
    void testSimpleEntityDetection() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.Entity;
                import javax.persistence.Id;

                @Entity
                public class User {
                    @Id
                    private Long id;
                    private String name;

                    public User() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("User.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getEntityCount());
        assertEquals("User", result.getEntities().get(0).getClassName());
        assertTrue(result.getEntities().get(0).getIssues().isEmpty());
    }

    @Test
    void testOneToManyRelationship() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;
                import java.util.List;

                @Entity
                public class Author {
                    @Id
                    private Long id;

                    @OneToMany(mappedBy = "author")
                    private List<Book> books;

                    public Author() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Author.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getEntityCount());
        JpaAnalyzer.EntityInfo entity = result.getEntities().get(0);
        assertEquals(1, entity.getRelationships().size());

        JpaAnalyzer.RelationshipInfo rel = entity.getRelationships().get(0);
        assertEquals("books", rel.getFieldName());
        assertEquals("OneToMany", rel.getRelationshipType());
        assertTrue(rel.isBidirectional());
    }

    @Test
    void testManyToOneRelationship() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;

                @Entity
                public class Book {
                    @Id
                    private Long id;

                    @ManyToOne
                    private Author author;

                    public Book() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Book.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getRelationshipCount());
        JpaAnalyzer.RelationshipInfo rel = result.getEntities().get(0).getRelationships().get(0);
        assertEquals("ManyToOne", rel.getRelationshipType());
    }

    @Test
    void testManyToManyRelationship() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;
                import java.util.Set;

                @Entity
                public class Student {
                    @Id
                    private Long id;

                    @ManyToMany
                    private Set<Course> courses;

                    public Student() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Student.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        JpaAnalyzer.RelationshipInfo rel = result.getEntities().get(0).getRelationships().get(0);
        assertEquals("ManyToMany", rel.getRelationshipType());
    }

    @Test
    void testMissingIdAnnotation() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.Entity;

                @Entity
                public class InvalidEntity {
                    private Long id;

                    public InvalidEntity() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("InvalidEntity.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getEntitiesWithIssues().size());
        assertTrue(result.getEntities().get(0).getIssues().stream()
                .anyMatch(issue -> issue.contains("@Id")));
    }

    @Test
    void testMissingNoArgConstructor() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;

                @Entity
                public class Product {
                    @Id
                    private Long id;

                    public Product(Long id) {
                        this.id = id;
                    }

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Product.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertTrue(result.getEntities().get(0).getIssues().stream()
                .anyMatch(issue -> issue.contains("no-argument constructor")));
    }

    @Test
    void testMissingEqualsHashCode() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;

                @Entity
                public class Order {
                    @Id
                    private Long id;

                    public Order() {}
                }
                """;

        Path entityFile = createJavaFile("Order.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertTrue(result.getEntities().get(0).getIssues().stream()
                .anyMatch(issue -> issue.contains("equals") || issue.contains("hashCode")));
    }

    @Test
    void testNPlusOneQueryDetection() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;
                import java.util.List;

                @Entity
                public class Department {
                    @Id
                    private Long id;

                    @OneToMany(fetch = FetchType.EAGER)
                    private List<Employee> employees;

                    public Department() {}

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Department.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertFalse(result.getAllPotentialNPlusOneQueries().isEmpty());
        assertTrue(result.getAllPotentialNPlusOneQueries().stream()
                .anyMatch(q -> q.contains("employees")));
    }

    @Test
    void testMultipleEntities() throws IOException {
        String user = """
                package com.example.model;
                import javax.persistence.*;
                @Entity
                public class User {
                    @Id
                    private Long id;
                    public User() {}
                    @Override
                    public boolean equals(Object o) { return false; }
                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        String product = """
                package com.example.model;
                import javax.persistence.*;
                @Entity
                public class Product {
                    @Id
                    private Long id;
                    public Product() {}
                    @Override
                    public boolean equals(Object o) { return false; }
                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path userFile = createJavaFile("User.java", user);
        Path productFile = createJavaFile("Product.java", product);

        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(userFile, productFile));

        assertEquals(2, result.getEntityCount());
    }

    @Test
    void testMappedSuperclass() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.MappedSuperclass;
                import javax.persistence.Id;

                @MappedSuperclass
                public abstract class BaseEntity {
                    @Id
                    private Long id;

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("BaseEntity.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getEntityCount());
    }

    @Test
    void testEmbeddable() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.Embeddable;

                @Embeddable
                public class Address {
                    private String street;
                    private String city;

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Address.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertEquals(1, result.getEntityCount());
        // Embeddable doesn't require @Id
    }

    @Test
    void testNonEntityClass() throws IOException {
        String serviceSource = """
                package com.example.service;

                public class UserService {
                    public void doSomething() {}
                }
                """;

        Path serviceFile = createJavaFile("UserService.java", serviceSource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(serviceFile));

        assertEquals(0, result.getEntityCount());
    }

    @Test
    void testEntityWithNoExplicitConstructor() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;

                @Entity
                public class SimpleEntity {
                    @Id
                    private Long id;

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("SimpleEntity.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        // Should not have "no-argument constructor" issue
        assertFalse(result.getEntities().get(0).getIssues().stream()
                .anyMatch(issue -> issue.contains("no-argument constructor")));
    }

    @Test
    void testEntityWithBothConstructors() throws IOException {
        String entitySource = """
                package com.example.model;

                import javax.persistence.*;

                @Entity
                public class Customer {
                    @Id
                    private Long id;

                    public Customer() {}

                    public Customer(Long id) {
                        this.id = id;
                    }

                    @Override
                    public boolean equals(Object o) { return false; }

                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("Customer.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        assertFalse(result.getEntities().get(0).getIssues().stream()
                .anyMatch(issue -> issue.contains("no-argument constructor")));
    }

    @Test
    void testResultToString() throws IOException {
        String entitySource = """
                package com.example.model;
                import javax.persistence.*;
                @Entity
                public class User {
                    @Id
                    private Long id;
                    public User() {}
                    @Override
                    public boolean equals(Object o) { return false; }
                    @Override
                    public int hashCode() { return 0; }
                }
                """;

        Path entityFile = createJavaFile("User.java", entitySource);
        JpaAnalyzer.JpaAnalysisResult result = analyzer.analyze(List.of(entityFile));

        String toString = result.toString();
        assertTrue(toString.contains("entities=1"));
    }

    /**
     * Helper method to create a Java file.
     */
    private Path createJavaFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
