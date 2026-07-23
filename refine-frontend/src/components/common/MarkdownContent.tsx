import ReactMarkdown from 'react-markdown';
import rehypeKatex from 'rehype-katex';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';

import { cn } from '@/lib/utils';

interface MarkdownContentProps {
  content: string;
  className?: string;
  isStreaming?: boolean;
}

export function normalizeModelMarkdown(content: string) {
  let inFence = false;

  return content
    .split('\n')
    .map((line) => {
      if (/^\s*(```|~~~)/.test(line)) {
        inFence = !inFence;
        return line;
      }
      if (inFence) return line;

      return line
        .replace(/^(\s{0,3}#{1,6})(?!#)(?=\S)/, '$1 ')
        .replace(/^(\s{0,3})([-*+])(?!\2)(?=\S)/, '$1$2 ')
        .replace(/^(\s{0,3}\d+\.)(?=\S)/, '$1 ')
        .replace(/\\([abcxyz])(?=\s*=)/g, '$1');
    })
    .join('\n');
}

export default function MarkdownContent({
  content,
  className,
  isStreaming = false,
}: MarkdownContentProps) {
  return (
    <div className={cn('markdown-body break-words', className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
      >
        {normalizeModelMarkdown(content)}
      </ReactMarkdown>
      {isStreaming && <span className="stream-caret" aria-hidden="true" />}
    </div>
  );
}
