import { ForgotPasswordForm } from '@/components/layout/forgot-password-form';
import { AuthShell } from '@/components/layout/auth-shell';

export default function ForgotPasswordPage() {
  return (
    <AuthShell>
      <ForgotPasswordForm />
    </AuthShell>
  );
}
