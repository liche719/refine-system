import type { ApiResponse } from '@/utils/api';

export interface LoginData {
  userId: string;
  userName: string;
  accessToken: string;
  refreshToken: string;
}

export type UserResponse = ApiResponse<LoginData>;
export type EmptyResponse = ApiResponse<null>;
