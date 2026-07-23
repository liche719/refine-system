export interface OverviewResponse {
  questionsNum: number;
  reviewRate: number;
  hardQuestions: number;
  studyTime: number;
}

export interface StudyDynamic {
  uploadCount: number;
  recentReviewCount: number;
}

export interface OverdueData {
  count: number;
  description: string;
}

export interface TrickyKnowledge {
  knowledgeId: number;
  knowledgeName: string;
}

export interface KeyPointSuggestion {
  knowledgePoint: string;
  reviewReason: string;
}
