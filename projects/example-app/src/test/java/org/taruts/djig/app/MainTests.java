package org.taruts.djig.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MainTests {

    @Test
    void contextLoads() {
        System.out.println("Module name = " + getClass().getModule().getName());
    }
}
