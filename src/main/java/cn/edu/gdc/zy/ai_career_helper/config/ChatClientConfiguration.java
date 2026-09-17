package cn.edu.gdc.zy.ai_career_helper.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class ChatClientConfiguration {
    @Autowired
    DashScopeChatModel dashScopeChatModel;

    @Value("classpath:files/systemPrompt.st")
    Resource systemPrompt;

    @Autowired
    ChatMemory chatMemory;

    @Bean
    public ChatClient chatClient() {
        //这里的敏感词后期可以从数据库或配置文件里面读取
        List<String> words = List.of(
                "装逼",
                "草泥马",
                "撕逼",
                "爆菊",
                "傻逼",
                "你妈的",
                "性交",
                "生殖器",
                "ISIS",
                "基地组织",
                "冰毒",
                "海洛因",
                "赌博平台"
        );
        SafeGuardAdvisor safeGuardAdvisor = SafeGuardAdvisor.builder()
                .sensitiveWords(words)
                .failureResponse("你好，这个问题我暂时无法回答，让我们换个话题再聊聊吧")
                .build();

        //对话记忆的拦截器
        PromptChatMemoryAdvisor promptChatMemoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .build();

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultSystem(s->s.param("currentTime", LocalDateTime.now()))
                .defaultAdvisors(promptChatMemoryAdvisor, safeGuardAdvisor)
                .build();
        return chatClient;
    }
}
