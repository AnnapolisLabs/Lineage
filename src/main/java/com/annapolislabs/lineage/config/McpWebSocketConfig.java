package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.mcp.McpServer;
import com.annapolislabs.lineage.security.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket configuration for MCP server
 */
@Configuration
@EnableWebSocket
public class McpWebSocketConfig implements WebSocketConfigurer {

    private final McpServer mcpServer;
    private final JwtUtil jwtUtil;

    public McpWebSocketConfig(McpServer mcpServer, JwtUtil jwtUtil) {
        this.mcpServer = mcpServer;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mcpServer, "/mcp")
                .setAllowedOrigins("*")
                .addInterceptors(new McpAuthInterceptor(jwtUtil));
    }

    /**
     * Interceptor to authenticate WebSocket connections using JWT
     */
    private static class McpAuthInterceptor implements HandshakeInterceptor {
        private final JwtUtil jwtUtil;

        public McpAuthInterceptor(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            try {
                // Extract JWT from query parameter or header
                String token = extractToken(request);
                
                if (token != null) {
                    // Validate token by checking expiration
                    String username = jwtUtil.extractUsername(token);
                    if (username != null && !jwtUtil.extractExpiration(token).before(new java.util.Date())) {
                        attributes.put("userId", username);
                        return true;
                    }
                }
            } catch (Exception e) {
                // Log authentication failure
                System.err.println("MCP WebSocket authentication failed: " + e.getMessage());
            }
            
            return false; // Reject connection if authentication fails
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // Nothing to do after handshake
        }

        private String extractToken(ServerHttpRequest request) {
            // Try query parameter first
            String query = request.getURI().getQuery();
            if (query != null && query.contains("token=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        return param.substring(6);
                    }
                }
            }
            
            // Try Authorization header
            if (request.getHeaders().containsKey("Authorization")) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
            
            return null;
        }
    }
}
