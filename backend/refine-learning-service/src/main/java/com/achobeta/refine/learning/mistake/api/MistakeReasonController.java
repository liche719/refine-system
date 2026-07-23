package com.achobeta.refine.learning.mistake.api;

import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.learning.mistake.application.MistakeService;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mistake-reason/")
public class MistakeReasonController {
    private final MistakeService service;

    public MistakeReasonController(MistakeService service) { this.service = service; }

    @PostMapping("toggle/{reasonName}")
    public Response<ReasonResponse> toggle(@Valid @RequestBody ReasonRequest request, @PathVariable String reasonName) {
        MistakeQuestion result = service.toggleReason(UserContext.get(), request.questionId(), reasonName, request.otherReasonText());
        return Response.success(toResponse(result, "错因更新成功"));
    }

    @PostMapping("update-other-reason")
    public Response<ReasonResponse> updateOther(@Valid @RequestBody ReasonRequest request) {
        MistakeQuestion result = service.updateOtherReason(UserContext.get(), request.questionId(), request.otherReasonText());
        return Response.success(toResponse(result, "其他错因更新成功"));
    }

    @GetMapping("get")
    public Response<ReasonResponse> get(@RequestParam(required = false) String userId, @RequestParam String questionId) {
        return Response.success(toResponse(service.reasons(UserContext.get(), questionId), "查询成功"));
    }

    @PostMapping("study-note/submit")
    public Response<NoteResponse> submitNote(@Valid @RequestBody NoteRequest request) {
        String userId = UserContext.get();
        service.updateNote(userId, request.questionId(), request.studyNote());
        return Response.success(new NoteResponse(userId, request.questionId(), request.studyNote(), true, "笔记更新成功"));
    }

    @GetMapping("study-note/get")
    public Response<NoteResponse> getNote(@RequestParam(required = false) String userId, @RequestParam String questionId) {
        MistakeQuestion result = service.reasons(UserContext.get(), questionId);
        return Response.success(new NoteResponse(result.userId(), result.questionId(), result.studyNote(), true, "查询成功"));
    }

    private ReasonResponse toResponse(MistakeQuestion value, String message) {
        return new ReasonResponse(value.userId(), value.questionId(), value.isCareless(), value.isUnfamiliar(),
                value.isCalculateErr(), value.isTimeShortage(), value.otherReasonFlag(), value.otherReason(), true, message);
    }

    public record ReasonRequest(String userId, @NotBlank String questionId, Integer isCareless,
                                Integer isUnfamiliar, Integer isCalculateErr, Integer isTimeShortage,
                                Integer otherReason, String otherReasonText) {
    }
    public record ReasonResponse(String userId, String questionId, Integer isCareless, Integer isUnfamiliar,
                                 Integer isCalculateErr, Integer isTimeShortage, Integer otherReason,
                                 String otherReasonText, Boolean success, String message) {
    }
    public record NoteRequest(String userId, @NotBlank String questionId, @NotBlank String studyNote) {
    }
    public record NoteResponse(String userId, String questionId, String studyNote, Boolean success, String message) {
    }
}
