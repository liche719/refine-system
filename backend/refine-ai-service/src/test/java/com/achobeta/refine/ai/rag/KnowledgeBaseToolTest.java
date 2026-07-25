package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.rag.application.RagSearchService;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeBaseToolTest {
    private final RagSearchService ragSearch = mock(RagSearchService.class);
    private final KnowledgeBaseTool tool = new KnowledgeBaseTool(ragSearch);

    @Test
    void returnsApprovedKnowledgeWithSourceLabels() {
        RagChunk chunk = new RagChunk(1L, "二次函数顶点的横坐标为 -b/(2a)。", metadata(), 0.9D, 0.8D, 0.03D);
        when(ragSearch.search("二次函数顶点怎么求", 3)).thenReturn(List.of(chunk));

        String result = tool.search(" 二次函数顶点怎么求 ");

        assertThat(result).contains("【来源：", "二次函数顶点的横坐标");
        verify(ragSearch).search("二次函数顶点怎么求", 3);
    }

    @Test
    void reportsMissingKnowledgeWithoutInventingReferences() {
        when(ragSearch.search("你好", 3)).thenReturn(List.of());

        assertThat(tool.search("你好")).contains("没有检索到").doesNotContain("【来源：");
    }

    @Test
    void rejectsBlankToolArgumentsWithoutCallingRetrieval() {
        assertThat(tool.search("  ")).contains("没有提供");
        verifyNoInteractions(ragSearch);
    }

    private RagDocumentMetadata metadata() {
        return new RagDocumentMetadata("数学-九年级-二次函数.md", "a".repeat(64), "二次函数的图像、顶点与最值",
                "数学", "九年级", "项目原创审核资料", "二次函数", "顶点与最值", "", true);
    }
}
