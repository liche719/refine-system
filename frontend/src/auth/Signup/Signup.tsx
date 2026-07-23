import { SignupForm } from '@/components/layout/signup-form';
import { AuthShell } from '@/components/layout/auth-shell';

export default function SignupPage() {
  return (
    <AuthShell>
      <SignupForm />
    </AuthShell>
  );
}
