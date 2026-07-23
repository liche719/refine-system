import request from '@/utils/request';
import { consumeSse } from '@/utils/sse';

const API_VERSION = import.meta.env.VITE_API_VERSION || 'v1';

export interface SolveStreamOptions {
  question: string;
  onMessage: (text: string) => void;
  onError: (error: unknown) => void;
  signal?: AbortSignal;
}

export async function solveStream(options: SolveStreamOptions) {
  try {
    await consumeSse({
      url: `/api/${API_VERSION}/solve/stream`,
      body: { questionContext: options.question },
      signal: options.signal,
      onMessage: options.onMessage,
    });
  } catch (error) {
    if (!(error instanceof DOMException && error.name === 'AbortError')) {
      options.onError(error);
    }
  }
}

export function sendMessage(data: {
  conversationId: string;
  message: string;
  onMessage: (value: string) => void;
  signal?: AbortSignal;
}) {
  return consumeSse({
    url: `/api/${API_VERSION}/conversation/send-message`,
    body: { conversationId: data.conversationId, message: data.message },
    onMessage: data.onMessage,
    signal: data.signal,
  });
}

export function solveWithContext(data: {
  questionId: string | number;
  userQuestion: string;
  questionContent?: string;
  onMessage: (value: string) => void;
  signal?: AbortSignal;
}) {
  return consumeSse({
    url: `/api/${API_VERSION}/conversation/solve-with-context`,
    body: {
      questionId: String(data.questionId),
      userQuestion: data.userQuestion,
      questionContent: data.questionContent,
    },
    onMessage: data.onMessage,
    signal: data.signal,
  });
}

export function deleteConversation(conversationId: string) {
  return request.delete<void>({
    url: `/api/${API_VERSION}/conversation/delete/${encodeURIComponent(conversationId)}`,
  });
}

export default {
  solveStream,
  sendMessage,
  solveWithContext,
  deleteConversation,
};
