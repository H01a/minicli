package com.minicli.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一配置：唯一读取 .env / 环境变量的地方（AGENTS.md 第 5 条）。
 *
 * <p>真正生效的配置文件是项目根目录 .env；.env.example 仅是 @root 个人使用的模板，
 * 不作为运行配置依据。完整键位与说明见 .env.example 与 docs/02-design.md §4.8。
 *
 * <p>取值优先级：.env > 同名系统环境变量 > 代码内默认值（必填项缺失则报错）。
 */
public final class Config {

    public static final String ENV_FILE = ".env";

    /** DeepSeek / LLM 配置 */
    public static final String KEY_API_KEY = "DEEPSEEK_API_KEY";
    public static final String KEY_MODEL = "DEEPSEEK_MODEL";
    public static final String KEY_BASE_URL = "DEEPSEEK_BASE_URL";
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";

    /** 网络与超时 */
    public static final String KEY_CONNECT_TIMEOUT_SECONDS = "DEEPSEEK_CONNECT_TIMEOUT_SECONDS";
    public static final String KEY_READ_TIMEOUT_SECONDS = "DEEPSEEK_READ_TIMEOUT_SECONDS";
    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    public static final int DEFAULT_READ_TIMEOUT_SECONDS = 120;

    /** Agent 主循环 */
    public static final String KEY_MAX_STEPS = "MINICLI_AGENT_MAX_STEPS";
    public static final String KEY_MAX_CONCURRENCY = "MINICLI_AGENT_MAX_CONCURRENCY";
    public static final String KEY_MAX_OBSERVATION_CHARS = "MINICLI_AGENT_MAX_OBSERVATION_CHARS";
    public static final int DEFAULT_MAX_STEPS = 50;
    public static final int DEFAULT_MAX_CONCURRENCY = 4;
    public static final int DEFAULT_MAX_OBSERVATION_CHARS = 4000;

    /** 智谱 GLM 联网搜索（可选；未配置时不注册 glm_web_search 工具） */
    public static final String KEY_GLM_API_KEY = "GLM_API_KEY";

    /** MCP stdio server 清单文件（相对项目根目录；文件不存在则跳过 MCP 注册） */
    public static final String KEY_MCP_SERVERS_FILE = "MINICLI_MCP_SERVERS_FILE";
    public static final String DEFAULT_MCP_SERVERS_FILE = "config/mcp-servers.json";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int maxSteps;
    private final int maxConcurrency;
    private final int maxObservationChars;
    private final String glmApiKey;
    private final String mcpServersFile;

    private Config(String apiKey, String model, String baseUrl,
                   int connectTimeoutSeconds, int readTimeoutSeconds,
                   int maxSteps, int maxConcurrency, int maxObservationChars,
                   String glmApiKey, String mcpServersFile) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.maxSteps = maxSteps;
        this.maxConcurrency = maxConcurrency;
        this.maxObservationChars = maxObservationChars;
        this.glmApiKey = glmApiKey;
        this.mcpServersFile = mcpServersFile;
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
        return new Config(
                apiKey.trim(),
                model.trim(),
                firstNonBlank(values.get(KEY_BASE_URL), System.getenv(KEY_BASE_URL), DEFAULT_BASE_URL).trim(),
                positiveInt(values, KEY_CONNECT_TIMEOUT_SECONDS, DEFAULT_CONNECT_TIMEOUT_SECONDS, "连接超时（秒）"),
                positiveInt(values, KEY_READ_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS, "读取超时（秒）"),
                positiveInt(values, KEY_MAX_STEPS, DEFAULT_MAX_STEPS, "ReAct 最大步数"),
                positiveInt(values, KEY_MAX_CONCURRENCY, DEFAULT_MAX_CONCURRENCY, "工具最大并发数"),
                positiveInt(values, KEY_MAX_OBSERVATION_CHARS, DEFAULT_MAX_OBSERVATION_CHARS, "观察结果截断字符数"),
                glmApiKey(values),
                mcpServersFile(values));
    }

    /** 测试/扩展用：显式指定三项配置（baseUrl 便于指向 MockWebServer），其余使用默认值。 */
    public static Config of(String apiKey, String model, String baseUrl) {
        return new Config(apiKey, model, baseUrl,
                DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS,
                DEFAULT_MAX_STEPS, DEFAULT_MAX_CONCURRENCY, DEFAULT_MAX_OBSERVATION_CHARS,
                "", DEFAULT_MCP_SERVERS_FILE);
    }

    /** 测试/扩展用：完整指定全部配置。 */
    public static Config of(String apiKey, String model, String baseUrl,
                            int connectTimeoutSeconds, int readTimeoutSeconds,
                            int maxSteps, int maxConcurrency, int maxObservationChars) {
        return new Config(apiKey, model, baseUrl,
                connectTimeoutSeconds, readTimeoutSeconds,
                maxSteps, maxConcurrency, maxObservationChars,
                "", DEFAULT_MCP_SERVERS_FILE);
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

    /** 读取智谱 API Key：可选，未配置返回空串。 */
    private static String glmApiKey(Map<String, String> values) {
        String raw = firstNonBlank(values.get(KEY_GLM_API_KEY), System.getenv(KEY_GLM_API_KEY));
        return raw == null ? "" : raw.trim();
    }

    /** 读取 MCP server 清单文件路径：缺失时使用默认路径。 */
    private static String mcpServersFile(Map<String, String> values) {
        return firstNonBlank(values.get(KEY_MCP_SERVERS_FILE),
                System.getenv(KEY_MCP_SERVERS_FILE), DEFAULT_MCP_SERVERS_FILE).trim();
    }

    /** 读取正整数配置：.env / 环境变量缺失或为空时用默认值；非法值（非数字、非正数）报错。 */
    private static int positiveInt(Map<String, String> values, String key, int defaultValue, String label) {
        String raw = firstNonBlank(values.get(key), System.getenv(key));
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new NumberFormatException("必须为正整数");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ConfigException("配置 " + key + " 无效: " + raw.trim() + "（" + label + "）");
        }
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static String firstNonBlank(String a, String b, String c) {
        return firstNonBlank(firstNonBlank(a, b), c);
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

    public int connectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int readTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public int maxSteps() {
        return maxSteps;
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    public int maxObservationChars() {
        return maxObservationChars;
    }

    public String glmApiKey() {
        return glmApiKey;
    }

    public String mcpServersFile() {
        return mcpServersFile;
    }
}
