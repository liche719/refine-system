// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import MarkdownContent from './MarkdownContent';

afterEach(cleanup);

describe('MarkdownContent', () => {
  it('renders markdown, GFM tables and formulas as elements', () => {
    const { container } = render(
      <MarkdownContent
        content={`## 解题步骤\n\n**结论**\n\n| 项目 | 值 |\n| --- | --- |\n| 结果 | 4 |\n\n$$\nx^2 = 4\n$$`}
      />,
    );

    expect(screen.getByRole('heading', { name: '解题步骤' })).not.toBeNull();
    expect(screen.getByText('结论').tagName).toBe('STRONG');
    expect(screen.getByRole('table')).not.toBeNull();
    expect(container.querySelector('.katex-display')).not.toBeNull();
  });

  it('does not interpret raw html', () => {
    const { container } = render(
      <MarkdownContent content={'<script>window.hacked = true</script>'} />,
    );

    expect(container.querySelector('script')).toBeNull();
    expect(screen.getByText(/window\.hacked/)).not.toBeNull();
  });

  it('normalizes compact model markdown without changing fenced code', () => {
    render(
      <MarkdownContent
        content={'###解题步骤\n\n1.先配方\n\n```md\n###保持原样\n```'}
      />,
    );

    expect(screen.getByRole('heading', { name: '解题步骤' })).not.toBeNull();
    expect(screen.getByRole('list')).not.toBeNull();
    expect(screen.getByText('###保持原样')).not.toBeNull();
  });
});
