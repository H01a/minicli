package com.minicli.tools.builtin;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 内置工具路径工具：~ 展开、相对当前目录解析、敏感写路径判定。 */
public final class PathUtil {

    private PathUtil() {
    }

    /** 展开 ~ / ~user 并相对当前目录解析为绝对规范化路径。 */
    public static Path resolve(String path) {
        String p = path == null ? "" : path.trim();
        if (p.isEmpty()) {
            p = ".";
        }
        if (p.equals("~")) {
            p = System.getProperty("user.home");
        } else if (p.startsWith("~/")) {
            p = System.getProperty("user.home") + p.substring(1);
        }
        return Paths.get(p).toAbsolutePath().normalize();
    }

    /** 写类工具安全判定：拒绝 .env* 与任意 .git 路径元素。 */
    public static boolean isSensitiveWritePath(Path path) {
        for (Path element : path) {
            String name = element.getFileName() == null ? "" : element.getFileName().toString();
            if (name.startsWith(".env") || name.equals(".git")) {
                return true;
            }
        }
        return false;
    }

    /** 截断到 max 字符，超长加省略号。 */
    public static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
