import { useState, useRef, useEffect } from 'react';
import { Plus, Send, StopCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/utils';
import { useAiExplain } from '@/features/AiExplain/AiExplain';
import type { QuestionContext } from '@/features/AiExplain/AiExplain';
import MarkdownContent from '@/components/common/MarkdownContent';

// StreamingText 组件
const StreamingText = ({
  content,
  isStreaming,
  markdown,
}: {
  content: string;
  isStreaming: boolean;
  markdown: boolean;
}) => {
  if (markdown) {
    return <MarkdownContent content={content} isStreaming={isStreaming} />;
  }
  return (
    <div className="whitespace-pre-wrap leading-relaxed break-words text-sm">
      {content}

      {isStreaming && (
        <span className="inline-block w-1.5 h-4 ml-1 align-middle bg-current animate-pulse rounded-sm" />
      )}
    </div>
  );
};
//
//  主组件
interface AiChatPanelProps {
  className?: string;
  mode?: 'standard' | 'embedded';
  questionContext?: QuestionContext;
}

export function AiChatPanel({
  className,
  mode = 'standard',
  questionContext,
}: AiChatPanelProps) {
  const isEmbedded = mode === 'embedded';
  const scope = questionContext
    ? `embedded-question:${questionContext.questionId}`
    : isEmbedded
      ? 'embedded-chat'
      : 'ai-chat';
  const hookResult = useAiExplain(scope, questionContext);

  const [isInputExpanded, setIsInputExpanded] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  if (!hookResult) return null;

  const {
    input,
    setInput,
    messages,
    isLoading,
    scrollRef,
    handleStop,
    handleSubmit,
    handleKeyDown,
  } = hookResult;

  useEffect(() => {
    if (isInputExpanded && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isInputExpanded]);

  return (
    <Card className={cn('h-full gap-0 shadow-sm', className)}>
      {/* 头部标题 */}
      <CardHeader>
        <CardTitle className="text-lg">
          {isEmbedded ? 'AI问答区' : '智能解析与问答'}
        </CardTitle>
      </CardHeader>

      {/* 内容主体 */}
      <CardContent className="flex-1 p-4 overflow-hidden relative flex flex-col">
        {/* 消息列表容器 (灰色背景) */}
        <div
          className={cn(
            'relative flex flex-1 flex-col overflow-hidden rounded-lg pt-3',
            isEmbedded ? 'bg-background' : 'bg-slate-50',
          )}
        >
          <div className="flex-1 h-full overflow-y-auto p-3 space-y-4 scroll-smooth pt-0">
            {/* 1. 默认欢迎语 (仅嵌入模式且无消息时显示) */}
            {isEmbedded && messages.length === 0 && (
              <div className="flex justify-start animate-in fade-in slide-in-from-left-2 duration-300">
                <div className="max-w-[90%] rounded-lg bg-secondary px-4 py-3 text-sm leading-relaxed text-secondary-foreground">
                  欢迎使用智能错题提问系统，请您根据什么问题提问
                </div>
              </div>
            )}

            {/* 2. 消息遍历 */}
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={cn(
                  'flex w-full px-1',
                  msg.role === 'user' ? 'justify-end' : 'justify-start',
                )}
              >
                <div
                  className={cn(
                    'max-w-[90%] rounded-lg px-4 py-2.5',
                    msg.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'border bg-secondary text-secondary-foreground',
                  )}
                >
                  {/* StreamingText 组件 */}
                  <StreamingText
                    content={msg.content}
                    isStreaming={!!msg.isStreaming}
                    markdown={msg.role === 'ai'}
                  />
                </div>
              </div>
            ))}

            <div ref={scrollRef} />
          </div>
        </div>

        <div className="mt-4 shrink-0 h-[50px] relative">
          {/* 状态 1:嵌入时的引导按钮 */}
          {!isInputExpanded && isEmbedded && (
            <Button
              className="flex h-full w-full cursor-pointer items-center justify-center gap-2 rounded-lg bg-primary text-sm font-medium text-primary-foreground shadow-none transition-colors hover:bg-primary/90"
              onClick={() => setIsInputExpanded(true)}
            >
              <Plus className="w-4 h-4" />
              请用自然语言提问
            </Button>
          )}

          {/* 状态 2: 输入框 + 发送按钮 */}
          {(isInputExpanded || !isEmbedded) && (
            <div className="flex gap-2 items-end h-full animate-in fade-in slide-in-from-bottom-2 duration-300">
              <div className="flex-1 relative h-full">
                <Textarea
                  ref={inputRef}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="请输入问题..."
                  className="h-full min-h-[50px] w-full resize-none rounded-lg border border-input bg-card py-3 pr-2 text-sm shadow-none focus:bg-card focus-visible:ring-primary"
                  disabled={isLoading}
                />
              </div>

              {/* 交互按钮 (发送 / 停止) */}
              <Button
                size="icon"
                className={cn(
                  'size-[50px] shrink-0 rounded-lg shadow-none transition-colors',
                  isLoading
                    ? 'border border-destructive/30 bg-destructive/10 text-destructive hover:bg-destructive/15'
                    : 'bg-primary text-primary-foreground hover:bg-primary/90',
                )}
                onClick={isLoading ? handleStop : handleSubmit}
                disabled={!isLoading && !input.trim()}
                aria-label={isLoading ? '停止生成' : '发送问题'}
                title={isLoading ? '停止生成' : '发送问题'}
              >
                {isLoading ? (
                  <div className="relative flex items-center justify-center group">
                    <Loader2 className="w-5 h-5 animate-spin group-hover:opacity-0 transition-opacity duration-200" />
                    <StopCircle className="w-5 h-5 absolute opacity-0 group-hover:opacity-100 transition-opacity duration-200 scale-90 group-hover:scale-100" />
                  </div>
                ) : (
                  <Send className="w-5 h-5 ml-0.5" />
                )}
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
