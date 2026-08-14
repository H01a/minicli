package com.minicli.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一配置：唯一读取 .env / 环境变量的地方（AGENTS.md 第 5 条）。
 * 键名以根目录 .env 为准：DEEPSEEK_API_KEY / DEEPSEEK_MODEL。
 */
public final class Config {

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String ENV_FILE = ".env";
    public static final String KEY_API_KEY = "DEEPSEEK_API_KEY";
    public static final String KEY_MODEL = "DEEPSEEK_MODEL";

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    private Config(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    /** 从项目根目录 .env 加载；也支持同名系统环境变量兜底。 */
    public static Config load() {
        return load(Path.of(ENV_FILE));
    }

    static Config load(Path envFile) {
        Map<String, String> values = new HashMap<>();
        if (Files.exists(envFile)) {
            values.putAll(parseEnvFile(envFile));
        }
        String apiKey = firstNonBlank(values.get(KEY_API_KEY), System.getenv(KEY_API_KEY));
        if (apiKey == null || apiKey.isBlank()) {
            throw new ConfigException("缺少配置 " + KEY_API_KEY
                    + "：请在项目根目录 .env 中填写（模板见 .env.example），或设置同名环境变量");
        }
        String model = firstNonBlank(values.get(KEY_MODEL), System.getenv(KEY_MODEL));
        if (model == null || model.isBlank()) {
            throw new ConfigException("缺少配置 " + KEY_MODEL + "：请在 .env 中填写模型名（如 deepseek-v4-flash）");
        }
        return new Config(apiKey.trim(), model.trim(), DEFAULT_BASE_URL);
    }

    /** 测试/扩展用：显式指定三项配置（baseUrl 便于指向 MockWebServer）。 */
    public static Config of(String apiKey, String model, String baseUrl) {
        return new Config(apiKey, model, baseUrl);
    }

    /** 解析 .env：忽略空行与 # 注释，支持 KEY=VALUE 与引号包裹的值。 */
    static Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> map = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        } catch (IOException e) {
            throw new ConfigException("读取 .env 失败: " + envFile, e);
        }
        return map;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public String baseUrl() {
        return baseUrl;
    }
}
