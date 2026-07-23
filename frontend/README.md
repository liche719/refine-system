# Refine 智能错题系统前端

基于 React 19、TypeScript、Vite、React Router、React Flow 和 Recharts 构建的 Refine 微服务前端。开发环境通过 Vite 将 `/api` 请求代理到 Gateway，浏览器不会直接访问内部服务。

## 本地运行

环境要求：Node.js 22.12+、npm 10+，以及已启动的 `../backend` 微服务。

```powershell
cd frontend
npm install
npm run dev
```

访问 [http://127.0.0.1:5173](http://127.0.0.1:5173)。本地演示账号：

```text
账号：demo@refine.local
密码：RefineDemo123
```

## 环境变量

复制 `.env.example` 作为本地配置参考：

```dotenv
VITE_BASE_URL=
VITE_PROXY_URL=http://localhost:8080
VITE_API_VERSION=v1
```

- `VITE_BASE_URL`：浏览器 API 前缀。开发环境留空，使用 Vite 同源代理。
- `VITE_PROXY_URL`：本地 Gateway 地址。
- `VITE_API_VERSION`：保留的 API 版本标识。

## 质量检查

```powershell
npm run lint
npm run type-check
npm test
npm run build
npm audit
```

## 本地联调

先启动 `../backend` 的基础设施与四个 Java 服务，再运行 `npm run dev`。浏览器只访问 `http://localhost:5173`，所有 `/api/**` 请求由 Vite 转发到 Gateway；不要将浏览器请求改为直连 `8101`、`8102` 或 `8103`。

本地验收顺序：登录、首页、错题库、知识点库、刷新错题详情页、AI 解题与问答、OCR 上传、生成练习题、判题 SSE。前端的 `npm run type-check`、`npm test`、`npm run lint` 与 `npm run build` 应全部通过。

## 已联调能力

- 注册、登录、刷新令牌、退出登录和重置密码。
- 学习概览、复习统计、错题筛选与批量删除。
- OCR 上传入库、错因切换、其他错因和复习笔记。
- 动态知识图谱、关联错题、知识点状态和生成练习题。
- AI 解题、生成题判题及 SSE 流式响应。

后端工程位于相邻目录 `../backend`。内部服务端口、JWT、`X-User-Id` 清洗和 `X-Internal-Token` 均由微服务侧控制，前端只通过 Gateway 访问公共 API。
