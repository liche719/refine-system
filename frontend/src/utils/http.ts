import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import {
  addRequest,
  removeRequest,
  cancelRequest,
  cancelAllRequest,
} from './requestQueue';
import { authStorage } from './auth';

const REFRESH_URL = '/api/userAccount/refreshToken';
const API_BASE_URL = import.meta.env.VITE_BASE_URL || '';
let isRefreshing = false;

type RetryConfig = InternalAxiosRequestConfig & { _authRetry?: boolean };
type QueuedRequest = {
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
};

let requestsQueue: QueuedRequest[] = [];

const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 8000,
});

export const refreshAccessToken = async (): Promise<string | null> => {
  try {
    const refreshToken = authStorage.refreshToken();
    if (!refreshToken) return null;

    const { data } = await axios.post(
      `${API_BASE_URL}${REFRESH_URL}`,
      {},
      { headers: { 'refresh-token': refreshToken } },
    );

    if (data.code === 200 && data.data) {
      const { newAccessToken, newRefreshToken } = data.data;
      authStorage.saveTokens(newAccessToken, newRefreshToken);
      return newAccessToken;
    }
    return null;
  } catch (error) {
    console.error('刷新 Token 失败', error);
    return null;
  }
};

axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    removeRequest(config);
    addRequest(config);

    const token = authStorage.accessToken();
    if (token && config.url !== REFRESH_URL && config.headers) {
      config.headers['access-token'] = token;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    removeRequest(response.config);
    return response.data;
  },
  async (error) => {
    const config = error.config as RetryConfig | undefined;
    if (config) removeRequest(config);

    if (
      error.response?.status !== 401 ||
      !config ||
      config.url === REFRESH_URL
    ) {
      return Promise.reject(error);
    }

    // A token issued by refresh was rejected too. Stop here instead of looping.
    if (config._authRetry) {
      authStorage.clear();
      window.location.replace('/login');
      return Promise.reject(error);
    }

    config._authRetry = true;

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        requestsQueue.push({
          resolve: (newToken) => {
            if (config.headers) config.headers['access-token'] = newToken;
            resolve(axiosInstance(config));
          },
          reject,
        });
      });
    }

    isRefreshing = true;
    try {
      const newToken = await refreshAccessToken();
      if (!newToken) throw new Error('刷新 Token 失败');

      requestsQueue.forEach((request) => request.resolve(newToken));
      requestsQueue = [];
      if (config.headers) config.headers['access-token'] = newToken;
      return axiosInstance(config);
    } catch (refreshError) {
      requestsQueue.forEach((request) => request.reject(refreshError));
      requestsQueue = [];
      authStorage.clear();
      window.location.replace('/login');
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

export const service = {
  request: <T = unknown>(config: AxiosRequestConfig): Promise<T> =>
    axiosInstance.request(config),
  cancelRequest,
  cancelAllRequest,
};
