package cn.edu.gdc.zy.ai_career_helper.controller;

import cn.edu.gdc.zy.ai_career_helper.service.CareerService;
import cn.edu.gdc.zy.ai_career_helper.tool.CareerTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
@CrossOrigin
public class AgentController {

    @Autowired
    ChatClient chatClient;

    @Autowired
    CareerService careerService;

    // 用户注册
    @GetMapping(value = "/register", produces = "text/plain;charset=utf-8")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        return careerService.register(username, password);
    }

    // 用户登录
    @GetMapping("/login")
    public Map<String, Object> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        String result = careerService.login(username, password);

        Map<String, Object> response = new HashMap<>();

        if (result.startsWith("登录成功")) {

            // 从 "登录成功，用户ID：1" 中提取 1
            String idString = result.substring(result.lastIndexOf("：") + 1);

            response.put("success", true);
            response.put("id", Long.parseLong(idString));
            response.put("message", "登录成功");

        } else {

            response.put("success", false);
            response.put("id", null);
            response.put("message", result);
        }

        return response;
    }

    // AI 对话
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(
            @RequestBody Map<String, String> request,
            @RequestParam("id") Long id
    ) {

        String message = request.getOrDefault("message", "你好");

        // 创建当前登录用户专属的工具
        CareerTools tools = new CareerTools(careerService, id);

        return chatClient.prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(
                                ChatMemory.CONVERSATION_ID,
                                String.valueOf(id)
                        )
                )
                .tools(tools)
                .user(message)
                .stream()
                .content();
    }
}