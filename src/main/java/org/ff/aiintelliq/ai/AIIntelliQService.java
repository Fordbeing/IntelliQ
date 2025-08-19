package org.ff.aiintelliq.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import org.ff.aiintelliq.ai.safeguardrail.SafeInputGuardrail;
import reactor.core.publisher.Flux;

@InputGuardrails({SafeInputGuardrail.class})
public interface AIIntelliQService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId long memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    String chatWithMemory(@MemoryId long memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatWithStream(@MemoryId long memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(@MemoryId long memoryId, @UserMessage String userMessage);
}
