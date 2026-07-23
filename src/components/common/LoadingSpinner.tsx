import { LoaderCircle } from 'lucide-react';

export default function LoadingSpinner({ text = '加载中' }: { text?: string }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />
      <span>{text}</span>
    </div>
  );
}
