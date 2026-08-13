package com.minicli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void greetingContainsJavaVersion() {
        String greeting = Main.greeting();
        assertTrue(greeting.startsWith("Hello from minicli"));
        assertTrue(greeting.contains(System.getProperty("java.version")));
    }

    @Test
    void runsOnJava21OrLater() {
        int major = Runtime.version().feature();
        assertTrue(major >= 21, "minicli requires Java 21+, got Java " + major);
    }
}
