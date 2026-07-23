import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  BookOpenCheck,
  BrainCircuit,
  Clock3,
  RefreshCcw,
  Sparkles,
  Target,
  Upload,
} from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { ChartLineMultiple } from '@/components/layout/ChartLineMultiple';
import {
  GetKeyPoint,
  GetOverDue,
  GetOverview,
  GetStudyDynamic,
  GetTrickyKnowledge,
} from '@/services/home/home';
import type {
  KeyPointSuggestion,
  OverviewResponse,
  OverdueData,
  StudyDynamic,
  TrickyKnowledge,
} from '@/services/home/type';
import { getStatistics } from '@/services/myQuestion/myQuestion';
import type { AnalysisData } from '@/services/myQuestion/type';

const EMPTY_OVERVIEW: OverviewResponse = {
  questionsNum: 0,
  reviewRate: 0,
  hardQuestions: 0,
  studyTime: 0,
};

export default function Home() {
  const navigate = useNavigate();
  const [overview, setOverview] = useState(EMPTY_OVERVIEW);
  const [overdue, setOverdue] = useState<OverdueData | null>(null);
  const [dynamic, setDynamic] = useState<StudyDynamic | null>(null);
  const [tricky, setTricky] = useState<TrickyKnowledge[]>([]);
  const [suggestions, setSuggestions] = useState<KeyPointSuggestion[]>([]);
  const [statistics, setStatistics] = useState<AnalysisData>({
    subjectDistribution: [],
    knowledgeDistribution: [],
    reviewTrend: [],
  });
  const [loading, setLoading] = useState(true);
  const [failedSections, setFailedSections] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFailedSections(0);

    const tasks = [
      GetOverview().then((value) => active && setOverview(value)),
      GetOverDue().then((value) => active && setOverdue(value)),
      GetStudyDynamic().then((value) => active && setDynamic(value)),
      GetTrickyKnowledge().then((value) => active && setTricky(value)),
      GetKeyPoint().then((value) => active && setSuggestions(value)),
      getStatistics().then((value) => active && setStatistics(value)),
    ];

    Promise.allSettled(tasks).then((results) => {
      if (!active) return;
      const failed = results.filter(
        (result) => result.status === 'rejected',
      ).length;
      setFailedSections(failed);
      setLoading(false);
      if (failed) toast.warning(`${failed} 项学习数据暂未加载，可稍后重试`);
    });
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const metrics = [
    {
      label: '累计错题',
      value: overview.questionsNum,
      suffix: '题',
      icon: BookOpenCheck,
    },
    {
      label: '复习完成率',
      value: Math.round(overview.reviewRate),
      suffix: '%',
      icon: RefreshCcw,
    },
    {
      label: '易错知识点',
      value: overview.hardQuestions,
      suffix: '个',
      icon: Target,
    },
    {
      label: '学习投入',
      value: overview.studyTime,
      suffix: '分钟',
      icon: Clock3,
    },
  ];

  return (
    <main className="app-page">
      <header className="page-heading home-heading">
        <div>
          <p className="page-kicker">LEARNING PULSE</p>
          <h1>今天，从一道错题开始</h1>
          <p>回看薄弱点，完成一次有目标的复习。</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {failedSections > 0 && (
            <Button
              variant="outline"
              onClick={() => setReloadKey((value) => value + 1)}
            >
              <RefreshCcw className="size-4" /> 重试数据
            </Button>
          )}
          <Button onClick={() => navigate('/upload-question')}>
            <Upload className="size-4" /> 上传错题
          </Button>
        </div>
      </header>

      <section className="metric-grid" aria-label="学习概况">
        {metrics.map(({ label, value, suffix, icon: Icon }) => (
          <article key={label} className="metric-item">
            <div className="metric-icon">
              <Icon className="size-5" />
            </div>
            <p>{label}</p>
            <strong>
              {loading ? '—' : value}
              {!loading && <small>{suffix}</small>}
            </strong>
          </article>
        ))}
      </section>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(300px,.85fr)]">
        <div className="section-block">
          <div className="section-title">
            <div>
              <p>REVIEW SIGNAL</p>
              <h2>近期复习节奏</h2>
            </div>
            <span>近 6 个月</span>
          </div>
          <div className="relative h-64">
            <ChartLineMultiple data={statistics.reviewTrend} />
            {loading && (
              <div className="absolute inset-0 grid place-items-center bg-card/75 text-sm text-muted-foreground backdrop-blur-[1px]">
                正在读取复习趋势…
              </div>
            )}
          </div>
          <div className="activity-strip">
            <span>
              <Upload className="size-4" /> 近期上传{' '}
              {loading ? '—' : (dynamic?.uploadCount ?? 0)} 题
            </span>
            <span>
              <RefreshCcw className="size-4" /> 已复习{' '}
              {loading ? '—' : (dynamic?.recentReviewCount ?? 0)} 题
            </span>
            <span>
              <Clock3 className="size-4" /> 待复习{' '}
              {loading ? '—' : (overdue?.count ?? 0)} 题
            </span>
          </div>
        </div>

        <div className="section-block">
          <div className="section-title">
            <div>
              <p>AI GUIDANCE</p>
              <h2>下一步建议</h2>
            </div>
            <BrainCircuit className="size-5" />
          </div>
          <div className="suggestion-list">
            {suggestions.map((item, index) => (
              <article key={`${item.knowledgePoint}-${index}`}>
                <span>{String(index + 1).padStart(2, '0')}</span>
                <div>
                  <strong>{item.knowledgePoint}</strong>
                  <p>{item.reviewReason}</p>
                </div>
              </article>
            ))}
            {!suggestions.length && (
              <p className="text-sm text-muted-foreground">
                {loading
                  ? '正在分析下一步学习任务…'
                  : '完成学习后，这里会给出针对性建议。'}
              </p>
            )}
          </div>
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="section-block">
          <div className="section-title">
            <div>
              <p>FOCUS</p>
              <h2>薄弱知识点</h2>
            </div>
            <Sparkles className="size-5" />
          </div>
          <div className="focus-list">
            {tricky.map((item) => (
              <button
                key={item.knowledgeId}
                onClick={() => navigate('/knowledge-base')}
              >
                <span>{item.knowledgeName}</span>
                <ArrowRight className="size-4" />
              </button>
            ))}
            {!tricky.length && (
              <p className="text-sm text-muted-foreground">
                {loading ? '正在识别薄弱知识点…' : '暂未识别出集中薄弱点。'}
              </p>
            )}
          </div>
        </div>
        <div className="quick-actions">
          <button onClick={() => navigate('/my-question')}>
            <BookOpenCheck />
            <span>
              <strong>整理错题</strong>
              <small>筛选、归因与复盘</small>
            </span>
            <ArrowRight />
          </button>
          <button onClick={() => navigate('/knowledge-base')}>
            <Target />
            <span>
              <strong>知识图谱</strong>
              <small>查看关联与练习</small>
            </span>
            <ArrowRight />
          </button>
          <button onClick={() => navigate('/ai-explain')}>
            <BrainCircuit />
            <span>
              <strong>AI 解题</strong>
              <small>流式分析题目</small>
            </span>
            <ArrowRight />
          </button>
        </div>
      </section>
    </main>
  );
}
