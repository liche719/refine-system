export interface ToggleErrorReasonData {
  questionId: string;
  reasonName: string;
  otherReasonText?: string;
}

export interface UpdateOtherReasonData {
  questionId: string;
  otherReasonText: string;
}

export interface SubmitStudyNoteData {
  questionId: string;
  studyNote: string;
}

export interface ErrorReasonState {
  userId: string;
  questionId: string;
  isCareless: number;
  isUnfamiliar: number;
  isCalculateErr: number;
  isTimeShortage: number;
  otherReason: number;
  otherReasonText: string | null;
  success: boolean;
  message: string;
}

export interface StudyNoteState {
  userId: string;
  questionId: string;
  studyNote: string | null;
  success: boolean;
  message: string;
}
