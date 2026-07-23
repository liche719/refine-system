import { atomWithStorage } from 'jotai/utils';

export const userAtom = atomWithStorage('user', {
  userId: '',
  userName: '',
  userAccount: '',
  avatar: '',
});
