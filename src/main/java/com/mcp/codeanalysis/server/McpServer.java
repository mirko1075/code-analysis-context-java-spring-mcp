package com.mcp.codeanalysis.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Main MCP Server class implementing the Model Context Protocol for Java/Spring code analysis.
 * Communicates via stdio using JSON-RPC protocol.
 */
public class McpServer {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    private static final String SERVER_NAME = "code-analysis-java-spring-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    private final JsonRpcHandler rpcHandler;
    private final ObjectMapper objectMapper;

    public McpServer() {
        this.objectMapper = new ObjectMapper();
        this.rpcHandler = new JsonRpcHandler();
    }

    /**
     * Main entry point for the MCP server.
     */
    public static void main(String[] args) {
        logger.info("Starting {} version {}", SERVER_NAME, SERVER_VERSION);

        McpServer server = new McpServer();

        try {
            server.run();
        } catch (Exception e) {
            logger.error("Fatal error in MCP server", e);
            System.exit(1);
        }
    }

    /**
     * Run the MCP server main loop.
     * Reads JSON-RPC messages from stdin, processes them, and writes responses to stdout.
     */
    public void run() throws Exception {
        logger.info("MCP server started, listening on stdio");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                Map<String, Object> request = null;
                try {
                    logger.debug("Received request: {}", line);

                    // Parse JSON-RPC request
                    request = objectMapper.readValue(line, java.util.Map.class);

                    // Handle request and get response
                    var response = rpcHandler.handleRequest(request);

                    // Write response to stdout (only if not null - notifications don't have responses)
                    if (response != null) {
                        String responseJson = objectMapper.writeValueAsString(response);
                        logger.debug("Sending response: {}", responseJson);

                        writer.write(responseJson);
                        writer.newLine();
                        writer.flush();
                    } else {
                        logger.debug("No response sent (notification handled)");
                    }

                } catch (Exception e) {
                    logger.error("Error processing request: {}", line, e);

                    // Send error response
                    var errorResponse = createErrorResponse(
                        request != null && request.containsKey("id") ? request.get("id") : null,
                        -32603,
                        "Internal error: " + e.getMessage()
                    );

                    String errorJson = objectMapper.writeValueAsString(errorResponse);
                    writer.write(errorJson);
                    writer.newLine();
                    writer.flush();
                }
            }

        } catch (Exception e) {
            logger.error("Fatal error in server loop", e);
            throw e;
        }

        logger.info("MCP server shutting down");
    }

    /**
     * Create a JSON-RPC error response.
     */
    private java.util.Map<String, Object> createErrorResponse(Object id, int code, String message) {
        var error = new java.util.HashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);

        var response = new java.util.HashMap<String, Object>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", error);

        return response;
    }
}
