import { Navigate, Outlet, useLocation } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import { authStorage } from '@/utils/auth';

export default function Main() {
  const location = useLocation();
  if (!authStorage.accessToken()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return (
    <Sidebar>
      <Outlet />
    </Sidebar>
  );
}
