import { useState, useRef } from 'react';

interface UseStreamingAIOptions {
  url: string;
  onComplete?: () => void;
  onError?: (error: Error) => void;
}

export function useStreamingAI({
  url,
  onComplete,
  onError,
}: UseStreamingAIOptions) {
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const streamAIResponse = async (question: string) => {
    setIsLoading(true);
    setContent('');

    // 创建中断控制器
    abortControllerRef.current = new AbortController();

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ question }),
        signal: abortControllerRef.current.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Response body is not readable');
      }

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          break;
        }

        const chunk = decoder.decode(value, { stream: true });

        setContent((prev) => prev + chunk);
      }

      onComplete?.();
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('Request aborted');
      } else {
        onError?.(error as Error);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const stopStreaming = () => {
    abortControllerRef.current?.abort();
    setIsLoading(false);
  };

  const reset = () => {
    setContent('');
    setIsLoading(false);
  };

  return {
    content,
    isLoading,
    streamAIResponse,
    stopStreaming,
    reset,
  };
}
