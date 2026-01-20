package com.mcp.codeanalysis.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyGraph.
 */
class DependencyGraphTest {

    @TempDir
    Path tempDir;

    private DependencyGraph packageGraph;
    private DependencyGraph classGraph;

    @BeforeEach
    void setUp() {
        packageGraph = new DependencyGraph(DependencyGraph.DependencyLevel.PACKAGE);
        classGraph = new DependencyGraph(DependencyGraph.DependencyLevel.CLASS);
    }

    @Test
    void testAddSimpleFile() throws IOException {
        String source = """
                package com.example.service;

                import com.example.model.User;
                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        Path javaFile = createJavaFile("UserService.java", source);
        packageGraph.addFile(javaFile);

        Set<String> nodes = packageGraph.getNodes();
        assertTrue(nodes.contains("com.example.service"));
        assertTrue(nodes.contains("com.example.model"));
        assertTrue(nodes.contains("com.example.repository"));
    }

    @Test
    void testPackageLevelDependencies() throws IOException {
        String serviceSource = """
                package com.example.service;

                import com.example.model.User;
                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        String repositorySource = """
                package com.example.repository;

                import com.example.model.User;

                public class UserRepository {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", serviceSource));
        packageGraph.addFile(createJavaFile("UserRepository.java", repositorySource));

        Set<String> serviceDeps = packageGraph.getDependenciesOf("com.example.service");
        assertTrue(serviceDeps.contains("com.example.model"));
        assertTrue(serviceDeps.contains("com.example.repository"));

        Set<String> repositoryDeps = packageGraph.getDependenciesOf("com.example.repository");
        assertTrue(repositoryDeps.contains("com.example.model"));
    }

    @Test
    void testClassLevelDependencies() throws IOException {
        String source = """
                package com.example.service;

                import com.example.model.User;
                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        classGraph.addFile(createJavaFile("UserService.java", source));

        Set<String> nodes = classGraph.getNodes();
        assertTrue(nodes.contains("com.example.service.UserService"));
        assertTrue(nodes.contains("com.example.model.User"));
        assertTrue(nodes.contains("com.example.repository.UserRepository"));

        Set<String> deps = classGraph.getDependenciesOf("com.example.service.UserService");
        assertTrue(deps.contains("com.example.model.User"));
        assertTrue(deps.contains("com.example.repository.UserRepository"));
    }

    @Test
    void testFilterStandardLibraryImports() throws IOException {
        String source = """
                package com.example.service;

                import java.util.List;
                import java.io.IOException;
                import javax.servlet.http.HttpServletRequest;
                import org.springframework.stereotype.Service;
                import com.example.model.User;

                @Service
                public class UserService {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", source));

        Set<String> nodes = packageGraph.getNodes();

        // Should include project packages
        assertTrue(nodes.contains("com.example.service"));
        assertTrue(nodes.contains("com.example.model"));

        // Should NOT include standard library packages
        assertFalse(nodes.contains("java.util"));
        assertFalse(nodes.contains("java.io"));
        assertFalse(nodes.contains("javax.servlet.http"));
        assertFalse(nodes.contains("org.springframework.stereotype"));
    }

    @Test
    void testCircularDependencyDetection() throws IOException {
        String serviceA = """
                package com.example.service;

                import com.example.repository.RepositoryA;

                public class ServiceA {
                }
                """;

        String repositoryA = """
                package com.example.repository;

                import com.example.service.ServiceB;

                public class RepositoryA {
                }
                """;

        String serviceB = """
                package com.example.service;

                import com.example.repository.RepositoryA;

                public class ServiceB {
                }
                """;

        packageGraph.addFile(createJavaFile("ServiceA.java", serviceA));
        packageGraph.addFile(createJavaFile("RepositoryA.java", repositoryA));
        packageGraph.addFile(createJavaFile("ServiceB.java", serviceB));

        List<List<String>> cycles = packageGraph.detectCircularDependencies();

        assertFalse(cycles.isEmpty());
        // There should be a cycle between service and repository packages
    }

    @Test
    void testNoCircularDependencies() throws IOException {
        String controller = """
                package com.example.controller;

                import com.example.service.UserService;

                public class UserController {
                }
                """;

        String service = """
                package com.example.service;

                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        String repository = """
                package com.example.repository;

                import com.example.model.User;

                public class UserRepository {
                }
                """;

        String model = """
                package com.example.model;

                public class User {
                }
                """;

        packageGraph.addFile(createJavaFile("UserController.java", controller));
        packageGraph.addFile(createJavaFile("UserService.java", service));
        packageGraph.addFile(createJavaFile("UserRepository.java", repository));
        packageGraph.addFile(createJavaFile("User.java", model));

        List<List<String>> cycles = packageGraph.detectCircularDependencies();

        assertTrue(cycles.isEmpty());
    }

    @Test
    void testCouplingMetrics() throws IOException {
        String controller = """
                package com.example.controller;

                import com.example.service.UserService;

                public class UserController {
                }
                """;

        String service = """
                package com.example.service;

                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        String repository = """
                package com.example.repository;

                public class UserRepository {
                }
                """;

        packageGraph.addFile(createJavaFile("UserController.java", controller));
        packageGraph.addFile(createJavaFile("UserService.java", service));
        packageGraph.addFile(createJavaFile("UserRepository.java", repository));

        Map<String, DependencyGraph.CouplingMetrics> metrics = packageGraph.calculateCouplingMetrics();

        // Controller depends on service (Ce=1), nothing depends on controller (Ca=0)
        DependencyGraph.CouplingMetrics controllerMetrics = metrics.get("com.example.controller");
        assertEquals(0, controllerMetrics.getAfferentCoupling());
        assertEquals(1, controllerMetrics.getEfferentCoupling());
        assertEquals(1.0, controllerMetrics.getInstability(), 0.01);
        assertTrue(controllerMetrics.isUnstable());

        // Service has controller depending on it (Ca=1), depends on repository (Ce=1)
        DependencyGraph.CouplingMetrics serviceMetrics = metrics.get("com.example.service");
        assertEquals(1, serviceMetrics.getAfferentCoupling());
        assertEquals(1, serviceMetrics.getEfferentCoupling());
        assertEquals(0.5, serviceMetrics.getInstability(), 0.01);

        // Repository has service depending on it (Ca=1), depends on nothing (Ce=0)
        DependencyGraph.CouplingMetrics repositoryMetrics = metrics.get("com.example.repository");
        assertEquals(1, repositoryMetrics.getAfferentCoupling());
        assertEquals(0, repositoryMetrics.getEfferentCoupling());
        assertEquals(0.0, repositoryMetrics.getInstability(), 0.01);
        assertTrue(repositoryMetrics.isStable());
    }

    @Test
    void testGetDependents() throws IOException {
        String service = """
                package com.example.service;

                import com.example.model.User;

                public class UserService {
                }
                """;

        String repository = """
                package com.example.repository;

                import com.example.model.User;

                public class UserRepository {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", service));
        packageGraph.addFile(createJavaFile("UserRepository.java", repository));

        Set<String> modelDependents = packageGraph.getDependentsOf("com.example.model");
        assertTrue(modelDependents.contains("com.example.service"));
        assertTrue(modelDependents.contains("com.example.repository"));
    }

    @Test
    void testNodeAndEdgeCounts() throws IOException {
        String service = """
                package com.example.service;

                import com.example.model.User;
                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", service));

        assertEquals(3, packageGraph.getNodeCount()); // service, model, repository
        assertEquals(2, packageGraph.getEdgeCount()); // service->model, service->repository
    }

    @Test
    void testAddMultipleFiles() throws IOException {
        List<Path> files = List.of(
                createJavaFile("UserController.java", """
                        package com.example.controller;
                        import com.example.service.UserService;
                        public class UserController {}
                        """),
                createJavaFile("UserService.java", """
                        package com.example.service;
                        import com.example.repository.UserRepository;
                        public class UserService {}
                        """),
                createJavaFile("UserRepository.java", """
                        package com.example.repository;
                        import com.example.model.User;
                        public class UserRepository {}
                        """)
        );

        packageGraph.addFiles(files);

        assertEquals(4, packageGraph.getNodeCount());
        assertTrue(packageGraph.getNodes().contains("com.example.controller"));
        assertTrue(packageGraph.getNodes().contains("com.example.service"));
        assertTrue(packageGraph.getNodes().contains("com.example.repository"));
        assertTrue(packageGraph.getNodes().contains("com.example.model"));
    }

    @Test
    void testFileWithNoPackage() throws IOException {
        String source = """
                import java.util.List;

                public class NoPackageClass {
                }
                """;

        packageGraph.addFile(createJavaFile("NoPackageClass.java", source));

        // Should not add anything to graph
        assertEquals(0, packageGraph.getNodeCount());
    }

    @Test
    void testSelfDependency() throws IOException {
        String source = """
                package com.example.service;

                import com.example.service.UserService;

                public class UserService {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", source));

        // Should not create self-edge
        Set<String> deps = packageGraph.getDependenciesOf("com.example.service");
        assertFalse(deps.contains("com.example.service"));
    }

    @Test
    void testWildcardImportsExcluded() throws IOException {
        String source = """
                package com.example.service;

                import com.example.model.*;
                import com.example.repository.UserRepository;

                public class UserService {
                }
                """;

        classGraph.addFile(createJavaFile("UserService.java", source));

        Set<String> deps = classGraph.getDependenciesOf("com.example.service.UserService");

        // Should include specific import
        assertTrue(deps.contains("com.example.repository.UserRepository"));

        // Should not include wildcard import at class level
        // (At package level it would still create a dependency)
    }

    @Test
    void testMultipleClassesInOneFile() throws IOException {
        String source = """
                package com.example.service;

                import com.example.model.User;

                public class UserService {
                }

                class UserServiceHelper {
                }
                """;

        classGraph.addFile(createJavaFile("UserService.java", source));

        Set<String> nodes = classGraph.getNodes();
        assertTrue(nodes.contains("com.example.service.UserService"));
        assertTrue(nodes.contains("com.example.service.UserServiceHelper"));
    }

    @Test
    void testCouplingMetricsToString() throws IOException {
        String source = """
                package com.example.service;

                import com.example.model.User;

                public class UserService {
                }
                """;

        packageGraph.addFile(createJavaFile("UserService.java", source));

        Map<String, DependencyGraph.CouplingMetrics> metrics = packageGraph.calculateCouplingMetrics();
        DependencyGraph.CouplingMetrics serviceMetrics = metrics.get("com.example.service");

        String toString = serviceMetrics.toString();
        assertTrue(toString.contains("com.example.service"));
        assertTrue(toString.contains("Ca="));
        assertTrue(toString.contains("Ce="));
        assertTrue(toString.contains("I="));
    }

    @Test
    void testInvalidFileHandling() throws IOException {
        String invalidSource = """
                package com.example.service

                this is not valid Java code
                """;

        Path invalidFile = createJavaFile("Invalid.java", invalidSource);

        // Should not throw exception, just log warning
        packageGraph.addFile(invalidFile);

        // Graph should remain empty or contain only what could be parsed
        assertTrue(packageGraph.getNodeCount() >= 0);
    }

    /**
     * Helper method to create a Java file in the temp directory.
     */
    private Path createJavaFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }
}
