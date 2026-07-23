import type { LucideIcon } from 'lucide-react';

export default function EmptyState({
  icon: Icon,
  title,
  description,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
}) {
  return (
    <div className="empty-state" role="status">
      <Icon className="size-6" aria-hidden="true" />
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}
