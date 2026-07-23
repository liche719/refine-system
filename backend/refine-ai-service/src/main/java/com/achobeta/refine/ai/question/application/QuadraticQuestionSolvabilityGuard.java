package com.achobeta.refine.ai.question.application;

import com.achobeta.refine.common.api.AppException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rejects only the unambiguous under-specified form that can be recognized
 * without pretending to be a general-purpose mathematics parser.
 */
final class QuadraticQuestionSolvabilityGuard {
    private static final Pattern STANDARD_FORM = Pattern.compile("y=ax(?:\\^2|²)\\+bx\\+c", Pattern.CASE_INSENSITIVE);
    private static final Pattern COORDINATE = Pattern.compile("[（(]\\s*[-+]?\\d+(?:\\.\\d+)?\\s*[,，]\\s*[-+]?\\d+(?:\\.\\d+)?\\s*[）)]");

    void verify(String content) {
        String normalized = content.replaceAll("\\s+", "").replace("－", "-");
        if (!asksForStandardQuadraticExpression(normalized) || hasEnoughIndependentConditions(normalized)) {
            return;
        }
        throw new AppException(10001, "AI 生成的二次函数题目条件不足");
    }

    private boolean asksForStandardQuadraticExpression(String content) {
        return STANDARD_FORM.matcher(content).find()
                && (content.contains("解析式") || content.contains("表达式") || content.contains("函数关系式"));
    }

    private boolean hasEnoughIndependentConditions(String content) {
        int coordinates = countCoordinates(content);
        if (content.contains("顶点")) {
            // A vertex contributes two constraints. One further point fixes a in vertex form.
            return coordinates >= 2;
        }
        if (containsTwoRoots(content)) {
            return coordinates >= 1;
        }
        return coordinates >= 3;
    }

    private int countCoordinates(String content) {
        Matcher matcher = COORDINATE.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean containsTwoRoots(String content) {
        return content.contains("两个零点") || content.contains("两根") || content.contains("两个根")
                || content.contains("交于A、B") || content.contains("交于A,B")
                || content.contains("交于a、b") || content.contains("交于a,b");
    }
}
