package com.annapolislabs.lineage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web configuration for serving the Vue.js frontend.
 * Handles SPA routing by forwarding all non-API requests to index.html.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources (JS, CSS, images) with caching
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(31536000); // 1 year cache

        registry.addResourceHandler("/vite.svg", "/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // 1 day cache

        // Handle Vue Router (SPA) - forward all other requests to index.html
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the resource exists, serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Otherwise, serve index.html for client-side routing (SPA)
                        // But only if it's not an API request
                        if (!resourcePath.startsWith("api/")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}