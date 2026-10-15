package com.salonbooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SalonBooksApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalonBooksApplication.class, args);
    }
}
