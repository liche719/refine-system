package com.achobeta.refine.ai.suggestion.application;

import com.achobeta.refine.ai.learning.application.port.LearningServicePort;
import com.achobeta.refine.ai.suggestion.application.port.SuggestionAiPort;
import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiLearningSuggestionService {
    private static final Pattern ADVICE_PATTERN = Pattern.compile("【([^】]+)】\\s*(.+)");

    private final LearningServicePort learningClient;
    private final SuggestionAiPort ai;

    public AiLearningSuggestionService(LearningServicePort learningClient, SuggestionAiPort ai) {
        this.learningClient = learningClient;
        this.ai = ai;
    }

    public List<KeyPointSuggestion> suggestions(String userId) {
        List<RecentKnowledgePoint> points = learningClient.recentKnowledge(userId, 10);
        if (points.isEmpty()) {
            return List.of(new KeyPointSuggestion("暂无学习数据", "请先完成学习，再查看建议"));
        }
        String context = points.stream()
                .map(point -> "- " + point.name() + "：" + value(point.description()))
                .collect(Collectors.joining("\n"));
        return parse(ai.suggest(context));
    }

    private List<KeyPointSuggestion> parse(String text) {
        List<KeyPointSuggestion> suggestions = new ArrayList<>();
        for (String line : text.split("\\R")) {
            Matcher matcher = ADVICE_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                suggestions.add(new KeyPointSuggestion(matcher.group(1).trim(), matcher.group(2).trim()));
            }
        }
        if (suggestions.isEmpty() && !text.isBlank()) {
            suggestions.add(new KeyPointSuggestion("综合建议", text.trim()));
        }
        return suggestions;
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "暂无描述" : value;
    }

    public record KeyPointSuggestion(String knowledgePoint, String reviewReason) { }
}
