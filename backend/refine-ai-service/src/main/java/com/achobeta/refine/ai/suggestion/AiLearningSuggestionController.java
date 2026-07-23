package com.achobeta.refine.ai.suggestion;

import com.achobeta.refine.ai.suggestion.application.AiLearningSuggestionService;
import com.achobeta.refine.common.security.UserContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai_suggession")
public class AiLearningSuggestionController {
    private final AiLearningSuggestionService service;

    public AiLearningSuggestionController(AiLearningSuggestionService service) {
        this.service = service;
    }

    @RequestMapping("/get_key_point")
    public List<AiLearningSuggestionService.KeyPointSuggestion> getKeyPoint() {
        return service.suggestions(UserContext.get());
    }
}
