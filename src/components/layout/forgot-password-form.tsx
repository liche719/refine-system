import { useState } from 'react';
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
import { ArrowLeft, CheckCircle2 } from 'lucide-react';
import { ResetPassword } from '@/services/user/user';
import { SendCode } from '@/services/user/user';
import { toast } from 'sonner';
import { errorMessage } from '@/utils/api';

export function ForgotPasswordForm({
  className,
  ...props
}: React.ComponentProps<'form'>) {
  const [step, setStep] = useState<'email' | 'reset' | 'success'>('email');
  const [formData, setFormData] = useState({
    userAccount: '',
    checkCode: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [countdown, setCountdown] = useState(0);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSendCode = async () => {
    if (!formData.userAccount) {
      setErrors({ ...errors, userAccount: '请输入邮箱地址' });
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.userAccount)) {
      setErrors({ ...errors, userAccount: '请输入有效的邮箱地址' });
      return;
    }

    setIsSendingCode(true);
    try {
      const res = await SendCode(formData.userAccount);
      if (res.code === 200) {
        toast.success('验证码发送成功');
        setCountdown(60);

        const timer = setInterval(() => {
          setCountdown((prev) => {
            if (prev <= 1) {
              clearInterval(timer);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
      } else {
        toast.error(res.info);
      }
    } catch (error) {
      toast.error(errorMessage(error, '验证码发送失败'));
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData({ ...formData, [id]: value });
    if (errors[id]) {
      setErrors({ ...errors, [id]: '' });
    }
  };

  const validateEmailStep = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.userAccount) {
      newErrors.userAccount = '请输入邮箱地址';
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.userAccount)) {
        newErrors.userAccount = '请输入有效的邮箱地址';
      }
    }
    if (!formData.checkCode) {
      newErrors.checkCode = '请输入验证码';
    } else if (formData.checkCode.length !== 6) {
      newErrors.checkCode = '验证码应为6位数字';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validateResetStep = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.newPassword) {
      newErrors.newPassword = '请输入新密码';
    } else if (formData.newPassword.length < 8) {
      newErrors.newPassword = '密码长度至少为8个字符';
    }
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = '请确认密码';
    } else if (formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = '两次输入的密码不一致';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleEmailStepSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validateEmailStep()) {
      setStep('reset');
    }
  };

  const handleResetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateResetStep()) return;
    setIsSubmitting(true);
    try {
      const res = await ResetPassword({
        userAccount: formData.userAccount,
        checkCode: formData.checkCode,
        newPassword: formData.newPassword,
      });
      if (res.code !== 200) throw new Error(res.info);
      toast.success('密码重置成功');
      setStep('success');
    } catch (error) {
      toast.error(errorMessage(error, '密码重置失败'));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (step === 'email') {
    return (
      <form
        className={cn('flex flex-col gap-6', className)}
        onSubmit={handleEmailStepSubmit}
        {...props}
      >
        <FieldGroup>
          <div className="flex flex-col items-center gap-1 text-center">
            <h1 className="text-2xl font-bold">忘记密码？</h1>
            <p className="text-muted-foreground text-sm text-balance">
              输入您的邮箱地址，我们将发送验证码以重置密码
            </p>
          </div>
          <Field>
            <FieldLabel htmlFor="userAccount">邮箱</FieldLabel>
            <Input
              id="userAccount"
              type="email"
              placeholder="请输入邮箱地址"
              value={formData.userAccount}
              onChange={handleChange}
              className={errors.userAccount ? 'border-red-500' : ''}
            />
            {errors.userAccount && (
              <FieldDescription className="text-red-500 text-xs">
                {errors.userAccount}
              </FieldDescription>
            )}
          </Field>
          <Field>
            <FieldLabel htmlFor="checkCode">验证码</FieldLabel>
            <div className="flex gap-2">
              <Input
                id="checkCode"
                type="text"
                placeholder="请输入6位验证码"
                maxLength={6}
                value={formData.checkCode}
                onChange={handleChange}
                className={errors.checkCode ? 'border-red-500' : ''}
              />
              <Button
                type="button"
                variant="outline"
                onClick={handleSendCode}
                disabled={countdown > 0 || isSendingCode}
                className="whitespace-nowrap min-w-[100px]"
              >
                {isSendingCode
                  ? '发送中...'
                  : countdown > 0
                    ? `${countdown}秒`
                    : '发送验证码'}
              </Button>
            </div>
            {errors.checkCode && (
              <FieldDescription className="text-red-500 text-xs">
                {errors.checkCode}
              </FieldDescription>
            )}
          </Field>
          <Field>
            <Button type="submit" className="w-full cursor-pointer">
              下一步
            </Button>
          </Field>
          <Field>
            <FieldDescription className="text-center">
              <Link
                to="/login"
                className="inline-flex items-center gap-1 underline underline-offset-4 hover:text-foreground cursor-pointer"
              >
                <ArrowLeft className="size-3" />
                返回登录
              </Link>
            </FieldDescription>
          </Field>
        </FieldGroup>
      </form>
    );
  }

  if (step === 'reset') {
    return (
      <form
        className={cn('flex flex-col gap-6', className)}
        onSubmit={handleResetSubmit}
        {...props}
      >
        <FieldGroup>
          <div className="flex flex-col items-center gap-1 text-center">
            <h1 className="text-2xl font-bold">重置密码</h1>
            <p className="text-muted-foreground text-sm text-balance">
              请输入您的新密码
            </p>
          </div>
          <Field>
            <FieldLabel htmlFor="newPassword">新密码</FieldLabel>
            <Input
              id="newPassword"
              type="password"
              value={formData.newPassword}
              onChange={handleChange}
              className={errors.newPassword ? 'border-red-500' : ''}
            />
            {errors.newPassword ? (
              <FieldDescription className="text-red-500 text-xs">
                {errors.newPassword}
              </FieldDescription>
            ) : (
              <FieldDescription>密码长度至少为8个字符</FieldDescription>
            )}
          </Field>
          <Field>
            <FieldLabel htmlFor="confirmPassword">确认密码</FieldLabel>
            <Input
              id="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className={errors.confirmPassword ? 'border-red-500' : ''}
            />
            {errors.confirmPassword && (
              <FieldDescription className="text-red-500 text-xs">
                {errors.confirmPassword}
              </FieldDescription>
            )}
          </Field>
          <Field>
            <Button
              type="submit"
              className="w-full cursor-pointer"
              disabled={isSubmitting}
            >
              {isSubmitting ? '提交中...' : '重置密码'}
            </Button>
          </Field>
          <Field>
            <FieldDescription className="text-center">
              <button
                type="button"
                onClick={() => setStep('email')}
                className="inline-flex items-center gap-1 underline underline-offset-4 hover:text-foreground cursor-pointer"
              >
                <ArrowLeft className="size-3" />
                返回上一步
              </button>
            </FieldDescription>
          </Field>
        </FieldGroup>
      </form>
    );
  }

  return (
    <div className={cn('flex flex-col gap-6', className)}>
      <FieldGroup>
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="rounded-full bg-green-100 p-3">
            <CheckCircle2 className="size-8 text-green-600" />
          </div>
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-bold">密码重置成功！</h1>
            <p className="text-muted-foreground text-sm text-balance">
              您的密码已成功重置，现在可以使用新密码登录了
            </p>
          </div>
        </div>
        <Field>
          <Link to="/login" className="w-full">
            <Button type="button" className="w-full cursor-pointer">
              返回登录
            </Button>
          </Link>
        </Field>
      </FieldGroup>
    </div>
  );
}
