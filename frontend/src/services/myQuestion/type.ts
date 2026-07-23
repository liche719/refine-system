export interface QuestionListParams {
  keyword?: string;
  subject?: string;
  errorType?: string;
  timeRange?: string;
  page?: number;
  size?: number;
}

export interface QuestionItem {
  id: number;
  questionId: string;
  questionContent: string;
  isCareless: number;
  isUnfamiliar: number;
  isCalculateErr: number;
  isTimeShortage: number;
  otherReasonFlag: number;
  otherReason: string | null;
  studyNote: string | null;
  updateTime: string;
  subject: string;
}

// 分页信息
export interface PageInfo {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

// data 部分
export interface QuestionListData {
  content: QuestionItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 最外层 API 响应
export type QuestionListResponse = QuestionListData;

// 主数据结构接口
export type StatisticsResponse = AnalysisData;

export interface AnalysisData {
  subjectDistribution: DistributionItem[];
  knowledgeDistribution: DistributionItem[];
  reviewTrend: ReviewTrendItem[];
}

// 基础数据项接口
export interface DistributionItem {
  [key: string]: number;
}

// 复习趋势数据项接口
export interface ReviewTrendItem {
  month: string;
  total: number;
  reviewed: number;
  completionRate: number;
}
