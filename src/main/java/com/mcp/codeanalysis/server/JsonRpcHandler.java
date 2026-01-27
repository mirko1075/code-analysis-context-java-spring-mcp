package com.mcp.codeanalysis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles JSON-RPC protocol messages for the MCP server.
 * Supports initialize, tools/list, and tools/call methods.
 */
public class JsonRpcHandler {
    private static final Logger logger = LoggerFactory.getLogger(JsonRpcHandler.class);
    private static final String SERVER_NAME = "code-analysis-java-spring-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    private final ToolRegistry toolRegistry;
    private boolean initialized = false;

    public JsonRpcHandler() {
        this.toolRegistry = new ToolRegistry();
    }

    /**
     * Handle a JSON-RPC request and return a response.
     */
    public Map<String, Object> handleRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", new HashMap<>());

        logger.info("Handling request: method={}, id={}", method, id);

        try {
            Map<String, Object> result;
            switch (method) {
                case "initialize":
                    result = handleInitialize(params);
                    break;
                case "tools/list":
                    result = handleToolsList();
                    break;
                case "tools/call":
                    result = handleToolsCall(params);
                    break;
                case "ping":
                    result = handlePing();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }

            return createSuccessResponse(id, result);

        } catch (Exception e) {
            logger.error("Error handling method: {}", method, e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle MCP initialize request.
     */
    private Map<String, Object> handleInitialize(Map<String, Object> params) {
        logger.info("Initializing MCP server");

        String protocolVersion = (String) params.getOrDefault("protocolVersion", "2024-11-05");
        Map<String, Object> clientInfo = (Map<String, Object>) params.get("clientInfo");

        logger.info("Client info: {}", clientInfo);

        initialized = true;

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", protocolVersion);

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        result.put("serverInfo", serverInfo);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        result.put("capabilities", capabilities);

        logger.info("Server initialized successfully");
        return result;
    }

    /**
     * Handle tools/list request - returns list of available tools.
     */
    private Map<String, Object> handleToolsList() {
        if (!initialized) {
            throw new IllegalStateException("Server not initialized");
        }

        logger.info("Listing available tools");

        List<Map<String, Object>> tools = toolRegistry.getToolsMetadata();

        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);

        logger.info("Returning {} tools", tools.size());
        return result;
    }

    /**
     * Handle tools/call request - invokes a specific tool.
     */
    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        if (!initialized) {
            throw new IllegalStateException("Server not initialized");
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", new HashMap<>());

        logger.info("Calling tool: {} with arguments: {}", toolName, arguments);

        // Invoke tool through registry
        String toolResult = toolRegistry.callTool(toolName, arguments);

        // MCP tools/call response format
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", toolResult);
        content.add(textContent);

        result.put("content", content);
        result.put("isError", false);

        logger.info("Tool {} executed successfully", toolName);
        return result;
    }

    /**
     * Handle ping request for health check.
     */
    private Map<String, Object> handlePing() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Create a successful JSON-RPC response.
     */
    private Map<String, Object> createSuccessResponse(Object id, Map<String, Object> result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    /**
     * Create a JSON-RPC error response.
     */
    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", error);

        return response;
    }
}
