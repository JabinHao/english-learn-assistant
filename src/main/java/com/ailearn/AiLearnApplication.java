package com.ailearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearnApplication.class, args);
    }
}
