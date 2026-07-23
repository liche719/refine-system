// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';

import { parseSseFrame } from './sse';

describe('SSE frame parser', () => {
  it('keeps Markdown whitespace in ordinary message chunks', () => {
    expect(parseSseFrame('data:   ```java\ndata:   code')).toEqual({
      event: 'message',
      data: '  ```java\n  code',
    });
  });

  it('preserves the error event name for callers', () => {
    expect(parseSseFrame('event: error\ndata: AI 服务暂时不可用')).toEqual({
      event: 'error',
      data: 'AI 服务暂时不可用',
    });
  });
});
