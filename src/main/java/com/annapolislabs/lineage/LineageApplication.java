package com.annapolislabs.lineage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LineageApplication {
    public static void main(String[] args) {
        SpringApplication.run(LineageApplication.class, args);
    }
}
