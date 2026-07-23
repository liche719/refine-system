import { useEffect } from 'react';
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useLocation,
} from 'react-router-dom';

import MainPage from '@/pages/Main';
import HomePage from '@/features/Home/Home';
import AiExplainPage from '@/pages/AiExplainPage/AiExplainPage';
import KnowledgePointPage from '@/pages/KnowledgePointPage/KnowledgePointPage';
// import PracticePage from "@/pages/practice/PracticePage";
import UploadQuestionPage from '@/features/UploadQuestion/UploadQuestion';
import QuestionDetailPage from '@/features/UploadQuestion/QuestionDetail';
import MyQuestionPage from '@/features/MyQuestion/MyQuestion';
import ForgotPasswordPage from '../auth/ForgotPassword/ForgotPassword';
import SignupPage from '@/auth/Signup/Signup';
import LoginPage from '@/auth/Login/Login';
import NotFound from '@/components/common/NotFound';

function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    const scrollToTop = () => window.scrollTo(0, 0);
    scrollToTop();
    const frameId = window.requestAnimationFrame(scrollToTop);
    const timeoutId = window.setTimeout(scrollToTop, 250);

    return () => {
      window.cancelAnimationFrame(frameId);
      window.clearTimeout(timeoutId);
    };
  }, [pathname]);

  return null;
}

function Router() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        {/* 认证路由 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route element={<MainPage />}>
          <Route path="/" element={<Navigate to="/home" replace />} />
          <Route path="/home" element={<HomePage />} />
          <Route path="/upload-question" element={<UploadQuestionPage />} />
          <Route
            path="/upload-question/question-detail"
            element={<QuestionDetailPage />}
          />
          <Route path="/my-question" element={<MyQuestionPage />} />
          <Route path="/knowledge-base" element={<KnowledgePointPage />} />
          <Route path="/ai-explain" element={<AiExplainPage />} />
        </Route>
        {/* 根级404路由 - 匹配所有其他未定义的路径 */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}

export default Router;
