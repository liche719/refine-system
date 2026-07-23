import request from '../../utils/request';
import { EmptyResponse, UserResponse } from './type';

export function Login(data: object) {
  return request.post<UserResponse>({ url: '/api/userAccount/login', data });
}

export function Signup(data: {
  userAccount: string;
  userPassword: string;
  userName: string;
  checkCode: string;
}) {
  return request.post<EmptyResponse>({
    url: '/api/userAccount/register',
    data,
  });
}

export function SendCode(userAccount: string) {
  return request.post<EmptyResponse>({
    url: '/api/userAccount/sendEmailCode',
    params: { userAccount },
  });
}

export function Logout() {
  return request.post<EmptyResponse>({
    url: '/api/userAccount/logout',
    headers: {
      'refresh-token': localStorage.getItem('refresh-token') || '',
    },
  });
}

export function ResetPassword(data: {
  userAccount: string;
  newPassword: string;
  checkCode: string;
}) {
  return request.post<EmptyResponse>({
    url: '/api/userAccount/resetPassword',
    params: data,
  });
}
