# Refine System

Refine 是一个面向错题沉淀、知识点复习和 AI 辅助学习的全栈项目。仓库将微服务后端与 React 前端放在同一版本库中，但保持独立构建、独立依赖与独立本地配置。

## 目录

```text
refine-system/
├─ backend/   # Java 21 / Spring Boot 微服务、Compose、运维脚本与技术文档
└─ frontend/  # React / Vite 学习端
```

## 本地启动

### 1. 启动后端基础设施与服务

```powershell
Push-Location backend
Copy-Item .env.example .env
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml up -d

# 在 IDE 中按 identity → learning → ai → gateway 顺序启动；或使用 Maven。
# 首次从仓库启动时，先安装 reactor 内的公共模块；之后仅在公共模块变更后重跑此命令。
mvn clean install -DskipTests

java -jar backend/refine-identity-service/target/refine-identity-service-1.0.0-SNAPSHOT.jar
java -jar backend/refine-learning-service/target/refine-learning-service-1.0.0-SNAPSHOT.jar
java -jar backend/refine-ai-service/target/refine-ai-service-1.0.0-SNAPSHOT.jar
java -jar backend/refine-gateway/target/refine-gateway-1.0.0-SNAPSHOT.jar
Pop-Location
```

后端配置、完整 Compose 与故障演练见 [backend/README.md](backend/README.md) 和 [backend/docs/OPERATIONS.md](backend/docs/OPERATIONS.md)。敏感配置只保存在 `backend/.env`，不可提交。

### 2. 启动前端

```powershell
Push-Location frontend
Copy-Item .env.example .env
npm install
npm run dev
Pop-Location
```

前端默认将 `/api/**` 代理到 `http://localhost:8080`。需要调整 Gateway 地址时，在 `frontend/.env` 设置 `VITE_PROXY_URL`。

## 验证

```powershell
Push-Location backend
mvn verify
Pop-Location

Push-Location frontend
npm run type-check
npm test
npm run lint
npm run build
Pop-Location
```

## 主要技术

- 后端：Java 21、Spring Boot 3、Spring Cloud Alibaba、Nacos、Sentinel、OpenFeign、RabbitMQ、MyBatis、Flyway、Redis、PgVector、LangChain4j。
- 前端：React 19、TypeScript、Vite、Tailwind CSS、React Router、React Markdown。
- 可观测性：SkyWalking、BanyanDB。

## 约定

- `backend/` 和 `frontend/` 都有独立 `.env`；仅提交各自的 `.env.example`。
- 前端所有业务请求经 Gateway，不直连 `8101`、`8102`、`8103`。
- 后端的数据库、消息和内部接口边界见 [backend/docs/ARCHITECTURE.md](backend/docs/ARCHITECTURE.md)。
