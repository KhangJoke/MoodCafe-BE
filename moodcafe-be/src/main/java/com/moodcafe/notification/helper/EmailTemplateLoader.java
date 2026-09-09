package com.moodcafe.notification.helper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class EmailTemplateLoader {

    public String loadTemplate(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + name);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
