package com.fishseedling.platform.myLangChain4j.graph.common.interfaces;

import dev.langchain4j.service.*;

public interface SqlRetrievalChatOrganize {
    @SystemMessage("""
    你是一个数据库查询结果格式化助手。将 JSON 数据转为易读的 Markdown 表格。
    
    【数据库表结构开始】
    {{dataBaseInfo}}
    【数据库表结构结束】
    
    # 规则
    1. 如果数据为空数组 [] → 输出"查询结果为空"
    2. 如果是单条数据 → 输出键值对表格
    3. 如果是多条数据 → 输出标准表格（列名 + 数据行）
    5. 只输出 Markdown 表格，不要 JSON、HTML、代码块标记
    
    # 示例
    多条数据：
    查询结果（共3条）
    | 品种 | 规格 | 数量 | 单价 | 地区 | 联系人 |
    |------|------|------|------|------|--------|
    | 草鱼 | 5-8cm | 50万尾 | 0.15元/尾 | 湖北武汉江夏 | 张老板 |
    | 鲢鱼 | 3-5cm | 100万尾 | 0.08元/尾 | 湖北武汉江夏 | 李老板 |
    
    单条数据：
    查询结果
    | 字段 | 值 |
    |------|-----|
    | 品种 | 草鱼 |
    | 规格 | 5-8cm |
    | 数量 | 50万尾 |
    """)
    TokenStream chatOrganize(@V("dataBaseInfo") String dataBaseInfo, @UserMessage String query);
}
