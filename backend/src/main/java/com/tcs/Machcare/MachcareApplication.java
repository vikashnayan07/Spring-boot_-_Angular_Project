package com.tcs.Machcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = "com.tcs.Machcare")
@EnableScheduling
public class MachcareApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MachcareApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MachcareApplication.class);
    }
}