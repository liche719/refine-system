package com.achobeta.refine.ai.analytics.infrastructure;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class AnalyticsMapperProjectionContractTest {
    @Test
    void constructorMappingsMatchProjectionSignatures() {
        for (Method method : AnalyticsMapper.class.getDeclaredMethods()) {
            ConstructorArgs mapping = method.getAnnotation(ConstructorArgs.class);
            if (mapping == null) {
                continue;
            }
            Class<?> projectionType = List.class.equals(method.getReturnType())
                    ? (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]
                    : method.getReturnType();
            Class<?>[] constructorTypes = Arrays.stream(mapping.value()).map(Arg::javaType).toArray(Class<?>[]::new);
            assertThatCode(() -> projectionType.getDeclaredConstructor(constructorTypes))
                    .as("AnalyticsMapper.%s constructor mapping", method.getName())
                    .doesNotThrowAnyException();
        }
    }
}
