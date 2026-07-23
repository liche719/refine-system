export interface SessionUser {
  userId: string;
  userName: string;
  userAccount: string;
  avatar: string;
}

const ACCESS_TOKEN = 'access-token';
const REFRESH_TOKEN = 'refresh-token';
const USER = 'user';

export const authStorage = {
  accessToken: () => localStorage.getItem(ACCESS_TOKEN),
  refreshToken: () => localStorage.getItem(REFRESH_TOKEN),
  saveTokens: (accessToken: string, refreshToken?: string) => {
    localStorage.setItem(ACCESS_TOKEN, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN);
    localStorage.removeItem(REFRESH_TOKEN);
    localStorage.removeItem(USER);
  },
};
