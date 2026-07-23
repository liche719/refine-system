import axios, { AxiosRequestConfig, Canceler } from 'axios';

type QueuedConfig = AxiosRequestConfig & { __requestQueueId?: string };
type PendingRequest = { cancel: Canceler; requestId: string };

const pendingMap = new Map<string, PendingRequest>();

const getRequestKey = (config: AxiosRequestConfig) => {
  const { method, url, params, data } = config;
  return [
    method,
    url,
    params ? JSON.stringify(params) : '',
    data ? JSON.stringify(data) : '',
  ].join('&');
};

export const removeRequest = (config: AxiosRequestConfig) => {
  const key = getRequestKey(config);
  const requestId = (config as QueuedConfig).__requestQueueId;
  const pending = pendingMap.get(key);
  if (pending && pending.requestId === requestId) {
    pendingMap.delete(key);
  }
};

export const addRequest = (config: AxiosRequestConfig) => {
  const key = getRequestKey(config);
  const existing = pendingMap.get(key);
  if (existing) {
    existing.cancel(`取消重复请求: ${config.url}`);
    pendingMap.delete(key);
  }

  const requestId = crypto.randomUUID();
  (config as QueuedConfig).__requestQueueId = requestId;
  config.cancelToken = new axios.CancelToken((cancel) => {
    pendingMap.set(key, { cancel, requestId });
  });
};

export const cancelRequest = (url: string | string[]) => {
  const urlList = Array.isArray(url) ? url : [url];
  for (const [key, pending] of pendingMap) {
    if (urlList.some((item) => key.includes(item))) {
      pending.cancel(`取消请求: ${url}`);
      pendingMap.delete(key);
    }
  }
};

export const cancelAllRequest = () => {
  for (const pending of pendingMap.values()) {
    pending.cancel('取消所有请求');
  }
  pendingMap.clear();
};
