import request from '@/utils/request';
import { consumeSse } from '@/utils/sse';
import type { ApiResponse } from '@/utils/api';

export interface GenerationData {
  questionId: string;
  content: string;
}

export function generateQuestion(mistakeQuestionId: number) {
  return request.post<ApiResponse<GenerationData>>({
    url: '/api/question/generation',
    params: { mistakeQuestionId },
    timeout: 90000,
  });
}

export function judgeQuestion(
  questionId: string,
  answer: string,
  onMessage: (value: string) => void,
  signal?: AbortSignal,
) {
  return consumeSse({
    url: '/api/question/judge',
    params: { questionId, answer },
    onMessage,
    signal,
  });
}

export function recordQuestion(questionId: string) {
  return request.post<ApiResponse<string>>({
    url: '/api/question/record',
    params: { questionId },
  });
}

export default { generateQuestion, judgeQuestion, recordQuestion };
