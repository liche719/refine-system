import {
  Send,
  Eraser,
  StopCircle,
  Bot,
  User,
  Sparkles,
  Footprints,
  Lightbulb,
  ScanSearch,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import MarkdownContent from '@/components/common/MarkdownContent';
import { cn } from '@/lib/utils';
import { useAiExplain } from '@/features/AiExplain/AiExplain';

export default function AIExplainPage() {
  const {
    input,
    setInput,
    messages,
    isLoading,
    scrollRef,
    handleStop,
    handleSubmit,
    handleClear,
    handleKeyDown,
  } = useAiExplain();

  return (
    <div className="app-page flex min-h-[calc(100svh-4rem)] flex-col">
      <div className="page-heading shrink-0">
        <div>
          <p className="page-kicker">AI WORKBENCH</p>
          <h1>AI 解题</h1>
          <p>输入题目，获取分步引导，并在同一会话中继续追问。</p>
        </div>
      </div>

      <Card className="min-h-[520px] flex-1 gap-0 overflow-hidden rounded-lg border-border bg-card shadow-none">
        <CardHeader className="flex shrink-0 flex-row items-center justify-between space-y-0 border-b px-5 py-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Sparkles className="size-4 text-primary" />
            <span>智能解析与问答</span>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClear}
            className="h-8 px-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
          >
            <Eraser className="mr-1 size-4" /> 清空对话
          </Button>
        </CardHeader>

        <CardContent className="relative flex-1 overflow-hidden bg-muted/20 p-0">
          <ScrollArea className="h-full px-4 py-6">
            {messages.length === 0 ? (
              <div className="mx-auto flex min-h-[44vh] max-w-3xl flex-col items-center justify-center gap-5 px-4 text-center text-muted-foreground">
                <div className="flex size-14 items-center justify-center rounded-lg border bg-card">
                  <Sparkles className="size-7 text-primary" />
                </div>
                <div>
                  <h2 className="font-medium text-foreground">
                    从题目或困惑开始
                  </h2>
                  <p className="mt-1 text-sm">
                    AI 会结合当前对话连续追问，帮助你自己找到解题路径。
                  </p>
                </div>
                <div className="grid w-full gap-2 sm:grid-cols-3">
                  {[
                    {
                      icon: Footprints,
                      title: '分步提示',
                      prompt:
                        '请不要直接给答案，先用一个问题引导我找到第一步。',
                    },
                    {
                      icon: ScanSearch,
                      title: '检查思路',
                      prompt: '我会写出自己的解题思路，请帮我定位第一处错误。',
                    },
                    {
                      icon: Lightbulb,
                      title: '举一反三',
                      prompt:
                        '请讲清这道题考查的知识点，再给我一道由浅入深的变式题。',
                    },
                  ].map(({ icon: Icon, title, prompt }) => (
                    <button
                      key={title}
                      type="button"
                      className="rounded-lg border bg-card p-3 text-left transition-colors hover:border-primary/50 hover:bg-primary/5"
                      onClick={() => setInput(prompt)}
                    >
                      <Icon className="mb-2 size-4 text-primary" />
                      <strong className="block text-sm text-foreground">
                        {title}
                      </strong>
                      <span className="mt-1 block text-xs leading-5">
                        点击填入引导语
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="mx-auto max-w-3xl space-y-5">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={cn(
                      'flex w-full gap-3',
                      msg.role === 'user' ? 'flex-row-reverse' : 'flex-row',
                    )}
                  >
                    <Avatar className="size-8 shrink-0 border bg-card">
                      <AvatarImage
                        src={msg.role === 'ai' ? '/ai-avatar.png' : ''}
                      />
                      <AvatarFallback
                        className={
                          msg.role === 'ai'
                            ? 'bg-secondary text-secondary-foreground'
                            : 'bg-muted text-foreground'
                        }
                      >
                        {msg.role === 'ai' ? (
                          <Bot size={16} />
                        ) : (
                          <User size={16} />
                        )}
                      </AvatarFallback>
                    </Avatar>

                    <div
                      className={cn(
                        'max-w-[85%] rounded-lg px-4 py-3 text-sm leading-relaxed',
                        msg.role === 'user'
                          ? 'bg-primary text-primary-foreground'
                          : 'border bg-card text-foreground',
                      )}
                    >
                      {msg.role === 'ai' ? (
                        <MarkdownContent
                          content={msg.content}
                          isStreaming={msg.isStreaming}
                        />
                      ) : (
                        <div className="whitespace-pre-wrap break-words">
                          {msg.content}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
                <div ref={scrollRef} />
              </div>
            )}
          </ScrollArea>
        </CardContent>

        <CardFooter className="shrink-0 border-t bg-card p-4">
          <div className="mx-auto flex w-full max-w-3xl items-end gap-3">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="在此输入题目..."
              className="max-h-[150px] min-h-[50px] resize-none border-input bg-background shadow-none transition-colors focus:bg-card focus-visible:ring-primary"
              disabled={isLoading}
            />
            {isLoading ? (
              <Button
                variant="destructive"
                size="icon"
                className="size-[50px] shrink-0 rounded-lg"
                onClick={handleStop}
                aria-label="停止生成"
                title="停止生成"
              >
                <StopCircle className="size-5" />
              </Button>
            ) : (
              <Button
                className="size-[50px] shrink-0 rounded-lg shadow-none"
                size="icon"
                onClick={handleSubmit}
                disabled={!input.trim()}
                aria-label="发送问题"
                title="发送问题"
              >
                <Send className="size-5" />
              </Button>
            )}
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
