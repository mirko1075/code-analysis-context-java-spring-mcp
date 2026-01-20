package com.mcp.codeanalysis.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolRegistry.
 */
class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
    }

    @Test
    void testGetToolsMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();

        assertNotNull(tools);
        assertEquals(6, tools.size());

        // Verify all tool names are present
        List<String> toolNames = tools.stream()
                .map(tool -> (String) tool.get("name"))
                .toList();

        assertTrue(toolNames.contains("arch"));
        assertTrue(toolNames.contains("deps"));
        assertTrue(toolNames.contains("patterns"));
        assertTrue(toolNames.contains("coverage"));
        assertTrue(toolNames.contains("conventions"));
        assertTrue(toolNames.contains("context"));
    }

    @Test
    void testArchToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> archTool = tools.stream()
                .filter(tool -> "arch".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(archTool);
        assertEquals("arch", archTool.get("name"));
        assertEquals("Analyze Java/Spring project architecture and structure", archTool.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) archTool.get("inputSchema");
        assertNotNull(inputSchema);
        assertEquals("object", inputSchema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("path"));
        assertTrue(properties.containsKey("depth"));
        assertTrue(properties.containsKey("diagrams"));
        assertTrue(properties.containsKey("metrics"));
        assertTrue(properties.containsKey("includeXml"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("path"));
    }

    @Test
    void testDepsToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> depsTool = tools.stream()
                .filter(tool -> "deps".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(depsTool);
        assertEquals("deps", depsTool.get("name"));
        assertEquals("Analyze package dependencies and imports", depsTool.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) depsTool.get("inputSchema");
        assertNotNull(inputSchema);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.containsKey("circular"));
        assertTrue(properties.containsKey("diagram"));
    }

    @Test
    void testPatternsToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> patternsTool = tools.stream()
                .filter(tool -> "patterns".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(patternsTool);
        assertEquals("patterns", patternsTool.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) patternsTool.get("inputSchema");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.containsKey("configType"));

        @SuppressWarnings("unchecked")
        Map<String, Object> configType = (Map<String, Object>) properties.get("configType");

        @SuppressWarnings("unchecked")
        List<String> configTypeEnum = (List<String>) configType.get("enum");
        assertTrue(configTypeEnum.contains("xml"));
        assertTrue(configTypeEnum.contains("java"));
        assertTrue(configTypeEnum.contains("both"));
        assertTrue(configTypeEnum.contains("all"));
    }

    @Test
    void testCoverageToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> coverageTool = tools.stream()
                .filter(tool -> "coverage".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(coverageTool);
        assertEquals("coverage", coverageTool.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) coverageTool.get("inputSchema");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.containsKey("report"));
        assertTrue(properties.containsKey("fw"));
    }

    @Test
    void testConventionsToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> conventionsTool = tools.stream()
                .filter(tool -> "conventions".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(conventionsTool);
        assertEquals("conventions", conventionsTool.get("name"));
    }

    @Test
    void testContextToolMetadata() {
        List<Map<String, Object>> tools = registry.getToolsMetadata();
        Map<String, Object> contextTool = tools.stream()
                .filter(tool -> "context".equals(tool.get("name")))
                .findFirst()
                .orElse(null);

        assertNotNull(contextTool);
        assertEquals("context", contextTool.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) contextTool.get("inputSchema");

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("task"));
        assertTrue(required.contains("path"));
    }

    @Test
    void testCallArchTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");
        arguments.put("depth", "d");

        String result = registry.callTool("arch", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"arch\""));
        assertTrue(result.contains("\"status\": \"not_implemented\""));
    }

    @Test
    void testCallDepsTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");
        arguments.put("circular", true);

        String result = registry.callTool("deps", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"deps\""));
        assertTrue(result.contains("\"status\": \"not_implemented\""));
    }

    @Test
    void testCallPatternsTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");

        String result = registry.callTool("patterns", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"patterns\""));
    }

    @Test
    void testCallCoverageTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");

        String result = registry.callTool("coverage", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"coverage\""));
    }

    @Test
    void testCallConventionsTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");

        String result = registry.callTool("conventions", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"conventions\""));
    }

    @Test
    void testCallContextTool() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("task", "Add authentication");
        arguments.put("path", "/test/path");

        String result = registry.callTool("context", arguments);

        assertNotNull(result);
        assertTrue(result.contains("\"tool\": \"context\""));
    }

    @Test
    void testCallUnknownTool() {
        Map<String, Object> arguments = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> {
            registry.callTool("unknown-tool", arguments);
        });
    }
}
