import type { AxiosRequestConfig } from 'axios';

export interface ChartData {
  month: string;
  total: number;
  reviewed: number;
}

export interface PieDataItem {
  [key: string]: number;
}

export interface ChartPieSimpleProps {
  data?: PieDataItem[];
}

export interface AxiosLikeError {
  response?: {
    status: number;
  };
  config?: AxiosRequestConfig;
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface CustomAxiosRequestConfig extends AxiosRequestConfig {}
