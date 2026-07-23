import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, BookMarked, Save, Square } from 'lucide-react';
import { toast } from 'sonner';

import { AiChatPanel } from '@/components/business/AiChatPanel';
import EmptyState from '@/components/common/EmptyState';
import MarkdownContent from '@/components/common/MarkdownContent';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Textarea } from '@/components/ui/textarea';
import {
  getErrorReasons,
  getStudyNote,
  submitStudyNote,
  toggleErrorReason,
  updateOtherReason,
} from '@/services/errorReason/errorReason';
import type { ExtractFirstResponse } from '@/services/ocr/type';
import { solveStream } from '@/services/apis/aiapi';
import { errorMessage, unwrap } from '@/utils/api';
import { getQuestionDetail } from '@/services/myQuestion/myQuestion';

const REASONS = [
  { key: 'isCareless', field: 'isCareless', label: '粗心马虎' },
  { key: 'isUnfamiliar', field: 'isUnfamiliar', label: '知识点不熟悉' },
  { key: 'isCalculateErr', field: 'isCalculateErr', label: '计算错误' },
  { key: 'isTimeShortage', field: 'isTimeShortage', label: '时间不够' },
  { key: 'otherReason', field: 'otherReason', label: '其他' },
] as const;

type ReasonValues = Record<(typeof REASONS)[number]['field'], number>;
const EMPTY_REASONS: ReasonValues = {
  isCareless: 0,
  isUnfamiliar: 0,
  isCalculateErr: 0,
  isTimeShortage: 0,
  otherReason: 0,
};

export default function QuestionDetailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const state = useLocation().state as {
    result?: ExtractFirstResponse;
    returnTo?: string;
  } | null;
  const stateResult = state?.result?.data || null;
  const questionId = searchParams.get('questionId');
  const [result, setResult] = useState<ExtractFirstResponse['data'] | null>(
    stateResult,
  );
  const [detailLoading, setDetailLoading] = useState(
    !stateResult && Boolean(questionId),
  );
  const returnTo =
    state?.returnTo ||
    (searchParams.get('from') === 'my-question'
      ? '/my-question'
      : '/upload-question');
  const returnLabel = returnTo === '/my-question' ? '返回我的错题' : '返回上传';
  const [solution, setSolution] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [reasons, setReasons] = useState<ReasonValues>(EMPTY_REASONS);
  const [otherReason, setOtherReason] = useState('');
  const [note, setNote] = useState('');
  const controller = useRef<AbortController | null>(null);

  useEffect(() => {
    if (stateResult) {
      setResult(stateResult);
      setDetailLoading(false);
      return;
    }
    if (!questionId) {
      setResult(null);
      setDetailLoading(false);
      return;
    }

    let active = true;
    setDetailLoading(true);
    getQuestionDetail(questionId)
      .then((response) => {
        if (active) setResult(unwrap(response));
      })
      .catch((error) => {
        if (active) {
          setResult(null);
          toast.error(errorMessage(error, '错题详情加载失败'));
        }
      })
      .finally(() => {
        if (active) setDetailLoading(false);
      });
    return () => {
      active = false;
    };
  }, [questionId, stateResult]);

  useEffect(() => {
    if (!result) return;
    if (returnTo === '/my-question') {
      Promise.all([
        getErrorReasons(result.questionId),
        getStudyNote(result.questionId),
      ])
        .then(([reasonResponse, noteResponse]) => {
          const value = unwrap(reasonResponse);
          setReasons({
            isCareless: value.isCareless,
            isUnfamiliar: value.isUnfamiliar,
            isCalculateErr: value.isCalculateErr,
            isTimeShortage: value.isTimeShortage,
            otherReason: value.otherReason,
          });
          setOtherReason(value.otherReasonText || '');
          setNote(unwrap(noteResponse).studyNote || '');
        })
        .catch((error) => toast.error(errorMessage(error, '错题信息加载失败')));
    }

    const nextController = new AbortController();
    controller.current = nextController;
    setStreaming(true);
    setSolution('');
    solveStream({
      question: result.questionText,
      signal: nextController.signal,
      onMessage: (value) => setSolution((current) => current + value),
      onError: (error) => toast.error(errorMessage(error, 'AI 解析失败')),
    }).finally(() => setStreaming(false));
    return () => nextController.abort();
  }, [result?.questionId, returnTo]);

  if (detailLoading) {
    return (
      <main className="app-page">
        <p className="text-center text-sm text-muted-foreground">
          正在加载错题详情...
        </p>
      </main>
    );
  }

  if (!result) {
    return (
      <main className="app-page">
        <EmptyState
          icon={BookMarked}
          title="没有可查看的题目"
          description="请先上传文件并完成识别。"
        />
        <div className="mt-4 text-center">
          <Button onClick={() => navigate(returnTo)}>{returnLabel}</Button>
        </div>
      </main>
    );
  }

  const toggle = async (key: string) => {
    try {
      const value = unwrap(
        await toggleErrorReason({
          questionId: result.questionId,
          reasonName: key,
        }),
      );
      setReasons({
        isCareless: value.isCareless,
        isUnfamiliar: value.isUnfamiliar,
        isCalculateErr: value.isCalculateErr,
        isTimeShortage: value.isTimeShortage,
        otherReason: value.otherReason,
      });
      setOtherReason(value.otherReasonText || '');
    } catch (error) {
      toast.error(errorMessage(error, '错因更新失败'));
    }
  };

  const saveOther = async () => {
    if (!otherReason.trim()) return toast.info('请输入具体原因');
    try {
      unwrap(
        await updateOtherReason({
          questionId: result.questionId,
          otherReasonText: otherReason.trim(),
        }),
      );
      toast.success('其他错因已保存');
    } catch (error) {
      toast.error(errorMessage(error, '其他错因保存失败'));
    }
  };

  const saveStudyNote = async () => {
    if (!note.trim()) return toast.info('请输入笔记内容');
    try {
      unwrap(
        await submitStudyNote({
          questionId: result.questionId,
          studyNote: note.trim(),
        }),
      );
      toast.success('复习笔记已保存');
    } catch (error) {
      toast.error(errorMessage(error, '笔记保存失败'));
    }
  };

  return (
    <main className="app-page question-detail-page">
      <header className="page-heading compact-heading">
        <div>
          <button className="back-link" onClick={() => navigate(returnTo)}>
            <ArrowLeft className="size-4" /> {returnLabel}
          </button>
          <h1>题目解析</h1>
          <p>题目已保存至错题库，可继续归因和记录笔记。</p>
        </div>
      </header>
      <div className="question-detail-layout">
        <section className="solution-column">
          <article className="question-source">
            <p className="page-kicker">ORIGINAL QUESTION</p>
            <h2>原题</h2>
            <div>{result.questionText}</div>
          </article>
          <article className="solution-output">
            <div className="section-title">
              <div>
                <p>AI SOLUTION</p>
                <h2>解题思路</h2>
              </div>
              {streaming && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => controller.current?.abort()}
                >
                  <Square className="size-3 fill-current" /> 停止
                </Button>
              )}
            </div>
            {solution ? (
              <MarkdownContent content={solution} isStreaming={streaming} />
            ) : (
              <p className="text-sm text-muted-foreground">
                {streaming ? '正在生成解答...' : '暂无解答'}
              </p>
            )}
          </article>
        </section>

        <aside className="reflection-column">
          <section>
            <div className="section-title">
              <div>
                <p>REASON</p>
                <h2>错因分析</h2>
              </div>
            </div>
            <div className="reason-list">
              {REASONS.map((reason) => (
                <label key={reason.key}>
                  <Checkbox
                    checked={reasons[reason.field] === 1}
                    onCheckedChange={() => toggle(reason.key)}
                  />
                  <span>{reason.label}</span>
                </label>
              ))}
            </div>
            {reasons.otherReason === 1 && (
              <div className="mt-3 space-y-2">
                <Textarea
                  value={otherReason}
                  onChange={(event) => setOtherReason(event.target.value)}
                  placeholder="具体是什么原因？"
                />
                <Button size="sm" variant="outline" onClick={saveOther}>
                  保存原因
                </Button>
              </div>
            )}
          </section>
          <section>
            <div className="section-title">
              <div>
                <p>MEMO</p>
                <h2>复习笔记</h2>
              </div>
            </div>
            <Textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="记录关键步骤、公式或提醒"
              className="min-h-36"
            />
            <Button className="mt-3 w-full" onClick={saveStudyNote}>
              <Save className="size-4" /> 保存笔记
            </Button>
          </section>
        </aside>

        <aside className="question-chat">
          <AiChatPanel
            mode="embedded"
            questionContext={{
              questionId: result.questionId,
              questionText: result.questionText,
            }}
          />
        </aside>
      </div>
    </main>
  );
}
