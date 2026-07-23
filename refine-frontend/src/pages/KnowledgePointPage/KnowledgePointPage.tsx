import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ReactFlow, {
  Background,
  Controls,
  type Edge,
  type Node,
} from 'reactflow';
import 'reactflow/dist/style.css';
import {
  BookOpen,
  CheckCircle2,
  LibraryBig,
  Network,
  Save,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';

import { AiChatPanel } from '@/components/business/AiChatPanel';
import EmptyState from '@/components/common/EmptyState';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import MarkdownContent from '@/components/common/MarkdownContent';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  fetchDefinition,
  fetchRelatedPoints,
  fetchRelatedQuestionsOrNote,
  fetchRootPoints,
  fetchStatistic,
  fetchTooltip,
  markAsMastered,
  saveNote,
  type KnowledgePointNode,
  type KnowledgeTooltip,
  type RelatedData,
} from '@/services/apis/KnowledgePointApi';
import {
  generateQuestion,
  judgeQuestion,
  recordQuestion,
  type GenerationData,
} from '@/services/apis/questionapi';
import { errorMessage, unwrap } from '@/utils/api';

const EMPTY_RELATED: RelatedData = { questions: [], note: '' };
const ALL_SUBJECTS = '__all__';

export default function KnowledgePointPage() {
  const navigate = useNavigate();
  const [subject, setSubject] = useState(ALL_SUBJECTS);
  const [roots, setRoots] = useState<KnowledgePointNode[]>([]);
  const [active, setActive] = useState<KnowledgePointNode | null>(null);
  const [relatedPoints, setRelatedPoints] = useState<KnowledgePointNode[]>([]);
  const [related, setRelated] = useState<RelatedData>(EMPTY_RELATED);
  const [definition, setDefinition] = useState('');
  const [statistic, setStatistic] = useState('');
  const [tooltip, setTooltip] = useState<KnowledgeTooltip | null>(null);
  const [note, setNote] = useState('');
  const [loadingRoots, setLoadingRoots] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);

  useEffect(() => {
    let activeRequest = true;
    setLoadingRoots(true);
    fetchRootPoints({ subject: subject === ALL_SUBJECTS ? '' : subject })
      .then((data) => {
        if (!activeRequest) return;
        setRoots(data);
        setActive(data[0] || null);
      })
      .catch((error) => toast.error(errorMessage(error, '知识点加载失败')))
      .finally(() => activeRequest && setLoadingRoots(false));
    return () => {
      activeRequest = false;
    };
  }, [subject]);

  const loadDetail = useCallback(async (point: KnowledgePointNode) => {
    setLoadingDetail(true);
    try {
      const [
        nextDefinition,
        nextStatistic,
        nextRelated,
        nextPoints,
        nextTooltip,
      ] = await Promise.all([
        fetchDefinition(point.id),
        fetchStatistic(point.id),
        fetchRelatedQuestionsOrNote(point.id),
        fetchRelatedPoints(point.id),
        fetchTooltip(point.id),
      ]);
      setDefinition(nextDefinition);
      setStatistic(nextStatistic);
      setRelated(nextRelated);
      setRelatedPoints(nextPoints);
      setTooltip(nextTooltip);
      setNote(nextRelated.note || '');
    } catch (error) {
      toast.error(errorMessage(error, '知识点详情加载失败'));
    } finally {
      setLoadingDetail(false);
    }
  }, []);

  useEffect(() => {
    if (active) loadDetail(active);
    else {
      setDefinition('');
      setRelated(EMPTY_RELATED);
      setRelatedPoints([]);
    }
  }, [active, loadDetail]);

  const graph = useMemo(
    () => buildGraph(active, relatedPoints),
    [active, relatedPoints],
  );

  const persistNote = async () => {
    if (!active || !note.trim()) return;
    try {
      await saveNote(active.id, note.trim());
      toast.success('笔记已保存');
    } catch (error) {
      toast.error(errorMessage(error, '笔记保存失败'));
    }
  };

  const master = async () => {
    if (!active) return;
    try {
      await markAsMastered(active.id);
      toast.success('已标记为掌握');
      await loadDetail(active);
    } catch (error) {
      toast.error(errorMessage(error, '状态更新失败'));
    }
  };

  return (
    <main className="app-page">
      <header className="page-heading">
        <div>
          <p className="page-kicker">KNOWLEDGE MAP</p>
          <h1>知识点库</h1>
          <p>从错题出发，查看知识关联并进行针对性练习。</p>
        </div>
        <Select value={subject} onValueChange={setSubject}>
          <SelectTrigger className="w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_SUBJECTS}>全部学科</SelectItem>
            {['数学', '物理', '化学', '英语'].map((item) => (
              <SelectItem key={item} value={item}>
                {item}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </header>

      {loadingRoots ? (
        <LoadingSpinner text="正在生成知识图谱" />
      ) : roots.length === 0 ? (
        <EmptyState
          icon={LibraryBig}
          title="暂无知识点"
          description="上传并解析错题后，知识点会自动沉淀在这里。"
          action={
            <Button size="sm" onClick={() => navigate('/upload-question')}>
              上传错题
            </Button>
          }
        />
      ) : (
        <div className="knowledge-layout">
          <aside className="knowledge-index">
            <p>知识目录</p>
            {roots.map((point) => (
              <button
                key={point.id}
                className={active?.id === point.id ? 'active' : ''}
                onClick={() => setActive(point)}
              >
                <span>{point.keyPoints}</span>
                <small>#{point.id}</small>
              </button>
            ))}
          </aside>

          <div className="knowledge-main">
            <section className="knowledge-graph" aria-label="知识关联图">
              <div className="section-title">
                <div>
                  <p>CONNECTIONS</p>
                  <h2>{active?.keyPoints}</h2>
                </div>
                <Network className="size-5" />
              </div>
              <ReactFlow
                nodes={graph.nodes}
                edges={graph.edges}
                fitView
                minZoom={0.5}
                maxZoom={1.5}
                onNodeClick={(_, node) => {
                  const next = [...roots, ...relatedPoints].find(
                    (item) => String(item.id) === node.id,
                  );
                  if (next) setActive(next);
                }}
              >
                <Background gap={24} size={1} color="#d7ddd9" />
                <Controls showInteractive={false} />
              </ReactFlow>
            </section>

            <section className="knowledge-detail">
              {loadingDetail ? (
                <LoadingSpinner text="正在读取知识点详情" />
              ) : (
                <>
                  <div className="section-title">
                    <div>
                      <p>NOTES</p>
                      <h2>知识讲解</h2>
                    </div>
                    <Badge variant="outline">
                      掌握度{' '}
                      {Math.round((tooltip?.degreeOfProficiency || 0) * 100)}%
                    </Badge>
                  </div>
                  <div className="knowledge-copy">
                    <p>{definition || '暂无知识点详情'}</p>
                    <small>{statistic}</small>
                  </div>
                  <div className="related-question-list">
                    <h3>关联错题</h3>
                    {related.questions.map((question) => (
                      <article key={question.id}>
                        <span>{question.id}</span>
                        <p>{question.question}</p>
                      </article>
                    ))}
                    {!related.questions.length && (
                      <p className="text-sm text-muted-foreground">
                        暂无关联错题
                      </p>
                    )}
                  </div>
                  <div className="note-editor">
                    <label htmlFor="knowledge-note">复习笔记</label>
                    <Textarea
                      id="knowledge-note"
                      value={note}
                      onChange={(event) => setNote(event.target.value)}
                      placeholder="记录公式、误区或解题线索"
                    />
                    <div>
                      <Button variant="outline" onClick={master}>
                        <CheckCircle2 className="size-4" /> 标记掌握
                      </Button>
                      <Button onClick={persistNote}>
                        <Save className="size-4" /> 保存笔记
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </section>
          </div>

          <aside className="knowledge-assistant">
            <PracticePanel
              mistakeIds={related.questions.map((item) => item.id)}
            />
            <div className="min-h-[360px]">
              <AiChatPanel mode="embedded" />
            </div>
          </aside>
        </div>
      )}
    </main>
  );
}

function PracticePanel({ mistakeIds }: { mistakeIds: number[] }) {
  const [question, setQuestion] = useState<GenerationData | null>(null);
  const [answer, setAnswer] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  const controller = useRef<AbortController | null>(null);

  const generate = async () => {
    if (!mistakeIds.length) return toast.info('当前知识点暂无关联错题');
    setLoading(true);
    setResult('');
    try {
      setQuestion(unwrap(await generateQuestion(mistakeIds[0])));
    } catch (error) {
      toast.error(errorMessage(error, '练习题生成失败'));
    } finally {
      setLoading(false);
    }
  };

  const judge = async () => {
    if (!question || !answer.trim()) return;
    controller.current = new AbortController();
    setLoading(true);
    setResult('');
    try {
      await judgeQuestion(
        question.questionId,
        answer.trim(),
        (chunk) => setResult((value) => value + chunk),
        controller.current.signal,
      );
    } catch (error) {
      toast.error(errorMessage(error, '判题失败'));
    } finally {
      setLoading(false);
    }
  };

  const record = async () => {
    if (!question) return;
    try {
      unwrap(await recordQuestion(question.questionId));
      toast.success('已加入错题库');
    } catch (error) {
      toast.error(errorMessage(error, '录入失败'));
    }
  };

  return (
    <section className="practice-panel">
      <div className="section-title">
        <div>
          <p>PRACTICE</p>
          <h2>举一反三</h2>
        </div>
        <Sparkles className="size-5" />
      </div>
      {!question ? (
        <div className="practice-empty">
          <BookOpen className="size-5" />
          <p>根据关联错题生成一道变式题。</p>
          <Button size="sm" onClick={generate} disabled={loading}>
            {loading ? '生成中...' : '生成练习题'}
          </Button>
          {loading && (
            <small className="text-muted-foreground">
              AI 正在生成变式题，通常需要几秒钟
            </small>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          <MarkdownContent content={question.content} />
          <Textarea
            value={answer}
            onChange={(event) => setAnswer(event.target.value)}
            placeholder="输入你的答案"
          />
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={judge}
              disabled={loading || !answer.trim()}
            >
              {loading ? '判题中...' : '提交答案'}
            </Button>
            <Button size="sm" variant="outline" onClick={record}>
              加入错题
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={generate}
              disabled={loading}
            >
              换一道
            </Button>
          </div>
          {result && (
            <div className="practice-result">
              <MarkdownContent content={result} isStreaming={loading} />
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function buildGraph(
  active: KnowledgePointNode | null,
  related: KnowledgePointNode[],
): { nodes: Node[]; edges: Edge[] } {
  if (!active) return { nodes: [], edges: [] };
  const nodes: Node[] = [
    {
      id: String(active.id),
      position: { x: 40, y: 120 },
      data: { label: active.keyPoints },
      style: {
        background: '#1f6b52',
        color: '#fff',
        border: 0,
        borderRadius: 6,
        width: 150,
        padding: 12,
      },
    },
  ];
  related.forEach((point, index) =>
    nodes.push({
      id: String(point.id),
      position: { x: 300, y: 20 + index * 90 },
      data: { label: point.keyPoints },
      style: {
        background: '#fff',
        color: '#1d2925',
        border: '1px solid #b8c8c1',
        borderRadius: 6,
        width: 150,
        padding: 10,
      },
    }),
  );
  const edges = related.map((point) => ({
    id: `${active.id}-${point.id}`,
    source: String(active.id),
    target: String(point.id),
    style: { stroke: '#78998b' },
  }));
  return { nodes, edges };
}
