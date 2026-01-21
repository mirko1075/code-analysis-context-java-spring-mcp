package com.mcp.codeanalysis.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComplexityAnalyzer.
 */
class ComplexityAnalyzerTest {

    private ComplexityAnalyzer analyzer;
    private JavaParser javaParser;

    @BeforeEach
    void setUp() {
        analyzer = new ComplexityAnalyzer();
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        javaParser = new JavaParser(config);
    }

    @Test
    void testSimpleMethodComplexity() {
        String code = """
                public class Test {
                    public void simpleMethod() {
                        System.out.println("Hello");
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(1, complexity, "Simple method should have complexity 1");
    }

    @Test
    void testMethodWithIfStatement() {
        String code = """
                public class Test {
                    public void methodWithIf(boolean condition) {
                        if (condition) {
                            System.out.println("True");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with if should have complexity 2");
    }

    @Test
    void testMethodWithIfElse() {
        String code = """
                public class Test {
                    public void methodWithIfElse(boolean condition) {
                        if (condition) {
                            System.out.println("True");
                        } else {
                            System.out.println("False");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with if-else should have complexity 2");
    }

    @Test
    void testMethodWithForLoop() {
        String code = """
                public class Test {
                    public void methodWithFor() {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(i);
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with for loop should have complexity 2");
    }

    @Test
    void testMethodWithWhileLoop() {
        String code = """
                public class Test {
                    public void methodWithWhile(int count) {
                        while (count > 0) {
                            count--;
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with while loop should have complexity 2");
    }

    @Test
    void testMethodWithDoWhileLoop() {
        String code = """
                public class Test {
                    public void methodWithDoWhile(int count) {
                        do {
                            count--;
                        } while (count > 0);
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with do-while loop should have complexity 2");
    }

    @Test
    void testMethodWithForEachLoop() {
        String code = """
                public class Test {
                    public void methodWithForEach(int[] numbers) {
                        for (int num : numbers) {
                            System.out.println(num);
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with for-each loop should have complexity 2");
    }

    @Test
    void testMethodWithSwitch() {
        String code = """
                public class Test {
                    public void methodWithSwitch(int value) {
                        switch (value) {
                            case 1:
                                System.out.println("One");
                                break;
                            case 2:
                                System.out.println("Two");
                                break;
                            default:
                                System.out.println("Other");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 3 (switch entries: case 1, case 2, default)
        assertEquals(4, complexity, "Method with switch should count each case");
    }

    @Test
    void testMethodWithTryCatch() {
        String code = """
                public class Test {
                    public void methodWithTryCatch() {
                        try {
                            System.out.println("Try");
                        } catch (Exception e) {
                            System.out.println("Catch");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with catch should have complexity 2");
    }

    @Test
    void testMethodWithMultipleCatch() {
        String code = """
                public class Test {
                    public void methodWithMultipleCatch() {
                        try {
                            System.out.println("Try");
                        } catch (IllegalArgumentException e) {
                            System.out.println("IllegalArgument");
                        } catch (Exception e) {
                            System.out.println("Exception");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 2 (two catch clauses)
        assertEquals(3, complexity, "Method with multiple catches should count each catch");
    }

    @Test
    void testMethodWithTernaryOperator() {
        String code = """
                public class Test {
                    public int methodWithTernary(boolean condition) {
                        return condition ? 1 : 0;
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        assertEquals(2, complexity, "Method with ternary operator should have complexity 2");
    }

    @Test
    void testMethodWithLogicalAnd() {
        String code = """
                public class Test {
                    public void methodWithAnd(boolean a, boolean b) {
                        if (a && b) {
                            System.out.println("Both true");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 1 (if) + 1 (&&)
        assertEquals(3, complexity, "Method with && should count logical operator");
    }

    @Test
    void testMethodWithLogicalOr() {
        String code = """
                public class Test {
                    public void methodWithOr(boolean a, boolean b) {
                        if (a || b) {
                            System.out.println("At least one true");
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 1 (if) + 1 (||)
        assertEquals(3, complexity, "Method with || should count logical operator");
    }

    @Test
    void testComplexMethod() {
        String code = """
                public class Test {
                    public void complexMethod(int value, boolean flag) {
                        if (value > 0) {
                            for (int i = 0; i < value; i++) {
                                if (flag && i % 2 == 0) {
                                    System.out.println("Even");
                                } else {
                                    System.out.println("Odd");
                                }
                            }
                        } else if (value < 0) {
                            while (value < 0) {
                                value++;
                            }
                        }

                        try {
                            processValue(value);
                        } catch (Exception e) {
                            handleError(e);
                        }
                    }

                    void processValue(int v) {}
                    void handleError(Exception e) {}
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 1 (if) + 1 (for) + 1 (if) + 1 (&&) + 1 (else if) + 1 (while) + 1 (catch)
        assertEquals(8, complexity, "Complex method should sum all decision points");
    }

    @Test
    void testNullMethod() {
        int complexity = analyzer.calculateComplexity(null);
        assertEquals(0, complexity, "Null method should have complexity 0");
    }

    @Test
    void testGetComplexityLevel() {
        assertEquals("low", analyzer.getComplexityLevel(3));
        assertEquals("low", analyzer.getComplexityLevel(5));
        assertEquals("moderate", analyzer.getComplexityLevel(7));
        assertEquals("moderate", analyzer.getComplexityLevel(10));
        assertEquals("high", analyzer.getComplexityLevel(15));
        assertEquals("high", analyzer.getComplexityLevel(20));
        assertEquals("very-high", analyzer.getComplexityLevel(25));
    }

    @Test
    void testGetRiskAssessment() {
        String lowRisk = analyzer.getRiskAssessment(3);
        assertTrue(lowRisk.contains("Low risk"));

        String moderateRisk = analyzer.getRiskAssessment(8);
        assertTrue(moderateRisk.contains("Moderate risk"));

        String highRisk = analyzer.getRiskAssessment(15);
        assertTrue(highRisk.contains("High risk"));

        String veryHighRisk = analyzer.getRiskAssessment(25);
        assertTrue(veryHighRisk.contains("Very high risk"));
    }

    @Test
    void testCalculateMaintainabilityIndex() {
        // Simple method: low complexity, few lines -> high maintainability
        int mi1 = analyzer.calculateMaintainabilityIndex(2, 5);
        assertTrue(mi1 > 80, "Simple method should have high maintainability");

        // Moderate method
        int mi2 = analyzer.calculateMaintainabilityIndex(10, 50);
        assertTrue(mi2 >= 40 && mi2 < 80, "Moderate method should have moderate maintainability");

        // Complex method: high complexity, many lines -> low maintainability
        int mi3 = analyzer.calculateMaintainabilityIndex(25, 100);
        assertTrue(mi3 < 40, "Complex method should have low maintainability");

        // Edge case: zero lines
        int mi4 = analyzer.calculateMaintainabilityIndex(5, 0);
        assertEquals(100, mi4, "Zero lines should return max maintainability");

        // Edge case: result should be clamped to 0-100
        int mi5 = analyzer.calculateMaintainabilityIndex(50, 500);
        assertTrue(mi5 >= 0 && mi5 <= 100, "Maintainability should be clamped to 0-100");
    }

    @Test
    void testGetMaintainabilityLevel() {
        assertEquals("excellent", analyzer.getMaintainabilityLevel(85));
        assertEquals("good", analyzer.getMaintainabilityLevel(70));
        assertEquals("moderate", analyzer.getMaintainabilityLevel(50));
        assertEquals("poor", analyzer.getMaintainabilityLevel(30));
        assertEquals("critical", analyzer.getMaintainabilityLevel(10));
    }

    @Test
    void testMethodWithNestedConditions() {
        String code = """
                public class Test {
                    public void nestedConditions(int a, int b, int c) {
                        if (a > 0) {
                            if (b > 0) {
                                if (c > 0) {
                                    System.out.println("All positive");
                                }
                            }
                        }
                    }
                }
                """;

        MethodDeclaration method = extractFirstMethod(code);
        int complexity = analyzer.calculateComplexity(method);

        // 1 (base) + 3 (three nested ifs)
        assertEquals(4, complexity, "Nested conditions should sum all decision points");
    }

    /**
     * Helper method to extract the first method from a code snippet.
     */
    private MethodDeclaration extractFirstMethod(String code) {
        ParseResult<CompilationUnit> parseResult = javaParser.parse(code);
        assertTrue(parseResult.isSuccessful(), "Code should parse successfully");

        Optional<CompilationUnit> cuOpt = parseResult.getResult();
        assertTrue(cuOpt.isPresent(), "Compilation unit should be present");

        CompilationUnit cu = cuOpt.get();
        MethodDeclaration method = cu.findFirst(MethodDeclaration.class).orElse(null);
        assertNotNull(method, "Method should be found");

        return method;
    }
}
