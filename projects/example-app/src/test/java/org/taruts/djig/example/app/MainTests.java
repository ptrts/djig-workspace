package org.taruts.djig.example.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MainTests {

    // todo Тесты ругаются, т.к. @DynamicProjectQualifier("1") есть,
    //      а динамических проектов в свойствах тестов - нет.
    //      Надо бы как-то эту проблему решить
    //@Test
    void contextLoads() {
        System.out.println("Module name = " + getClass().getModule().getName());
    }
}
