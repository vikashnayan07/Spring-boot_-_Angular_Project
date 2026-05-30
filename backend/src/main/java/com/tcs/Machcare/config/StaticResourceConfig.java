package com.tcs.Machcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*.js", "/*.css", "/favicon.ico", "/assets/**", "/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).mustRevalidate());
    }
}