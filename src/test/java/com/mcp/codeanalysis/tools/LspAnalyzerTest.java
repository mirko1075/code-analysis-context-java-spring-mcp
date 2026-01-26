package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.lsp.LspBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LspAnalyzer.
 */
class LspAnalyzerTest {

    @TempDir
    Path tempDir;

    private LspAnalyzer analyzer;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = new LspAnalyzer();

        // Create a test Java file
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        testFile = srcDir.resolve("TestClass.java");
        String testCode = """
            package com.example;

            public class TestClass {
                private String name;
                private int age;

                public TestClass(String name, int age) {
                    this.name = name;
                    this.age = age;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public int getAge() {
                    return age;
                }

                public void setAge(int age) {
                    this.age = age;
                }
            }
            """;
        Files.writeString(testFile, testCode);
    }

    @Test
    void testDiagnostics() {
        // Test diagnostics operation
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "diagnostics";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("diagnostic"));
    }

    @Test
    void testSymbols() {
        // Test symbols operation
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "symbols";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getSymbols());
        assertFalse(result.getSymbols().isEmpty());

        // Should find class, fields, methods
        List<LspBridge.SymbolInfo> symbols = result.getSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.kind().equals("Class")));
        assertTrue(symbols.stream().anyMatch(s -> s.kind().equals("Field")));
        assertTrue(symbols.stream().anyMatch(s -> s.kind().equals("Method")));
    }

    @Test
    void testHoverWithoutPosition() {
        // Test hover without line/column should return error
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "hover";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Line and column required"));
    }

    @Test
    void testHoverWithPosition() {
        // Test hover with position
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "hover";
        options.line = 4;
        options.column = 20;

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
    }

    @Test
    void testDefinitionWithoutPosition() {
        // Test definition without line/column should return error
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "definition";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Line and column required"));
    }

    @Test
    void testReferencesWithPosition() {
        // Test find references
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "references";
        options.line = 4;
        options.column = 20;

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
    }

    @Test
    void testCompletions() {
        // Test code completions
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "completions";
        options.line = 12;
        options.column = 10;

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getCompletions());
        assertFalse(result.getCompletions().isEmpty());
    }

    @Test
    void testAllDiagnostics() throws IOException {
        // Create another file with error
        Path errorFile = tempDir.resolve("src/main/java/com/example/ErrorClass.java");
        String errorCode = """
            package com.example;

            public class ErrorClass {
                public void badMethod() {
                    String x = "test"
                    // Missing semicolon
                }
            }
            """;
        Files.writeString(errorFile, errorCode);

        // Test all-diagnostics operation
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString(); // Any file works for all-diagnostics
        options.operation = "all-diagnostics";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getAllDiagnostics());
    }

    @Test
    void testFileNotFound() {
        // Test with non-existent file
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = "nonexistent.java";
        options.operation = "diagnostics";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    void testUnknownOperation() {
        // Test with unknown operation
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "unknown-operation";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Unknown operation"));
    }

    @Test
    void testFormatResult() {
        // Test result formatting
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = "symbols";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);
        String formatted = analyzer.formatResult(result);

        assertNotNull(formatted);
        assertTrue(formatted.contains("## LSP Analysis Result"));
        assertTrue(formatted.contains("**File**:"));
        assertTrue(formatted.contains("**Summary**:"));
    }

    @Test
    void testFormatResultWithError() {
        // Test formatting with error
        LspAnalyzer.LspAnalysisResult result = new LspAnalyzer.LspAnalysisResult();
        result.setError("Test error message");

        String formatted = analyzer.formatResult(result);

        assertNotNull(formatted);
        assertTrue(formatted.contains("Error:"));
        assertTrue(formatted.contains("Test error message"));
    }

    @Test
    void testRelativeFilePath() {
        // Test with relative file path
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = "src/main/java/com/example/TestClass.java";
        options.operation = "diagnostics";

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
    }

    @Test
    void testDefaultOperation() {
        // Test with no operation specified (should default to diagnostics)
        LspAnalyzer.LspOptions options = new LspAnalyzer.LspOptions();
        options.file = testFile.toString();
        options.operation = null; // Will default to "diagnostics"

        LspAnalyzer.LspAnalysisResult result = analyzer.analyze(tempDir.toString(), options);

        assertNotNull(result);
        assertNull(result.getError());
        assertTrue(result.getSummary().contains("diagnostic"));
    }
}
