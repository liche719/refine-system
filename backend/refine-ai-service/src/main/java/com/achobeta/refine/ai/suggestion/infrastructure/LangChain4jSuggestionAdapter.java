package com.achobeta.refine.ai.suggestion.infrastructure;

import com.achobeta.refine.ai.provider.RefineEducationAiService;
import com.achobeta.refine.ai.shared.infrastructure.LangChain4jCallSupport;
import com.achobeta.refine.ai.suggestion.application.port.SuggestionAiPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jSuggestionAdapter implements SuggestionAiPort {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jSuggestionAdapter.class);
    private final RefineEducationAiService assistant;

    public LangChain4jSuggestionAdapter(RefineEducationAiService assistant) {
        this.assistant = assistant;
    }

    @Override
    public String suggest(String recentKnowledgePoints) {
        return LangChain4jCallSupport.complete("learning suggestion",
                () -> assistant.learningSuggestions(recentKnowledgePoints), log);
    }
}
