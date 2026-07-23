import request from '../../utils/request';
import type { ApiResponse } from '@/utils/api';
import { unwrap } from '@/utils/api';
import type {
  KeyPointSuggestion,
  OverviewResponse,
  OverdueData,
  StudyDynamic,
  TrickyKnowledge,
} from './type';

export async function GetOverview(): Promise<OverviewResponse> {
  const res = await request.get<OverviewResponse>({
    url: '/api/v1/overview/get_overview',
  });
  return res;
}

export async function GetOverDue(): Promise<OverdueData> {
  const res = await request.get<ApiResponse<OverdueData>>({
    url: '/api/v1/feedback/review/overdue-count',
  });
  return unwrap(res);
}

export async function GetTrickyKnowledge(): Promise<TrickyKnowledge[]> {
  const res = await request.get<ApiResponse<TrickyKnowledge[]>>({
    url: '/api/v1/feedback/review/tricky_knowledge',
  });
  return unwrap(res);
}

export async function GetKeyPoint(): Promise<KeyPointSuggestion[]> {
  return request.get<KeyPointSuggestion[]>({
    url: '/api/v1/ai_suggession/get_key_point',
  });
}

export async function GetStudyDynamic(): Promise<StudyDynamic> {
  return request.get<StudyDynamic>({
    url: '/api/v1/overview/get_study_dynamic',
  });
}
