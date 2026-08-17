package com.minicli.tools.builtin;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUtilTest {

    @Test
    void resolveExpandsHomeAndNormalizes() {
        assertEquals(System.getProperty("user.home"), PathUtil.resolve("~").toString());
        assertTrue(PathUtil.resolve("~/x").startsWith(System.getProperty("user.home")));
        assertTrue(PathUtil.resolve(".").isAbsolute());
        assertEquals(Path.of("").toAbsolutePath().normalize(), PathUtil.resolve(""));
    }

    @Test
    void detectsSensitiveWritePaths() {
        assertTrue(PathUtil.isSensitiveWritePath(Path.of("/tmp/foo/.env")));
        assertTrue(PathUtil.isSensitiveWritePath(Path.of("/tmp/foo/.env.example")));
        assertTrue(PathUtil.isSensitiveWritePath(Path.of("/tmp/foo/.git/config")));
        assertFalse(PathUtil.isSensitiveWritePath(Path.of("/tmp/foo/notes.txt")));
    }

    @Test
    void abbreviatesLongText() {
        assertEquals("abc", PathUtil.abbreviate("abc", 5));
        assertEquals("abcd…", PathUtil.abbreviate("abcdef", 4));
    }
}
