package com.achobeta.refine.ai.rag;

import com.achobeta.refine.ai.rag.application.RagSearchService;
import com.achobeta.refine.ai.rag.application.query.RagChunk;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** LangChain4j boundary for approved knowledge-base retrieval. */
@Component
public class KnowledgeBaseTool {
    private static final int MAX_RESULTS = 3;
    private final RagSearchService ragSearch;

    public KnowledgeBaseTool(RagSearchService ragSearch) {
        this.ragSearch = ragSearch;
    }

    @Tool(name = "search_knowledge_base", value = {
            "检索经过审核的学习知识库，返回与问题相关的教材知识、公式、概念、史料分析或学习方法。",
            "当回答学科概念、公式、史实、题目解答或需要可靠知识依据时使用；不要用于问候和纯流程咨询。"
    })
    public String search(@P(name = "query", value = "用于检索知识库的完整中文问题或关键词") String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return "没有提供可检索的问题，无法返回知识库资料。";
        }
        String references = ragSearch.search(normalized, MAX_RESULTS).stream()
                .map(RagChunk::referenceText)
                .collect(Collectors.joining("\n\n"));
        return references.isBlank() ? "知识库中没有检索到足以支持该问题的可靠资料。" : references;
    }
}
