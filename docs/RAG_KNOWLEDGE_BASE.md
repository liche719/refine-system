# 教材知识库 RAG

## 定位

RAG 只存放经人工审核、可追溯版本的教材、教纲和知识点资料。它不保存用户错题、聊天记录或未经验证的网络答案。

资料原文是事实来源；PgVector 保存资料分块、向量、校验值与出处，用于检索，不能被当作唯一的事实源。

## 资料格式

将资料放在 `docs/rag/` 下。Markdown 或文本资料必须以如下 front matter 开头，只有 `approved: true` 才会导入：

```markdown
---
title: 一元一次方程
subject: 数学
grade: 七年级
textbookVersion: 人教版
chapter: 第三章
section: 3.1 从算式到方程
pageReference: 82
approved: true
---

方程中含有未知数……
```

`title` 必填；`subject`、`grade`、教材版本、章节、页码建议完整填写。PDF 与 DOCX 在具备同等可审核元数据前不应标记为可导入，推荐先转为 Markdown 后入库。

## 导入与检索

- AI 服务启动时以文件 SHA-256、Embedding 模型和维度判断是否已导入；相同资料不会重复调用远程 Embedding。
- LangChain4j 使用递归分块，默认约 450 tokens、80 tokens 重叠，再批量调用远程 Embedding。
- PgVector HNSW 负责余弦相似度召回；`pg_trgm` 负责术语与关键词召回；应用层以 RRF 融合排序。
- 只有超过融合分数阈值的资料进入模型上下文；资料块始终包含 `【来源：教材 · 章节】`。
- 不存在启动清表行为；旧 `knowledge_embeddings` 保留，新的教材 RAG 使用 `rag_documents` 与 `rag_chunks`。
- 紧急降级时设置 `refine.pgvector.retrieval-enabled=false`；不会删除任何资料或影响普通 AI 对话。

## 资料治理

1. 先核对教材版本、章节、页码和版权授权，再把资料标记为 `approved: true`。
2. 修改资料后重启 AI 服务即可只重建该资料的分块；撤销资料时先删除或取消其审核标记，再执行一次受控重新索引。
3. 模型或向量维度变更需要新建索引版本并重新导入，不能混用不同维度的向量。
4. 维护问题—期望章节—不可回答问题的离线评测集，至少检查 Recall@3、MRR 与来源引用正确率。
