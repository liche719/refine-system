// @vitest-environment jsdom

import type { AxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it } from 'vitest';

import {
  addRequest,
  cancelAllRequest,
  cancelRequest,
  removeRequest,
} from './requestQueue';

afterEach(cancelAllRequest);

describe('request queue', () => {
  it('does not let an older duplicate response remove the newer request', async () => {
    const first: AxiosRequestConfig = { method: 'get', url: '/api/example' };
    const second: AxiosRequestConfig = { method: 'get', url: '/api/example' };

    addRequest(first);
    addRequest(second);
    removeRequest(first);
    cancelRequest('/api/example');

    await expect(second.cancelToken?.promise).resolves.toMatchObject({
      message: '取消请求: /api/example',
    });
  });
});
