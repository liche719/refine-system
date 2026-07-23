import { LoginForm } from '@/components/layout/login-form';
import { AuthShell } from '@/components/layout/auth-shell';

export default function LoginPage() {
  return (
    <AuthShell>
      <LoginForm />
    </AuthShell>
  );
}
