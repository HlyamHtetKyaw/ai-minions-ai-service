package com.aiminion.aiservice.common.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${audio.storage.path:/tmp/aiminion/audio}")
    private String audioStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path audioDir = Paths.get(audioStoragePath).toAbsolutePath().normalize();
        String audioLocation = audioDir.toUri().toString();
        if (!audioLocation.endsWith("/")) audioLocation = audioLocation + "/";

        registry.addResourceHandler("/audio/**")
                .addResourceLocations(audioLocation);
    }
}