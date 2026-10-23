package com.achobeta.refine.learning.mistake.application.query;

/**
 * 生成题上下文在 Learning 服务内部的投影结果。
 *
 * <p>刻意不复用 contracts 里的 {@code GenerationContextResponse}：
 * mapper 直接投影成跨进程契约，会让「改一列」同时牵动两个服务的发布节奏，
 * 所谓的版本化契约也就名存实亡。这里用本地 record 接 SQL，再在 application 层映射成契约。
 */
public record GenerationContext(
        Long mistakeQuestionId,
        String subject,
        Integer knowledgePointId,
        String knowledgePointName,
        String questionContent) {
}
