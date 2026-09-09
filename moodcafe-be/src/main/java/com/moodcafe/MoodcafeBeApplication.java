package com.moodcafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MoodcafeBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoodcafeBeApplication.class, args);
    }

}
