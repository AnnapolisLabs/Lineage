package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.mcp.McpServer;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final JwtTokenProvider jwtTokenProvider;

    public McpWebSocketConfig(McpServer mcpServer, JwtTokenProvider jwtTokenProvider) {
        this.mcpServer = mcpServer;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mcpServer, "/mcp")
                .setAllowedOrigins("*")
                .addInterceptors(new McpAuthInterceptor(jwtTokenProvider));
    }

    /**
     * Interceptor to authenticate WebSocket connections using JWT
     */
    private static class McpAuthInterceptor implements HandshakeInterceptor {
        private static final Logger log = LoggerFactory.getLogger(McpAuthInterceptor.class);
        private final JwtTokenProvider jwtTokenProvider;

        public McpAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            try {
                // Extract JWT from query parameter or header
                String token = extractToken(request);
                
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getEmailFromToken(token);
                    if (username != null) {
                        attributes.put("userId", username);
                        return true;
                    }
                }
            } catch (Exception e) {
                // Log authentication failure
                log.error("MCP WebSocket authentication failed: {}", e.getMessage());
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
