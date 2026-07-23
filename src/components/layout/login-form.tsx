import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Login } from '@/services/user/user';
import { userAtom } from '@/atoms/user';
import { useAtom } from 'jotai';
import { authStorage } from '@/utils/auth';
import { errorMessage } from '@/utils/api';

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<'form'>) {
  const [formData, setFormData] = useState({
    userAccount: '',
    userPassword: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const [, setUser] = useAtom(userAtom);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData({ ...formData, [id]: value });
    if (errors[id]) {
      setErrors({ ...errors, [id]: '' });
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!validateForm()) return;
    setIsSubmitting(true);
    try {
      const res = await Login(formData);
      if (res.code !== 200) throw new Error(res.info);
      setUser({
        userId: res.data.userId,
        userName: res.data.userName,
        userAccount: formData.userAccount,
        avatar: '',
      });
      authStorage.saveTokens(res.data.accessToken, res.data.refreshToken);
      toast.success('登录成功');
      navigate('/home', { replace: true });
    } catch (error) {
      toast.error(errorMessage(error, '登录失败'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.userAccount) {
      newErrors.userAccount = '请输入邮箱地址';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.userAccount)) {
        newErrors.userAccount = '请输入有效的邮箱地址';
      }
    }
    if (!formData.userPassword) {
      newErrors.userPassword = '请输入密码';
    } else if (formData.userPassword.length < 8) {
      newErrors.userPassword = '密码长度至少为8个字符';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  return (
    <form
      className={cn('flex flex-col gap-6', className)}
      {...props}
      onSubmit={handleSubmit}
    >
      <FieldGroup>
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="text-2xl font-bold">登录您的账户</h1>
          <p className="text-muted-foreground text-sm text-balance">
            输入您的邮箱和密码进行登录
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="userAccount">邮箱</FieldLabel>
          <Input
            id="userAccount"
            type="text"
            placeholder="请输入邮箱地址"
            value={formData.userAccount}
            onChange={handleChange}
            autoComplete="email"
            className={errors.userAccount ? 'border-red-500' : ''}
          />
          {errors.userAccount && (
            <FieldDescription className="text-red-500 text-xs">
              {errors.userAccount}
            </FieldDescription>
          )}
        </Field>
        <Field>
          <div className="flex items-center">
            <FieldLabel htmlFor="userPassword">密码</FieldLabel>
            <Link
              to="/forgot-password"
              className="ml-auto text-sm underline-offset-4 hover:underline cursor-pointer"
            >
              忘记密码？
            </Link>
          </div>
          <Input
            id="userPassword"
            type="password"
            required
            autoComplete="current-password"
            value={formData.userPassword}
            onChange={handleChange}
            className={errors.userPassword ? 'border-red-500' : ''}
          />
          {errors.userPassword && (
            <FieldDescription className="text-red-500 text-xs">
              {errors.userPassword}
            </FieldDescription>
          )}
        </Field>
        <Field>
          <Button
            type="submit"
            className="w-full cursor-pointer"
            disabled={isSubmitting}
          >
            {isSubmitting ? '登录中...' : '登录'}
          </Button>
        </Field>
        <Field>
          <FieldDescription className="text-center">
            还没有账户？{' '}
            <Link
              to="/signup"
              className="underline underline-offset-4 hover:text-foreground cursor-pointer"
            >
              立即注册
            </Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  );
}
