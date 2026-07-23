import request from '../../utils/request';
import {
  ToggleErrorReasonData,
  UpdateOtherReasonData,
  SubmitStudyNoteData,
  ErrorReasonState,
  StudyNoteState,
} from './type';
import type { ApiResponse } from '@/utils/api';

export function toggleErrorReason(data: ToggleErrorReasonData) {
  const { reasonName, ...body } = data;
  return request.post<ApiResponse<ErrorReasonState>>({
    url: `/api/v1/mistake-reason/toggle/${reasonName}`,
    data: body,
  });
}

export function updateOtherReason(data: UpdateOtherReasonData) {
  return request.post<ApiResponse<ErrorReasonState>>({
    url: '/api/v1/mistake-reason/update-other-reason',
    data,
  });
}

export function submitStudyNote(data: SubmitStudyNoteData) {
  return request.post<ApiResponse<StudyNoteState>>({
    url: '/api/v1/mistake-reason/study-note/submit',
    data,
  });
}

export function getErrorReasons(questionId: string) {
  return request.get<ApiResponse<ErrorReasonState>>({
    url: '/api/v1/mistake-reason/get',
    params: { questionId },
  });
}

export function getStudyNote(questionId: string) {
  return request.get<ApiResponse<StudyNoteState>>({
    url: '/api/v1/mistake-reason/study-note/get',
    params: { questionId },
  });
}
