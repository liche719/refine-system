import { Link } from 'react-router-dom';
import { useEffect } from 'react';
import { toast } from 'sonner';

// 404错误页面组件
export default function NotFound() {
  // 组件挂载时显示一个错误提示
  useEffect(() => {
    toast.error('页面不存在');
  }, []);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background">
      <div className="text-center p-8 max-w-md">
        {/* 大号错误码 */}
        <h1 className="text-9xl font-bold text-red-500 mb-4">404</h1>

        {/* 错误信息 */}
        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
          抱歉，您要查找的页面不存在
        </h2>

        <p className="text-gray-600 dark:text-gray-300 mb-8">
          您可能输入了错误的URL，或者该页面已被删除。
        </p>

        {/* 引导回到主页的按钮 */}
        <Link
          to="/home"
          className="inline-flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-white bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary transition duration-150 ease-in-out"
        >
          Back to Home
        </Link>
      </div>
    </div>
  );
}
