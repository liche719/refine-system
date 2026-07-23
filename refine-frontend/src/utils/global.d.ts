import type { AxiosRequestConfig } from 'axios';

declare type AxiosMethod = 'get' | 'post' | 'delete' | 'put';

declare type AxiosResponseType =
  | 'arraybuffer'
  | 'blob'
  | 'document'
  | 'json'
  | 'text'
  | 'stream';
declare interface AxiosConfig extends AxiosRequestConfig {
  params?: unknown;
  data?: unknown;
  url?: string;
  method?: AxiosMethod;
  headers?: RawAxiosRequestHeaders;
  responseType?: AxiosResponseType;
}
