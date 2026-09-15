package com.moodcafe;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires a running PostgreSQL instance and configured environment variables")
class MoodcafeBeApplicationTests {

    @Test
    void contextLoads() {
    }

}
