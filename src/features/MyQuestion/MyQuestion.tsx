import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BookOpenCheck,
  CheckSquare2,
  Search,
  Trash2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import EmptyState from '@/components/common/EmptyState';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { ChartLineMultiple } from '@/components/layout/ChartLineMultiple';
import { ChartPieDonut } from '@/components/layout/ChartPieDonut';
import { ChartPieSimple } from '@/components/layout/ChartPieSimple';
import {
  deleteQuestion,
  getQuestionList,
  getStatistics,
} from '@/services/myQuestion/myQuestion';
import type { AnalysisData, QuestionItem } from '@/services/myQuestion/type';
import { errorMessage } from '@/utils/api';

const EMPTY_STATS: AnalysisData = {
  subjectDistribution: [],
  knowledgeDistribution: [],
  reviewTrend: [],
};

function reasonLabel(item: QuestionItem) {
  const reasons = [
    item.isCareless === 1 && '粗心马虎',
    item.isUnfamiliar === 1 && '知识点不熟悉',
    item.isCalculateErr === 1 && '计算错误',
    item.isTimeShortage === 1 && '时间不够',
    item.otherReasonFlag === 1 && (item.otherReason || '其他'),
  ].filter(Boolean);
  return reasons.length ? reasons.join('、') : '待归因';
}

export default function MyQuestionPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [subject, setSubject] = useState('ALL');
  const [errorType, setErrorType] = useState('ALL');
  const [timeRange, setTimeRange] = useState('THIS_MONTH');
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<QuestionItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selected, setSelected] = useState<number[]>([]);
  const [statistics, setStatistics] = useState(EMPTY_STATS);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [result, stats] = await Promise.all([
        getQuestionList({
          keyword: keyword.trim() || undefined,
          subject: subject === 'ALL' ? undefined : subject,
          errorType: errorType === 'ALL' ? undefined : errorType,
          timeRange,
          page,
          size: 8,
        }),
        getStatistics(),
      ]);
      setItems(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
      setStatistics(stats);
      setSelected([]);
    } catch (error) {
      toast.error(errorMessage(error, '错题加载失败'));
    } finally {
      setLoading(false);
    }
  }, [errorType, keyword, page, subject, timeRange]);

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
  }, [load]);

  const allSelected = useMemo(
    () => items.length > 0 && items.every((item) => selected.includes(item.id)),
    [items, selected],
  );

  const removeSelected = async () => {
    if (!selected.length) return;
    try {
      await deleteQuestion(selected);
      toast.success(`已删除 ${selected.length} 道错题`);
      await load();
    } catch (error) {
      toast.error(errorMessage(error, '删除失败'));
    }
  };

  const reviewQuestion = (item: QuestionItem) => {
    navigate(
      `/upload-question/question-detail?questionId=${encodeURIComponent(item.questionId)}&from=my-question`,
      {
        state: {
          result: {
            traceId: '',
            code: 200,
            info: 'loaded-from-mistake-library',
            data: {
              questionId: item.questionId,
              questionText: item.questionContent,
            },
          },
          returnTo: '/my-question',
        },
      },
    );
  };

  return (
    <main className="app-page">
      <header className="page-heading">
        <div>
          <p className="page-kicker">MISTAKE LIBRARY</p>
          <h1>我的错题</h1>
          <p>筛选、复盘并整理已收录的题目。</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <BookOpenCheck className="size-4" /> 共 {totalElements} 题
        </div>
      </header>

      <section className="filter-bar" aria-label="错题筛选">
        <div className="relative min-w-0 flex-1 sm:min-w-64">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(0);
            }}
            placeholder="搜索题目内容"
            className="pl-9"
          />
        </div>
        <FilterSelect
          value={subject}
          onChange={(value) => {
            setSubject(value);
            setPage(0);
          }}
          placeholder="学科"
          items={[
            'ALL:全部学科',
            '数学:数学',
            '物理:物理',
            '化学:化学',
            '英语:英语',
          ]}
        />
        <FilterSelect
          value={errorType}
          onChange={(value) => {
            setErrorType(value);
            setPage(0);
          }}
          placeholder="错因"
          items={[
            'ALL:全部错因',
            'careless:粗心马虎',
            'unfamiliar:知识点不熟悉',
            'calculationError:计算错误',
            'timeShortage:时间不够',
            'other:其他',
          ]}
        />
        <FilterSelect
          value={timeRange}
          onChange={(value) => {
            setTimeRange(value);
            setPage(0);
          }}
          placeholder="时间"
          items={[
            'THIS_WEEK:本周',
            'THIS_MONTH:本月',
            'THIS_QUARTER:本季度',
            'THIS_YEAR:本年',
          ]}
        />
      </section>

      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 xl:grid-cols-[minmax(0,1fr)_310px]">
        <section className="min-w-0">
          <div className="mb-3 flex min-h-9 items-center justify-between gap-3">
            <label className="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground">
              <Checkbox
                checked={allSelected}
                onCheckedChange={(checked) =>
                  setSelected(checked ? items.map((item) => item.id) : [])
                }
              />
              全选本页
            </label>
            {selected.length > 0 && (
              <Button variant="destructive" size="sm" onClick={removeSelected}>
                <Trash2 className="size-4" /> 删除 {selected.length} 题
              </Button>
            )}
          </div>

          {loading ? (
            <LoadingSpinner text="正在读取错题" />
          ) : items.length === 0 ? (
            <EmptyState
              icon={CheckSquare2}
              title="没有匹配的错题"
              description="调整筛选条件，或先上传一道题目。"
            />
          ) : (
            <div className="grid gap-3 md:grid-cols-2">
              {items.map((item) => (
                <Card key={item.id} className="question-card">
                  <CardContent className="p-4">
                    <div className="flex items-start gap-3">
                      <Checkbox
                        aria-label={`选择题目 ${item.id}`}
                        checked={selected.includes(item.id)}
                        onCheckedChange={(checked) =>
                          setSelected((current) =>
                            checked
                              ? [...current, item.id]
                              : current.filter((id) => id !== item.id),
                          )
                        }
                      />
                      <div className="min-w-0 flex-1">
                        <p className="line-clamp-3 min-h-16 text-sm leading-6">
                          {item.questionContent}
                        </p>
                        <div className="mt-4 flex flex-wrap items-center gap-2">
                          <Badge variant="outline">
                            {item.subject || '未分类'}
                          </Badge>
                          <Badge
                            variant="secondary"
                            className="max-w-full truncate"
                          >
                            {reasonLabel(item)}
                          </Badge>
                          <time className="ml-auto text-xs text-muted-foreground">
                            {item.updateTime?.slice(0, 10)}
                          </time>
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="mt-3 w-full justify-between border border-border/70 px-3"
                          onClick={() => reviewQuestion(item)}
                        >
                          查看解析与复盘
                          <ArrowRight className="size-4" />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          <div className="mt-5 flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0 || loading}
              onClick={() => setPage((value) => value - 1)}
            >
              上一页
            </Button>
            <span className="text-sm text-muted-foreground">
              第 {totalPages ? page + 1 : 0} / {totalPages} 页
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages || loading}
              onClick={() => setPage((value) => value + 1)}
            >
              下一页
            </Button>
          </div>
        </section>

        <aside className="analytics-panel">
          <h2>学习分布</h2>
          <div>
            <p>学科占比</p>
            <ChartPieDonut data={statistics.subjectDistribution} />
          </div>
          <div>
            <p>知识点错误</p>
            <ChartPieSimple data={statistics.knowledgeDistribution} />
          </div>
          <div>
            <p>复习趋势</p>
            <div className="h-48">
              <ChartLineMultiple data={statistics.reviewTrend} />
            </div>
          </div>
        </aside>
      </div>
    </main>
  );
}

function FilterSelect({
  value,
  onChange,
  placeholder,
  items,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  items: string[];
}) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-full sm:w-40">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {items.map((item) => {
          const [key, label] = item.split(':');
          return (
            <SelectItem key={key} value={key}>
              {label}
            </SelectItem>
          );
        })}
      </SelectContent>
    </Select>
  );
}
