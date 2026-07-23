import { useState, useRef, useEffect } from 'react';
import {
  deleteConversation,
  sendMessage,
  solveWithContext,
} from '@/services/apis/aiapi';

export interface Message {
  id: string;
  role: 'user' | 'ai';
  content: string;
  isStreaming?: boolean;
}
// 1. 生成唯一ID的工具函数
const generateId = () => {
  return Date.now().toString() + Math.random().toString(36).slice(2, 9);
};

const conversationStorageKey = (scope: string) =>
  `refine.ai.conversation.${scope}`;

const createConversationId = () =>
  globalThis.crypto?.randomUUID?.() || generateId();

const loadConversationId = (scope: string) => {
  const key = conversationStorageKey(scope);
  const existing = localStorage.getItem(key);
  if (existing) return existing;
  const created = createConversationId();
  localStorage.setItem(key, created);
  return created;
};

export interface QuestionContext {
  questionId: string;
  questionText: string;
}

export const useAiExplain = (
  scope = 'ai-explain',
  questionContext?: QuestionContext,
) => {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [conversationId, setConversationId] = useState(() =>
    loadConversationId(scope),
  );

  const scrollRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setConversationId(loadConversationId(scope));
    setMessages([]);
    setInput('');
  }, [scope]);

  // 消息列表更新时自动滚动
  useEffect(() => {
    if (messages.length > 0 && scrollRef.current) {
      const scrollContainer = scrollRef.current.closest<HTMLElement>(
        '[data-radix-scroll-area-viewport], .overflow-y-auto',
      );
      scrollContainer?.scrollTo({
        top: scrollContainer.scrollHeight,
        behavior: 'smooth',
      });
    }
  }, [messages, isLoading]);

  /**
   * 停止生成
   */
  const handleStop = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsLoading(false);

    setMessages((prev) => {
      const newArr = [...prev];
      if (newArr.length > 0) {
        const lastMsg = newArr[newArr.length - 1];
        if (lastMsg.role === 'ai' && lastMsg.isStreaming) {
          newArr[newArr.length - 1] = { ...lastMsg, isStreaming: false };
        }
      }
      return newArr;
    });
  };

  /**
   * 提交问题
   */
  const handleSubmit = async () => {
    if (!input.trim() || isLoading) return;

    const userText = input.trim();
    setInput('');
    setIsLoading(true);

    // 2. 使用 generateId 生成绝对唯一 ID
    const userMsgId = generateId();
    setMessages((prev) => [
      ...prev,
      { id: userMsgId, role: 'user', content: userText },
    ]);

    const aiMsgId = generateId();
    setMessages((prev) => [
      ...prev,
      { id: aiMsgId, role: 'ai', content: '', isStreaming: true },
    ]);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const onMessage = (chunk: string) => {
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === aiMsgId ? { ...msg, content: msg.content + chunk } : msg,
          ),
        );
      };
      if (questionContext) {
        await solveWithContext({
          questionId: questionContext.questionId,
          questionContent: questionContext.questionText,
          userQuestion: userText,
          onMessage,
          signal: controller.signal,
        });
      } else {
        await sendMessage({
          conversationId,
          message: userText,
          onMessage,
          signal: controller.signal,
        });
      }
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        console.error('Stream Error:', error);
        setMessages((prev) =>
          prev.map((msg) =>
            msg.id === aiMsgId
              ? {
                  ...msg,
                  content: `${msg.content}\n\n> 网络请求中断或出错，请重试。`,
                  isStreaming: false,
                }
              : msg,
          ),
        );
      }
    } finally {
      setIsLoading(false);
      abortControllerRef.current = null;

      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === aiMsgId ? { ...msg, isStreaming: false } : msg,
        ),
      );
    }
  };

  const handleClear = () => {
    handleStop();
    const previousConversationId = conversationId;
    const nextConversationId = createConversationId();
    localStorage.setItem(conversationStorageKey(scope), nextConversationId);
    setConversationId(nextConversationId);
    setMessages([]);
    setInput('');
    void deleteConversation(previousConversationId).catch((error) =>
      console.error('Conversation cleanup failed:', error),
    );
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return {
    input,
    setInput,
    messages,
    isLoading,
    scrollRef,
    handleStop,
    handleSubmit,
    handleClear,
    handleKeyDown,
  };
};
