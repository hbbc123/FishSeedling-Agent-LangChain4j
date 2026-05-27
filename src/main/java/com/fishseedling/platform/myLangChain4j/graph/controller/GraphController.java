package com.fishseedling.platform.myLangChain4j.graph.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.myLangChain4j.graph.pojo.dto.AiReception;
import com.fishseedling.platform.myLangChain4j.graph.service.GraphService;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

@RestController
@RequestMapping("/ai")
public class GraphController {

    @Resource
    GraphService graphService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * SSE 流式 AI 对话入口
     */
    @PostMapping("/chat")
    public SseEmitter ai(@RequestBody AiReception aiReception, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        graphService.receiveAndProcess(aiReception, request, emitter);
        return emitter;
    }

    /**
     * 获取会话历史消息
     * 使用 LangChain4j 内置序列化解决 Jackson 无法序列化 SystemMessage 的问题
     */
    @PostMapping("/getHistory")
    public Result<List<Map<String, Object>>> history(@RequestBody AiReception aiReception, HttpServletRequest request) {
        try {
            List<ChatMessage> userHistory = graphService.getUserHistory(aiReception);
            if (userHistory == null || userHistory.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            // 使用 LangChain4j 的序列化器转为 JSON，再用 Jackson 解析为 List<Map>
            String json = messagesToJson(userHistory);
            List<Map<String, Object>> result = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取历史失败：" + e.getMessage());
        }
    }

    @PostMapping("/delUserHistory")
    public Result<String> delUserHistory(@RequestBody AiReception aiReception, HttpServletRequest request) {
        graphService.delUserHistory(aiReception);
        return Result.success("删除成功");
    }
}
