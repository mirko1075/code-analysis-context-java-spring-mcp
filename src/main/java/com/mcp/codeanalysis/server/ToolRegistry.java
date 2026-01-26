package com.mcp.codeanalysis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry for MCP analysis tools.
 * Manages tool metadata and routing of tool calls.
 */
public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

    /**
     * Get metadata for all registered tools.
     */
    public List<Map<String, Object>> getToolsMetadata() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool 1: Architecture Analyzer
        tools.add(createToolMetadata(
            "arch",
            "Analyze Java/Spring project architecture and structure",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "depth", Map.of(
                        "type", "string",
                        "description", "Analysis depth: 'o' (overview), 'd' (detailed), 'x' (deep)",
                        "enum", List.of("o", "d", "x")
                    ),
                    "types", Map.of(
                        "type", "array",
                        "description", "Analysis types to include",
                        "items", Map.of("type", "string")
                    ),
                    "includeXml", Map.of(
                        "type", "boolean",
                        "description", "Parse XML configuration files (Spring XML beans)"
                    ),
                    "diagrams", Map.of(
                        "type", "boolean",
                        "description", "Generate Mermaid diagrams"
                    ),
                    "metrics", Map.of(
                        "type", "boolean",
                        "description", "Include code metrics"
                    )
                ),
                "required", List.of("path")
            )
        ));

        // Tool 2: Dependency Mapper
        tools.add(createToolMetadata(
            "deps",
            "Analyze package dependencies and imports",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "circular", Map.of(
                        "type", "boolean",
                        "description", "Detect circular dependencies"
                    ),
                    "diagram", Map.of(
                        "type", "boolean",
                        "description", "Generate Mermaid dependency diagram"
                    ),
                    "focus", Map.of(
                        "type", "string",
                        "description", "Focus on specific package"
                    )
                ),
                "required", List.of("path")
            )
        ));

        // Tool 3: Pattern Detector
        tools.add(createToolMetadata(
            "patterns",
            "Detect Spring Framework and enterprise Java patterns",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "types", Map.of(
                        "type", "array",
                        "description", "Pattern types to detect",
                        "items", Map.of("type", "string")
                    ),
                    "configType", Map.of(
                        "type", "string",
                        "description", "Configuration types: xml, java, both, all",
                        "enum", List.of("xml", "java", "both", "all")
                    )
                ),
                "required", List.of("path")
            )
        ));

        // Tool 4: Coverage Analyzer
        tools.add(createToolMetadata(
            "coverage",
            "Analyze test coverage and generate test suggestions",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "report", Map.of(
                        "type", "string",
                        "description", "Coverage report path (jacoco.xml, jacoco.csv)"
                    ),
                    "fw", Map.of(
                        "type", "string",
                        "description", "Test framework: junit5, junit4, testng",
                        "enum", List.of("junit5", "junit4", "testng")
                    )
                ),
                "required", List.of("path")
            )
        ));

        // Tool 5: Convention Validator
        tools.add(createToolMetadata(
            "conventions",
            "Validate Java conventions and Spring best practices",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "auto", Map.of(
                        "type", "boolean",
                        "description", "Auto-detect project conventions"
                    ),
                    "severity", Map.of(
                        "type", "string",
                        "description", "Minimum severity: err, warn, info",
                        "enum", List.of("err", "warn", "info")
                    )
                ),
                "required", List.of("path")
            )
        ));

        // Tool 6: Context Pack Generator
        tools.add(createToolMetadata(
            "context",
            "Generate AI-optimized context packs",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "task", Map.of(
                        "type", "string",
                        "description", "Task description"
                    ),
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "tokens", Map.of(
                        "type", "integer",
                        "description", "Token budget (default: 50000)"
                    ),
                    "format", Map.of(
                        "type", "string",
                        "description", "Output format: md, json, xml",
                        "enum", List.of("md", "json", "xml")
                    )
                ),
                "required", List.of("task", "path")
            )
        ));

        // Tool 7: LSP Analyzer
        tools.add(createToolMetadata(
            "lsp",
            "Language Server Protocol features: diagnostics, hover, definitions, references, completions, symbols",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "Project root directory"
                    ),
                    "file", Map.of(
                        "type", "string",
                        "description", "Target Java file path (relative or absolute)"
                    ),
                    "operation", Map.of(
                        "type", "string",
                        "description", "LSP operation to perform",
                        "enum", List.of("diagnostics", "hover", "definition", "references", "completions", "symbols", "all-diagnostics")
                    ),
                    "line", Map.of(
                        "type", "integer",
                        "description", "Line number (required for hover, definition, references, completions)"
                    ),
                    "column", Map.of(
                        "type", "integer",
                        "description", "Column number (required for hover, definition, references, completions)"
                    )
                ),
                "required", List.of("path", "file")
            )
        ));

        return tools;
    }

    /**
     * Call a specific tool with given arguments.
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        logger.info("Calling tool: {} with arguments: {}", toolName, arguments);

        // Route to specific tool implementation
        return switch (toolName) {
            case "arch" -> callArchitectureAnalyzer(arguments);
            case "deps" -> callDependencyMapper(arguments);
            case "patterns" -> callPatternDetector(arguments);
            case "coverage" -> callCoverageAnalyzer(arguments);
            case "conventions" -> callConventionValidator(arguments);
            case "context" -> callContextPackGenerator(arguments);
            case "lsp" -> callLspAnalyzer(arguments);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private String callArchitectureAnalyzer(Map<String, Object> arguments) {
        // TODO: Implement full architecture analyzer
        return "{\n" +
               "  \"tool\": \"arch\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Architecture Analyzer tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callDependencyMapper(Map<String, Object> arguments) {
        // TODO: Implement full dependency mapper
        return "{\n" +
               "  \"tool\": \"deps\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Dependency Mapper tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callPatternDetector(Map<String, Object> arguments) {
        // TODO: Implement full pattern detector
        return "{\n" +
               "  \"tool\": \"patterns\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Pattern Detector tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callCoverageAnalyzer(Map<String, Object> arguments) {
        // TODO: Implement full coverage analyzer
        return "{\n" +
               "  \"tool\": \"coverage\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Coverage Analyzer tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callConventionValidator(Map<String, Object> arguments) {
        // TODO: Implement full convention validator
        return "{\n" +
               "  \"tool\": \"conventions\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Convention Validator tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callContextPackGenerator(Map<String, Object> arguments) {
        // TODO: Implement full context pack generator
        return "{\n" +
               "  \"tool\": \"context\",\n" +
               "  \"status\": \"not_implemented\",\n" +
               "  \"message\": \"Context Pack Generator tool is not yet implemented\",\n" +
               "  \"arguments\": " + arguments + "\n" +
               "}";
    }

    private String callLspAnalyzer(Map<String, Object> arguments) {
        try {
            // Import LspAnalyzer class
            com.mcp.codeanalysis.tools.LspAnalyzer analyzer = new com.mcp.codeanalysis.tools.LspAnalyzer();

            // Parse arguments
            String path = (String) arguments.get("path");
            String file = (String) arguments.get("file");
            String operation = (String) arguments.getOrDefault("operation", "diagnostics");
            Integer line = arguments.containsKey("line") ? ((Number) arguments.get("line")).intValue() : null;
            Integer column = arguments.containsKey("column") ? ((Number) arguments.get("column")).intValue() : null;

            // Create options
            com.mcp.codeanalysis.tools.LspAnalyzer.LspOptions options =
                new com.mcp.codeanalysis.tools.LspAnalyzer.LspOptions();
            options.file = file;
            options.operation = operation;
            options.line = line;
            options.column = column;

            // Perform analysis
            com.mcp.codeanalysis.tools.LspAnalyzer.LspAnalysisResult result = analyzer.analyze(path, options);

            // Format result as markdown
            return analyzer.formatResult(result);

        } catch (Exception e) {
            logger.error("Error calling LSP Analyzer", e);
            return "{\n" +
                   "  \"tool\": \"lsp\",\n" +
                   "  \"status\": \"error\",\n" +
                   "  \"message\": \"" + e.getMessage() + "\",\n" +
                   "  \"arguments\": " + arguments + "\n" +
                   "}";
        }
    }

    /**
     * Helper to create tool metadata structure.
     */
    private Map<String, Object> createToolMetadata(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        return tool;
    }
}
