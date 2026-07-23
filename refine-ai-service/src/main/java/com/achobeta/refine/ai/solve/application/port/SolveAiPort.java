package com.achobeta.refine.ai.solve.application.port;

import java.util.function.Consumer;

public interface SolveAiPort {
    String solve(String questionContext);

    void streamSolve(String questionContext, Consumer<String> onToken,
                     Runnable onComplete, Consumer<Throwable> onError);
}
