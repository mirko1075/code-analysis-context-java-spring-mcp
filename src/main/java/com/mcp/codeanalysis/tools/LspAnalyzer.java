package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.lsp.LspBridge;
import com.mcp.codeanalysis.utils.FileScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * LSP (Language Server Protocol) analysis tool for MCP.
 * Provides IDE-like features: diagnostics, hover, definitions, references, completions.
 */
public class LspAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(LspAnalyzer.class);

    /**
     * Analyze using LSP features.
     *
     * @param projectPath Path to project root
     * @param options LSP analysis options
     * @return LSP analysis result
     */
    public LspAnalysisResult analyze(String projectPath, LspOptions options) {
        Path projectRoot = Paths.get(projectPath);
        LspAnalysisResult result = new LspAnalysisResult();

        try {
            LspBridge lspBridge = new LspBridge(projectRoot);
            FileScanner fileScanner = new FileScanner(projectRoot);

            // Get target file
            Path targetFile = resolveTargetFile(projectRoot, options.file);
            if (targetFile == null) {
                result.setError("Target file not found: " + options.file);
                return result;
            }

            result.setFilePath(targetFile.toString());

            // Execute requested LSP operations
            if (options.operation == null || options.operation.isEmpty()) {
                options.operation = "diagnostics"; // Default
            }

            switch (options.operation.toLowerCase()) {
                case "diagnostics" -> {
                    List<LspBridge.Diagnostic> diagnostics = lspBridge.getDiagnostics(targetFile);
                    result.setDiagnostics(diagnostics);
                    result.setSummary(String.format("Found %d diagnostic(s)", diagnostics.size()));
                }

                case "hover" -> {
                    if (options.line == null || options.column == null) {
                        result.setError("Line and column required for hover operation");
                        return result;
                    }
                    LspBridge.HoverInfo hover = lspBridge.getHoverInfo(targetFile, options.line, options.column);
                    result.setHoverInfo(hover);
                    result.setSummary(hover != null ? "Hover info retrieved" : "No hover info found");
                }

                case "definition" -> {
                    if (options.line == null || options.column == null) {
                        result.setError("Line and column required for definition operation");
                        return result;
                    }
                    LspBridge.DefinitionInfo definition = lspBridge.findDefinition(targetFile, options.line, options.column);
                    result.setDefinitionInfo(definition);
                    result.setSummary(definition != null ? "Definition found" : "No definition found");
                }

                case "references" -> {
                    if (options.line == null || options.column == null) {
                        result.setError("Line and column required for references operation");
                        return result;
                    }
                    List<LspBridge.ReferenceInfo> references = lspBridge.findReferences(targetFile, options.line, options.column);
                    result.setReferences(references);
                    result.setSummary(String.format("Found %d reference(s)", references.size()));
                }

                case "completions" -> {
                    if (options.line == null || options.column == null) {
                        result.setError("Line and column required for completions operation");
                        return result;
                    }
                    List<LspBridge.CompletionItem> completions = lspBridge.getCompletions(targetFile, options.line, options.column);
                    result.setCompletions(completions);
                    result.setSummary(String.format("Found %d completion(s)", completions.size()));
                }

                case "symbols" -> {
                    List<LspBridge.SymbolInfo> symbols = lspBridge.getDocumentSymbols(targetFile);
                    result.setSymbols(symbols);
                    result.setSummary(String.format("Found %d symbol(s)", symbols.size()));
                }

                case "all-diagnostics" -> {
                    // Get diagnostics for all Java files in project
                    List<Path> javaFiles = fileScanner.scanJavaFiles();
                    Map<String, List<LspBridge.Diagnostic>> allDiagnostics = new HashMap<>();
                    int totalIssues = 0;

                    for (Path javaFile : javaFiles) {
                        List<LspBridge.Diagnostic> diagnostics = lspBridge.getDiagnostics(javaFile);
                        if (!diagnostics.isEmpty()) {
                            allDiagnostics.put(javaFile.toString(), diagnostics);
                            totalIssues += diagnostics.size();
                        }
                    }

                    result.setAllDiagnostics(allDiagnostics);
                    result.setSummary(String.format("Found %d issues in %d file(s)", totalIssues, allDiagnostics.size()));
                }

                default -> {
                    result.setError("Unknown operation: " + options.operation);
                    return result;
                }
            }

        } catch (IOException e) {
            logger.error("Error performing LSP analysis: {}", projectPath, e);
            result.setError("IO Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Resolve target file path.
     */
    private Path resolveTargetFile(Path projectRoot, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        Path target = Paths.get(filePath);
        if (target.isAbsolute()) {
            return target;
        }

        // Try relative to project root
        Path resolved = projectRoot.resolve(filePath);
        if (resolved.toFile().exists()) {
            return resolved;
        }

        // Try relative to src/main/java
        resolved = projectRoot.resolve("src/main/java").resolve(filePath);
        if (resolved.toFile().exists()) {
            return resolved;
        }

        return null;
    }

    /**
     * Format result as text.
     */
    public String formatResult(LspAnalysisResult result) {
        if (result.getError() != null) {
            return "Error: " + result.getError();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## LSP Analysis Result\n\n");
        sb.append("**File**: ").append(result.getFilePath()).append("\n");
        sb.append("**Summary**: ").append(result.getSummary()).append("\n\n");

        // Format diagnostics
        if (result.getDiagnostics() != null && !result.getDiagnostics().isEmpty()) {
            sb.append("### Diagnostics\n\n");
            for (LspBridge.Diagnostic diag : result.getDiagnostics()) {
                sb.append(String.format("- **Line %d** [%s]: %s\n",
                    diag.line(), diag.severity(), diag.message()));
            }
            sb.append("\n");
        }

        // Format all diagnostics
        if (result.getAllDiagnostics() != null && !result.getAllDiagnostics().isEmpty()) {
            sb.append("### All Project Diagnostics\n\n");
            result.getAllDiagnostics().forEach((file, diagnostics) -> {
                sb.append(String.format("**%s** (%d issues)\n", file, diagnostics.size()));
                for (LspBridge.Diagnostic diag : diagnostics) {
                    sb.append(String.format("  - Line %d [%s]: %s\n",
                        diag.line(), diag.severity(), diag.message()));
                }
                sb.append("\n");
            });
        }

        // Format hover info
        if (result.getHoverInfo() != null) {
            LspBridge.HoverInfo hover = result.getHoverInfo();
            sb.append("### Hover Information\n\n");
            sb.append(String.format("- **Name**: %s\n", hover.name()));
            sb.append(String.format("- **Type**: %s\n", hover.type()));
            sb.append(String.format("- **Kind**: %s\n\n", hover.kind()));
        }

        // Format definition info
        if (result.getDefinitionInfo() != null) {
            LspBridge.DefinitionInfo def = result.getDefinitionInfo();
            sb.append("### Definition\n\n");
            sb.append(String.format("- **Name**: %s\n", def.name()));
            sb.append(String.format("- **Type**: %s\n", def.type()));
            sb.append(String.format("- **Kind**: %s\n\n", def.kind()));
        }

        // Format references
        if (result.getReferences() != null && !result.getReferences().isEmpty()) {
            sb.append("### References\n\n");
            for (LspBridge.ReferenceInfo ref : result.getReferences()) {
                sb.append(String.format("- **%s:Line %d**: %s\n",
                    ref.filePath(), ref.line(), ref.text()));
            }
            sb.append("\n");
        }

        // Format completions
        if (result.getCompletions() != null && !result.getCompletions().isEmpty()) {
            sb.append("### Code Completions\n\n");
            for (LspBridge.CompletionItem completion : result.getCompletions()) {
                sb.append(String.format("- **%s** (%s)", completion.label(), completion.kind()));
                if (completion.detail() != null) {
                    sb.append(": ").append(completion.detail());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Format symbols
        if (result.getSymbols() != null && !result.getSymbols().isEmpty()) {
            sb.append("### Document Symbols\n\n");
            for (LspBridge.SymbolInfo symbol : result.getSymbols()) {
                sb.append(String.format("- **%s** (%s) - Line %d\n",
                    symbol.name(), symbol.kind(), symbol.line()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // Options class
    public static class LspOptions {
        public String file;           // Target file path (required)
        public String operation;      // Operation: diagnostics, hover, definition, references, completions, symbols, all-diagnostics
        public Integer line;          // Line number (for hover, definition, references, completions)
        public Integer column;        // Column number (for hover, definition, references, completions)
    }

    // Result class
    public static class LspAnalysisResult {
        private String filePath;
        private String summary;
        private String error;
        private List<LspBridge.Diagnostic> diagnostics;
        private Map<String, List<LspBridge.Diagnostic>> allDiagnostics;
        private LspBridge.HoverInfo hoverInfo;
        private LspBridge.DefinitionInfo definitionInfo;
        private List<LspBridge.ReferenceInfo> references;
        private List<LspBridge.CompletionItem> completions;
        private List<LspBridge.SymbolInfo> symbols;

        // Getters and setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public List<LspBridge.Diagnostic> getDiagnostics() { return diagnostics; }
        public void setDiagnostics(List<LspBridge.Diagnostic> diagnostics) { this.diagnostics = diagnostics; }

        public Map<String, List<LspBridge.Diagnostic>> getAllDiagnostics() { return allDiagnostics; }
        public void setAllDiagnostics(Map<String, List<LspBridge.Diagnostic>> allDiagnostics) {
            this.allDiagnostics = allDiagnostics;
        }

        public LspBridge.HoverInfo getHoverInfo() { return hoverInfo; }
        public void setHoverInfo(LspBridge.HoverInfo hoverInfo) { this.hoverInfo = hoverInfo; }

        public LspBridge.DefinitionInfo getDefinitionInfo() { return definitionInfo; }
        public void setDefinitionInfo(LspBridge.DefinitionInfo definitionInfo) {
            this.definitionInfo = definitionInfo;
        }

        public List<LspBridge.ReferenceInfo> getReferences() { return references; }
        public void setReferences(List<LspBridge.ReferenceInfo> references) { this.references = references; }

        public List<LspBridge.CompletionItem> getCompletions() { return completions; }
        public void setCompletions(List<LspBridge.CompletionItem> completions) { this.completions = completions; }

        public List<LspBridge.SymbolInfo> getSymbols() { return symbols; }
        public void setSymbols(List<LspBridge.SymbolInfo> symbols) { this.symbols = symbols; }
    }
}
