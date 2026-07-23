package com.achobeta.refine.ai.question;

import com.achobeta.refine.ai.stream.SseSupport;
import com.achobeta.refine.ai.question.application.QuestionWorkflowService;
import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@Validated
@RestController
@RequestMapping("/api/question")
public class QuestionController {
    private final QuestionWorkflowService service;
    private final Executor executor;

    public QuestionController(QuestionWorkflowService service, @Qualifier("aiExecutor") Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    @PostMapping("/generation")
    public Response<QuestionWorkflowService.GeneratedQuestion> generate(@RequestParam @NotNull Long mistakeQuestionId) {
        return Response.success(service.generate(UserContext.get(), mistakeQuestionId));
    }

    @PostMapping(value = "/judge", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter judge(@RequestParam @NotBlank String questionId, @RequestParam @NotBlank String answer) {
        String userId = UserContext.get();
        return SseSupport.emitStreaming(executor, (onToken, onComplete, onError) ->
                service.streamJudge(userId, questionId, answer, onToken, onComplete, onError));
    }

    @PostMapping("/record")
    public Response<String> record(@RequestParam @NotBlank String questionId) {
        service.record(UserContext.get(), questionId);
        return Response.success("已加入错题");
    }
}
