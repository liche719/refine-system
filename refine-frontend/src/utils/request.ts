import { service } from './http';
import type { CustomAxiosRequestConfig } from './type';

export default {
  get: <T>(option: CustomAxiosRequestConfig) => {
    return service.request<T>({ method: 'get', ...option });
  },
  post: <T>(option: CustomAxiosRequestConfig) => {
    return service.request<T>({ method: 'post', ...option });
  },
  delete: <T>(option: CustomAxiosRequestConfig) => {
    return service.request<T>({ method: 'delete', ...option });
  },
  put: <T>(option: CustomAxiosRequestConfig) => {
    return service.request<T>({ method: 'put', ...option });
  },
  cancelRequest: (url: string | string[]) => {
    return service.cancelRequest(url);
  },
  cancelAllRequest: () => {
    return service.cancelAllRequest();
  },
};
