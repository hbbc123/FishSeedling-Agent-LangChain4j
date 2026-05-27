package com.fishseedling.platform.myLangChain4j.graph.enums;

import cn.hutool.json.JSONUtil;
import com.fishseedling.platform.entity.Logistics;
import com.fishseedling.platform.entity.Post;

// 使用普通类代替枚举，支持泛型
public class ApiMethodMapping<T> {

    public static final ApiMethodMapping<Post> POST_LIST =
            new ApiMethodMapping<>("/front/post/list", "/post-detail/{id}", Post.class);

    public static final ApiMethodMapping<Logistics> LOGISTICS_LIST =
            new ApiMethodMapping<>("/front/logistics/list", "/logistics-detail/{id}", Logistics.class);

    private final String listPath;
    private final String detailPattern;
    private final Class<T> targetClass;

    private ApiMethodMapping(String listPath, String detailPattern, Class<T> targetClass) {
        this.listPath = listPath;
        this.detailPattern = detailPattern;
        this.targetClass = targetClass;
    }

    public String getListPath() {
        return listPath;
    }

    public String getDetailPattern() {
        return detailPattern;
    }

    // 返回明确的 Class<T>
    public Class<T> getTargetClass() {
        return targetClass;
    }

    public String buildDetailUrl(Object id) {
        return detailPattern.replace("{id}", String.valueOf(id));
    }

    // 根据路径查找（需要手动维护映射）
    public static ApiMethodMapping<?> fromListPath(String listPath) {
        if (POST_LIST.listPath.equals(listPath)) return POST_LIST;
        if (LOGISTICS_LIST.listPath.equals(listPath)) return LOGISTICS_LIST;
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getTargetClassTyped() {
        return (Class<T>) targetClass;
    }
}