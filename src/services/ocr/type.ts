export interface ExtractFirstResponse {
  traceId: string;
  code: number;
  info: string;
  data: {
    questionText: string;
    questionId: string;
    subject?: string | null;
    knowledgePoint?: string | null;
  };
}
