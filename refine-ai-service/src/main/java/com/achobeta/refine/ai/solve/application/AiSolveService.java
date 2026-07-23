package com.achobeta.refine.ai.solve.application;

import com.achobeta.refine.ai.solve.application.port.SolveAiPort;
import org.springframework.stereotype.Service;

@Service
public class AiSolveService {
    private final SolveAiPort ai;

    public AiSolveService(SolveAiPort ai) {
        this.ai = ai;
    }

    public String solve(String questionContext) {
        return ai.solve(questionContext);
    }

    public void stream(String questionContext, java.util.function.Consumer<String> onToken,
                       Runnable onComplete, java.util.function.Consumer<Throwable> onError) {
        ai.streamSolve(questionContext, onToken, onComplete, onError);
    }
}
