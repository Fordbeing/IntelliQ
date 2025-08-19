package org.ff.aiintelliq.ai;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class AIIntelliQServiceTest {

    @Resource
    private AIIntelliQService aiIntelliQService;

    @Test
    void chatWithMemory() {
        String chat = aiIntelliQService.chatWithMemory(123456L, "你好呀，我是吴荣发！");
        System.out.println(chat);
        chat = aiIntelliQService.chatWithMemory(123456L, "我叫什么？");
        System.out.println(chat);
        // 测试不同id，不会根据上面的上下文
        chat = aiIntelliQService.chatWithMemory(1234567L, "我叫什么？");
        System.out.println(chat);
    }

    @Test
    void chatWithStream() {
        Flux<ServerSentEvent<String>> map = aiIntelliQService.chatWithStream(123456L, "你好呀，我是吴荣发！")
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
        map.blockLast(); // 阻塞等待流结束，确保资源正确释放
        System.out.println(map);
    }

    @Test
    void chatWithRag() {
        Result<String> result = aiIntelliQService.chatWithRag(10086L, "怎么学习 Java？有哪些常见面试题？");
        String content = result.content();
        List<Content> sources = result.sources();
        System.out.println(content);
        System.out.println(sources);
    }

    @Test
    void chatWithGuardrail() {
        String result = aiIntelliQService.chat(10086L, "kill the game");
        System.out.println(result);
    }
}