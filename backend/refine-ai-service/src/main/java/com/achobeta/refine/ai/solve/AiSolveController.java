package com.achobeta.refine.ai.solve;

import com.achobeta.refine.ai.solve.application.AiSolveService;
import com.achobeta.refine.ai.stream.SseSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/v1/solve/")
public class AiSolveController {
    private final AiSolveService service;
    private final Executor executor;

    public AiSolveController(AiSolveService service, @Qualifier("aiExecutor") Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    @PostMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody SolveRequest request) {
        return SseSupport.emitStreaming(executor, (onToken, onComplete, onError) ->
                service.stream(request.questionContext(), onToken, onComplete, onError));
    }

    public record SolveRequest(@NotBlank String questionContext) { }
}
