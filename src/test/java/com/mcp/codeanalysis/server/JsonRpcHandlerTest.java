package com.mcp.codeanalysis.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonRpcHandler.
 */
class JsonRpcHandlerTest {

    private JsonRpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonRpcHandler();
    }

    @Test
    void testHandleInitialize() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "initialize");

        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");

        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("name", "test-client");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        request.put("params", params);

        Map<String, Object> response = handler.handleRequest(request);

        // Verify response structure
        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(1, response.get("id"));
        assertNotNull(response.get("result"));

        // Verify result content
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertEquals("2024-11-05", result.get("protocolVersion"));

        @SuppressWarnings("unchecked")
        Map<String, Object> serverInfo = (Map<String, Object>) result.get("serverInfo");
        assertNotNull(serverInfo);
        assertEquals("code-analysis-java-spring-mcp", serverInfo.get("name"));
        assertEquals("1.0.0", serverInfo.get("version"));

        @SuppressWarnings("unchecked")
        Map<String, Object> capabilities = (Map<String, Object>) result.get("capabilities");
        assertNotNull(capabilities);
        assertNotNull(capabilities.get("tools"));
    }

    @Test
    void testHandleToolsListBeforeInitialize() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");

        Map<String, Object> response = handler.handleRequest(request);

        // Should return error response when not initialized
        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(2, response.get("id"));
        assertNotNull(response.get("error"));
        assertNull(response.get("result"));
    }

    @Test
    void testHandleToolsListAfterInitialize() {
        // First initialize
        Map<String, Object> initRequest = new HashMap<>();
        initRequest.put("jsonrpc", "2.0");
        initRequest.put("id", 1);
        initRequest.put("method", "initialize");
        initRequest.put("params", new HashMap<>());
        handler.handleRequest(initRequest);

        // Then list tools
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");

        Map<String, Object> response = handler.handleRequest(request);

        // Verify response
        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(2, response.get("id"));
        assertNotNull(response.get("result"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertNotNull(result.get("tools"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");

        // Should have 7 tools
        assertEquals(7, tools.size());

        // Verify tool names
        List<String> toolNames = tools.stream()
                .map(tool -> (String) tool.get("name"))
                .toList();

        assertTrue(toolNames.contains("arch"));
        assertTrue(toolNames.contains("deps"));
        assertTrue(toolNames.contains("patterns"));
        assertTrue(toolNames.contains("coverage"));
        assertTrue(toolNames.contains("conventions"));
        assertTrue(toolNames.contains("context"));
        assertTrue(toolNames.contains("lsp"));
    }

    @Test
    void testHandleToolsCall() {
        // First initialize
        Map<String, Object> initRequest = new HashMap<>();
        initRequest.put("jsonrpc", "2.0");
        initRequest.put("id", 1);
        initRequest.put("method", "initialize");
        initRequest.put("params", new HashMap<>());
        handler.handleRequest(initRequest);

        // Then call arch tool
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "tools/call");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "arch");

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", "/test/path");
        params.put("arguments", arguments);

        request.put("params", params);

        Map<String, Object> response = handler.handleRequest(request);

        // Verify response
        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(3, response.get("id"));
        assertNotNull(response.get("result"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertNotNull(result.get("content"));
        assertEquals(false, result.get("isError"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertEquals(1, content.size());
        assertEquals("text", content.get(0).get("type"));
        assertNotNull(content.get(0).get("text"));
    }

    @Test
    void testHandlePing() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 4);
        request.put("method", "ping");

        Map<String, Object> response = handler.handleRequest(request);

        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(4, response.get("id"));
        assertNotNull(response.get("result"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertEquals("ok", result.get("status"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void testHandleUnknownMethod() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 5);
        request.put("method", "unknown/method");

        Map<String, Object> response = handler.handleRequest(request);

        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(5, response.get("id"));
        assertNotNull(response.get("error"));
        assertNull(response.get("result"));

        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.get("error");
        assertEquals(-32603, error.get("code"));
        assertTrue(((String) error.get("message")).contains("Unknown method"));
    }

    @Test
    void testHandleNullId() {
        // Null id means notification - no response expected
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", null);
        request.put("method", "notifications/initialized");

        Map<String, Object> response = handler.handleRequest(request);

        // Notifications should return null (no response)
        assertNull(response);
    }

    @Test
    void testHandleNotificationInitialized() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        // No id field = notification
        request.put("method", "notifications/initialized");
        request.put("params", new HashMap<>());

        Map<String, Object> response = handler.handleRequest(request);

        // Notifications should return null (no response)
        assertNull(response);
    }

    @Test
    void testHandleUnknownNotification() {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        // No id field = notification
        request.put("method", "notifications/unknown");
        request.put("params", new HashMap<>());

        Map<String, Object> response = handler.handleRequest(request);

        // Unknown notifications should still return null (just logged and ignored)
        assertNull(response);
    }
}
