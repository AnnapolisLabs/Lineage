package com.annapolislabs.lineage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter that processes JWT tokens from incoming requests
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);
            logger.debug("JWT token from request: {}", token != null ? "present" : "null");

            if (token != null && jwtTokenProvider.validateToken(token)) {
                logger.debug("JWT token validation successful");

                // Check token type
                JwtTokenProvider.TokenType tokenType = jwtTokenProvider.getTokenType(token);
                if (tokenType == JwtTokenProvider.TokenType.ACCESS) {
                    authenticateUser(request, token);
                } else {
                    logger.warn("Invalid token type provided: {}", tokenType);
                }
            } else {
                logger.debug("JWT token validation failed or token is null");
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            securityAuditService.logAuthenticationFailure("JWT_PROCESSING_ERROR", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    private void authenticateUser(HttpServletRequest request, String token) {
        try {
            String email = jwtTokenProvider.getEmailFromToken(token);
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            
            logger.debug("Authenticating user: {} with ID: {}", email, userId);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            // Validate token against user details
            if (jwtTokenProvider.validateToken(token) && 
                userDetails.getUsername().equals(email)) {
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.debug("Successfully authenticated user: {}", email);
                
                // Log successful authentication
                securityAuditService.logSuccessfulAuthentication(userId, email);
                
            } else {
                logger.warn("Token validation failed for user: {}", email);
                securityAuditService.logAuthenticationFailure("TOKEN_VALIDATION_FAILED", email);
            }
            
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            securityAuditService.logAuthenticationFailure("AUTHENTICATION_ERROR", e.getMessage());
            
            // Clear security context on error
            SecurityContextHolder.clearContext();
        }
    }
    
    /**
     * Check if request should be skipped from JWT authentication
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip authentication for public endpoints (login, register, etc.)
        // But NOT for /api/auth/me which requires authentication
        return (path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) ||
               path.startsWith("/api/invitations/") ||
               path.startsWith("/swagger") ||
               path.startsWith("/api-docs") ||
               path.equals("/error") ||
               path.startsWith("/h2-console/") ||
               path.equals("/actuator/health") ||
               path.equals("/actuator/info");
    }
}