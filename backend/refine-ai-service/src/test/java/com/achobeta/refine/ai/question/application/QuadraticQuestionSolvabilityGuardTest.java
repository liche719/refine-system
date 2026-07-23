package com.achobeta.refine.ai.question.application;

import com.achobeta.refine.common.api.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuadraticQuestionSolvabilityGuardTest {
    private final QuadraticQuestionSolvabilityGuard guard = new QuadraticQuestionSolvabilityGuard();

    @Test
    void rejectsStandardFormWithOnlyTwoPointConditions() {
        assertThatThrownBy(() -> guard.verify("已知二次函数 y=ax²+bx+c 经过点(0,1)和(1,3)，求其解析式。"))
                .isInstanceOf(AppException.class)
                .hasMessage("AI 生成的二次函数题目条件不足");
    }

    @Test
    void acceptsVertexAndOneAdditionalPointBecauseTheyDetermineTheCoefficient() {
        assertThatCode(() -> guard.verify("已知二次函数 y=ax²+bx+c 的顶点为(1,2)，且经过点(3,10)，求其解析式。"))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsThreeIndependentPointConditions() {
        assertThatCode(() -> guard.verify("已知二次函数 y=ax²+bx+c 经过点(0,1)、(1,2)、(2,5)，求其解析式。"))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsTwoRootsAndOneNumericalPoint() {
        assertThatCode(() -> guard.verify("已知二次函数 y=ax²+bx+c 的两个零点为 x=1 和 x=3，且经过点(0,3)，求其解析式。"))
                .doesNotThrowAnyException();
    }
}
