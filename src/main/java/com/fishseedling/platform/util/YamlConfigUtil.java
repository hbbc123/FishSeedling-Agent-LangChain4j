package com.fishseedling.platform.util;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

/**
 * YAML 配置读取工具类
 * 专门用于读取 applications.yml 中的 server.servlet.context-path 和 spring.url 并拼接
 */
public class YamlConfigUtil {

    private static final String CONFIG_FILE = "applications.yml";
    private static Map<String, Object> configCache = null;

    /**
     * 加载配置文件
     */
    private static Map<String, Object> loadConfig() {
        if (configCache != null) {
            return configCache;
        }

        try (InputStream in = YamlConfigUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new RuntimeException("配置文件不存在: " + CONFIG_FILE);
            }
            Yaml yaml = new Yaml();
            configCache = yaml.load(in);
            return configCache;
        } catch (Exception e) {
            throw new RuntimeException("加载 YAML 配置失败", e);
        }
    }

    /**
     * 获取 server.servlet.context-path
     */
    private static String getContextPath() {
        Map<String, Object> config = loadConfig();

        // 获取 server
        Object server = config.get("server");
        if (!(server instanceof Map)) {
            return "";
        }

        // 获取 servlet
        Object servlet = ((Map<?, ?>) server).get("servlet");
        if (!(servlet instanceof Map)) {
            return "";
        }

        // 获取 context-path
        Object contextPath = ((Map<?, ?>) servlet).get("context-path");
        if (contextPath == null) {
            return "";
        }

        String path = contextPath.toString();
        // 确保以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // 去除末尾的 /
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 获取 spring.url
     */
    private static String getSpringUrl() {
        Map<String, Object> config = loadConfig();

        // 获取 spring
        Object spring = config.get("spring");
        if (!(spring instanceof Map)) {
            return null;
        }

        // 获取 url
        Object url = ((Map<?, ?>) spring).get("url");
        return url != null ? url.toString() : null;
    }

    /**
     * 获取 server.port
     */
    private static Integer getServerPort() {
        Map<String, Object> config = loadConfig();

        // 获取 server
        Object server = config.get("server");
        if (!(server instanceof Map)) {
            return null;
        }

        // 获取 port
        Object port = ((Map<?, ?>) server).get("port");
        if (port == null) {
            return null;
        }

        if (port instanceof Integer) {
            return (Integer) port;
        }
        return Integer.parseInt(port.toString());
    }

    /**
     * 拼接完整 URL（对外提供的方法）
     */
    public static String getFullUrl() {
        String url = getSpringUrl();
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 如果不包含协议，添加 http://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Integer port = getServerPort();
            if (port != null && !url.contains(":")) {
                url = "http://" + url + ":" + port;
            } else {
                url = "http://" + url;
            }
        }

        // 去除末尾的斜杠
        url = url.replaceAll("/$", "");

        // 拼接 context-path
        String contextPath = getContextPath();
        if (!contextPath.isEmpty()) {
            url = url + contextPath;
        }

        return url;
    }
}