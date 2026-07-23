import axios from 'axios';

export interface ApiResponse<T> {
  traceId: string | null;
  code: number;
  info: string;
  data: T;
}

export class ApiError extends Error {
  readonly status?: number;
  readonly code?: number;
  readonly traceId?: string | null;

  constructor(
    message: string,
    status?: number,
    code?: number,
    traceId?: string | null,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

export function isApiResponse<T>(value: unknown): value is ApiResponse<T> {
  return Boolean(
    value &&
      typeof value === 'object' &&
      'code' in value &&
      'info' in value &&
      'data' in value,
  );
}

export function unwrap<T>(value: T | ApiResponse<T>): T {
  if (!isApiResponse<T>(value)) return value;
  if (value.code !== 200) {
    throw new ApiError(
      value.info || '请求失败',
      undefined,
      value.code,
      value.traceId,
    );
  }
  return value.data;
}

export function errorMessage(
  error: unknown,
  fallback = '请求失败，请稍后重试',
) {
  if (error instanceof ApiError) return error.message;
  if (axios.isAxiosError(error)) {
    const body = error.response?.data;
    if (body && typeof body === 'object' && 'info' in body) {
      return String(body.info);
    }
    if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试';
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
