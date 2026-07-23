package com.achobeta.refine.ai.conversation;

import com.achobeta.refine.ai.stream.SseSupport;
import com.achobeta.refine.ai.conversation.application.ConversationService;
import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/v1/conversation/")
public class ConversationController {
    private final ConversationService service;
    private final Executor executor;

    public ConversationController(ConversationService service, @Qualifier("aiExecutor") Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    @PostMapping(value = "send-message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@Valid @RequestBody SendMessageRequest request) {
        String userId = UserContext.get();
        return SseSupport.emitStreaming(executor, (onToken, onComplete, onError) ->
                service.streamSend(userId, request.conversationId(), request.message(),
                        onToken, onComplete, onError));
    }

    @PostMapping(value = "solve-with-context", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter solve(@Valid @RequestBody SolveWithContextRequest request) {
        String userId = UserContext.get();
        return SseSupport.emitStreaming(executor, (onToken, onComplete, onError) ->
                service.streamSolveWithContext(userId, request.questionId(), request.questionContent(), request.userQuestion(),
                        onToken, onComplete, onError));
    }

    @DeleteMapping("delete/{conversationId}")
    public Response<Boolean> delete(@PathVariable String conversationId) {
        return Response.success(service.delete(UserContext.get(), conversationId));
    }

    public record SendMessageRequest(@NotBlank String conversationId, @NotBlank String message) { }
    public record SolveWithContextRequest(@NotBlank String questionId, String questionContent, @NotBlank String userQuestion) { }
}
