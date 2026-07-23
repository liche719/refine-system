import request from '../../utils/request';
import {
  QuestionListParams,
  QuestionListResponse,
  StatisticsResponse,
} from './type';
import type { ApiResponse } from '@/utils/api';

export async function getQuestionList(params: QuestionListParams) {
  return request.get<QuestionListResponse>({
    url: '/api/v1/feedback/review/list',
    params,
  });
}

export function getQuestionDetail(questionId: string) {
  return request.get<
    ApiResponse<{
      questionId: string;
      questionText: string;
      subject?: string | null;
    }>
  >({
    url: '/api/v1/feedback/review/detail',
    params: { questionId },
  });
}

export function deleteQuestion(questionIds: number[]) {
  return request.delete({
    url: '/api/v1/feedback/review/deleteBatch',
    params: { questionIds: questionIds.join(',') },
  });
}

export async function getStatistics() {
  return request.get<StatisticsResponse>({
    url: '/api/v1/feedback/review/statistics',
  });
}
