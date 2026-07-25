package com.achobeta.refine.ai.solve.infrastructure;

import com.achobeta.refine.ai.provider.RefineSolveAiService;
import com.achobeta.refine.ai.shared.infrastructure.LangChain4jCallSupport;
import com.achobeta.refine.ai.solve.application.port.SolveAiPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class LangChain4jSolveAdapter implements SolveAiPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jSolveAdapter.class);
    private final RefineSolveAiService assistant;

    public LangChain4jSolveAdapter(RefineSolveAiService assistant) {
        this.assistant = assistant;
    }

    @Override
    public String solve(String questionContext) {
        return LangChain4jCallSupport.complete("solve", () -> assistant.solve(questionContext), log);
    }

    @Override
    public void streamSolve(String questionContext, Consumer<String> onToken,
                            Runnable onComplete, Consumer<Throwable> onError) {
        LangChain4jCallSupport.stream(() -> assistant.streamSolve(questionContext), onToken, onComplete, onError);
    }
}
