package com.minicli.config;

/** 配置加载失败（如 .env 缺失 DEEPSEEK_API_KEY）时抛出。 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
