package com.achobeta.refine.learning.infrastructure;

import com.achobeta.refine.learning.knowledge.infrastructure.KnowledgeMapper;
import com.achobeta.refine.learning.mistake.infrastructure.MistakeMapper;
import com.achobeta.refine.learning.overview.infrastructure.OverviewMapper;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MyBatisProjectionContractTest {
    @Test
    void constructorMappingsMatchProjectionSignatures() {
        assertMappings(MistakeMapper.class);
        assertMappings(KnowledgeMapper.class);
        assertMappings(OverviewMapper.class);
    }

    @Test
    void mapperParametersUseExplicitNamesWhenSqlReferencesThem() throws NoSuchMethodException {
        Method nextId = KnowledgeMapper.class.getDeclaredMethod("nextId", String.class);

        assertThat(nextId.getParameters()[0].getAnnotation(Param.class).value()).isEqualTo("userId");
    }

    private void assertMappings(Class<?> mapperType) {
        for (Method method : mapperType.getDeclaredMethods()) {
            ConstructorArgs mapping = method.getAnnotation(ConstructorArgs.class);
            if (mapping == null) {
                continue;
            }
            Class<?> projectionType = projectionType(method);
            Class<?>[] constructorTypes = Arrays.stream(mapping.value()).map(Arg::javaType).toArray(Class<?>[]::new);
            assertThatCode(() -> projectionType.getDeclaredConstructor(constructorTypes))
                    .as("%s.%s constructor mapping", mapperType.getSimpleName(), method.getName())
                    .doesNotThrowAnyException();
        }
    }

    private Class<?> projectionType(Method method) {
        if (List.class.equals(method.getReturnType())) {
            return (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        }
        return method.getReturnType();
    }
}
