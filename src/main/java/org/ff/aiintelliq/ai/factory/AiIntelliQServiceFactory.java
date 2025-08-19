package org.ff.aiintelliq.ai.factory;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.ff.aiintelliq.ai.AIIntelliQService;
import org.ff.aiintelliq.ai.memorystore.PersistentChatMemoryStore;
import org.ff.aiintelliq.ai.tool.InterviewQuestionTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AiIntelliQServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    // 暂时不支持
//    @Resource
//    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel streamingChatModel;

    private final Map<Long, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    @Bean
    public AIIntelliQService aiIntelliQService() {
        return AiServices.builder(AIIntelliQService.class)
                // 模型
                .chatModel(myQwenChatModel)
                // 会话记忆
                .chatMemoryProvider(memoryId ->
                        memoryStore.computeIfAbsent((Long) memoryId,
                                id -> MessageWindowChatMemory.withMaxMessages(10)
                        )
                )
                // rag检索增强
                .contentRetriever(contentRetriever)
                // 工具调用
                .tools(new InterviewQuestionTool())
                // mcp工具调用
                //.toolProvider(mcpToolProvider)
                .streamingChatModel(streamingChatModel)
                .build();
    }


}
