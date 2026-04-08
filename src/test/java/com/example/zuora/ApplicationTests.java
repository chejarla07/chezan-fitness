package com.example.zuora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "zuora.client-id=test",
    "zuora.client-secret=test"
})
class ApplicationTests {

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
    }
}
