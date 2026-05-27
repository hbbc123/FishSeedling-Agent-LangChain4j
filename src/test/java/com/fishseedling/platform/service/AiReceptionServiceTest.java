package com.fishseedling.platform.service;


import com.fishseedling.platform.myLangChain4j.graph.pojo.dto.AiReception;
import com.fishseedling.platform.myLangChain4j.graph.service.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AiReceptionServiceTest {

    @Autowired
    private GraphService graphService;

    @Test
    void testInvokeAgent() {
        AiReception aiReception = new AiReception();
        aiReception.setMsg("鱼养殖行业前景");
        aiReception.setThreadId("1111");
        aiReception.setUserId(54545);


        // 2. 创建 Mock HttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 设置请求参数
        request.setMethod("POST");
        request.setRequestURI("/api/receive");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Test Client");
        request.addParameter("source", "web");

        graphService.receiveAndProcess(aiReception,request);
    }
}