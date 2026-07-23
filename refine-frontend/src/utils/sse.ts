import { ApiError, errorMessage } from './api';
import { authStorage } from './auth';
import { refreshAccessToken } from './http';

interface StreamOptions {
  url: string;
  method?: 'POST';
  body?: unknown;
  params?: Record<string, string | number>;
  signal?: AbortSignal;
  onMessage: (value: string) => void;
}

export type SseFrame = { event: string; data: string };

export function parseSseFrame(frame: string): SseFrame | null {
  const lines = frame.split(/\r?\n/);
  const event =
    lines
      .find((line) => line.startsWith('event:'))
      ?.slice(6)
      .trim() || 'message';
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).replace(/^ /, ''))
    .join('\n');
  return data ? { event, data } : null;
}

function endpoint(url: string, params?: Record<string, string | number>) {
  const base = import.meta.env.VITE_BASE_URL || window.location.origin;
  const target = new URL(url, base);
  Object.entries(params || {}).forEach(([key, value]) =>
    target.searchParams.set(key, String(value)),
  );
  return target.toString();
}

async function send(options: StreamOptions, token: string | null) {
  return fetch(endpoint(options.url, options.params), {
    method: options.method || 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(token ? { 'access-token': token } : {}),
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });
}

export async function consumeSse(options: StreamOptions) {
  let response = await send(options, authStorage.accessToken());
  if (response.status === 401) {
    const token = await refreshAccessToken();
    if (token) response = await send(options, token);
  }
  if (!response.ok) {
    let message = `请求失败 (${response.status})`;
    try {
      const body = await response.json();
      if (body?.info) message = body.info;
    } catch {
      // Keep the status-based message for non-JSON failures.
    }
    throw new ApiError(message, response.status);
  }
  if (!response.body) throw new ApiError('服务未返回流式内容');

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  try {
    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
      const events = buffer.split(/\r?\n\r?\n/);
      buffer = events.pop() || '';
      for (const event of events) {
        const frame = parseSseFrame(event);
        if (!frame || frame.data === '[DONE]') continue;
        if (frame.event === 'error') {
          throw new ApiError(frame.data, response.status);
        }
        options.onMessage(frame.data);
      }
      if (done) break;
    }
    const remaining = parseSseFrame(buffer);
    if (remaining && remaining.data !== '[DONE]') {
      if (remaining.event === 'error') {
        throw new ApiError(remaining.data, response.status);
      }
      options.onMessage(remaining.data);
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError')
      throw error;
    throw new ApiError(errorMessage(error, '流式响应读取失败'));
  } finally {
    reader.releaseLock();
  }
}
