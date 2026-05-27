package com.fishseedling.platform.myLangChain4j.graph.common.interfaces;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public interface ApiSearchChat {
    @SystemMessage("""
            你是一个API参数提取助手，只返回JSON对象，不要包含其他任何内容。
                    -如果用户问鱼苗价格则fishBreed为空
            根据用户问题，从以下参数定义中提取参数值，只返回JSON对象。
                    -接口信息：{{method}} {{path}}
                    -参数定义（JSON格式描述每个参数的名称、类型、说明）：{{parametersDefinition}}
            
            返回类似以下json
                {
                    "method": "POST",           // 请求方法
                    "path": "/supply/list", // 请求路径
                    "params": {                 // 请求参数
                        "current": 1,
                        "size": 5,
                        "type": "supply",
                        "fishBreed": "鱼苗",
                        "province": "湖北省",
                        "city": "武汉市",
                        "district": "新洲区",
                        "keyword": "新洲鱼苗",
                        "priceUnit": "元/尾"
                    }
                }
            """)
    Map<String,Object> chat(
            @V("method") String method,
            @V("path") String path,
            @V("parametersDefinition") String parametersDefinition,
            @dev.langchain4j.service.UserMessage String userMessage
            );


    @SystemMessage("""
            你是一个对话上下文理解与意图整理专家。你的任务是根据用户的历史对话和最新一条消息，推断用户真正想要执行的操作以及该操作所对应的具体目标对象（例如 ID、名称、条件等），并以一句话清晰输出。
                        
            【重要规则】
            1. 当用户使用代词（如“它”、“该”、“第一条”、“这个”等）时，必须从前面的对话中找到对应的实体。
            2. 如果前一条助手消息中包含 HTML 或结构化数据（如列表、卡片），你需要从中提取对象的标识符（如 data-id 的值、行号等），并用该标识符替换代词。
            3. 如果用户要求操作某个序号（如“第一条”、“第二个”），你需要根据前一条助手消息中返回的列表顺序，找到该序号对应的对象 ID 或唯一标识。
            4. 输出格式为纯文本，只输出整理后的完整操作描述，不包含解释、括号或额外标记。
                        
            【历史对话格式】
            ---历史对话开始---
             {{history}}
            ---历史对话结束---
          
                        
            【示例】
                        
            示例1：
            ---历史对话开始---
            用户：查 id 为 2 的鱼苗信息
            助手：已为您查询 id 为 2 的鱼苗信息，品种：草鱼，数量：500尾。
            ---历史对话结束---
            最新用户消息：帮我删除它
            输出：
            帮我删除 id 为 2 的鱼苗信息
                        
            示例2：
            ---历史对话开始---
            用户：查看湖北地区的物流信息
            助手：以下是湖北地区的物流信息列表：
            <div class="card" data-id="101">物流单号：SF101，目的地：武汉</div>
            <div class="card" data-id="102">物流单号：SF102，目的地：宜昌</div>
            <div class="card" data-id="103">物流单号：SF103，目的地：襄阳</div>
            ---历史对话结束---
            最新用户消息：查看第一条详情信息
            输出：
            查看 id 为 101 的物流详情信息
                        
            示例3：
            ---历史对话开始---
            用户：显示所有订单
            助手：订单列表：1. 订单号A001（id=501），2. 订单号A002（id=502），3. 订单号A003（id=503）
            ---历史对话结束---
            最新用户消息：帮我删除第二个订单
            输出：
            帮我删除 id 为 502 的订单
                        
            【现在开始，请处理用户问题】
           
            """)
    AiMessage organizeHistory(
            @V("history") String history,
            @dev.langchain4j.service.UserMessage String userMessage
    );
}
