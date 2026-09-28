package com.rental.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serves uploaded driving-license files at /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + java.nio.file.Paths.get(uploadDir).toAbsolutePath() + "/");

        registry.addResourceHandler("/Forend/**")
                .addResourceLocations("classpath:/Forend/");
    }
}
