package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Security Configuration with JWT authentication, CSRF protection,
 * CORS configuration, RBAC, session management, rate limiting, and security headers
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String PROJECT_MANAGER = "PROJECT_MANAGER";
    private static final String USERS_API_PATH = "/api/users/**";
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;
    private final CsrfTokenService csrfTokenService;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         UserDetailsService userDetailsService,
                         CsrfTokenService csrfTokenService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.userDetailsService = userDetailsService;
        this.csrfTokenService = csrfTokenService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF Protection - Disable built-in CSRF since we use custom validation
                .csrf(csrf -> csrf.disable())
                
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Session Management (Stateless for JWT)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .maximumSessions(5)
                    .maxSessionsPreventsLogin(false)
                )
                
                // Authorization Rules
                .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/invitations/**").permitAll()
                    .requestMatchers("/api/csrf/**").permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/h2-console/**").hasRole(ADMIN)
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    
                    // Static resources
                    .requestMatchers("/", "/index.html", "/assets/**", "/vite.svg", "/favicon.ico").permitAll()
                    
                    // Admin endpoints
                    .requestMatchers("/api/admin/**").hasRole(ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/admin/users").hasRole(ADMIN)
                    .requestMatchers(HttpMethod.POST, "/api/admin/users").hasAnyRole(ADMIN, PROJECT_MANAGER)
                    .requestMatchers(HttpMethod.PUT, "/api/admin/users/**").hasRole(ADMIN)
                    .requestMatchers(HttpMethod.DELETE, "/api/admin/users/**").hasRole(ADMIN)
                    
                    // User management endpoints
                    .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                    .requestMatchers(HttpMethod.GET, USERS_API_PATH).hasAnyRole(ADMIN, PROJECT_MANAGER)
                    .requestMatchers(HttpMethod.POST, USERS_API_PATH).hasAnyRole(ADMIN, PROJECT_MANAGER)
                    .requestMatchers(HttpMethod.PUT, USERS_API_PATH).hasAnyRole(ADMIN, PROJECT_MANAGER)
                    .requestMatchers(HttpMethod.DELETE, USERS_API_PATH).hasRole(ADMIN)
                    
                    // Security endpoints
                    .requestMatchers("/api/security/**").authenticated()
                    
                    // Project management (requires authentication)
                    .requestMatchers("/api/projects/test-import").permitAll()
                    .requestMatchers("/api/projects/import").permitAll()
                    .requestMatchers("/api/projects/**").authenticated()
                    
                    // Everything else requires authentication
                    .anyRequest().authenticated()
                )
                
                // Security Headers
                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.deny())
                    .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                        .preload(true)
                    )
                    .referrerPolicy(referrerPolicy -> referrerPolicy
                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                )
                
                // Exception Handling
                .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        String errorResponse = """
                            {
                                "error": {
                                    "code": "ACCESS_DENIED",
                                    "message": "Insufficient permissions to access this resource",
                                    "path": "%s",
                                    "timestamp": "%s"
                                }
                            }
                            """.formatted(request.getRequestURI(), java.time.Instant.now().toString());
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write(errorResponse);
                    })
                )
                
                // Authentication Provider
                .authenticationProvider(authenticationProvider())
                
                // JWT Filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Add custom CSRF validation filter after JWT authentication
                .addFilterAfter(csrfValidationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOriginsList = Arrays.asList(allowedOrigins);
        List<String> allowedMethodsList = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        List<String> allowedHeadersList = Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-CSRF-Token",
            "X-XSRF-TOKEN",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        );
        List<String> exposedHeadersList = Arrays.asList("Authorization");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins
        configuration.setAllowedOriginPatterns(allowedOriginsList);
        
        // Set allowed methods
        configuration.setAllowedMethods(allowedMethodsList);
        
        // Set allowed headers
        configuration.setAllowedHeaders(allowedHeadersList);
        
        // Set exposed headers
        configuration.setExposedHeaders(exposedHeadersList);
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Set max age
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CsrfTokenRepository cookieCsrfTokenRepository() {
        org.springframework.security.web.csrf.CookieCsrfTokenRepository repository = 
            org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // Configure the repository for JWT-based API
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setParameterName("_csrf");
        
        return repository;
    }
    
    @Bean
    public CsrfValidationFilter csrfValidationFilter() {
        return new CsrfValidationFilter(csrfTokenService);
    }
}
