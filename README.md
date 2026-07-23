# Refine Microservices

这是 Refine 的并行微服务版本。原 DDD 单体不参与本工程 Maven reactor，也不需要修改；新版每个业务服务内部继续按领域组织代码。

## 模块

| 模块 | 端口 | 职责 |
|---|---:|---|
| `refine-gateway` | 8080 | 路由、JWT、身份头清洗、CORS、Sentinel 限流 |
| `refine-identity-service` | 8101 | 注册、登录、邮箱验证码、密码和令牌 |
| `refine-learning-service` | 8102 | 错题、知识点、复习、概览 |
| `refine-ai-service` | 8103 | AI/OCR、会话、题目生成、学习分析、RAG |
| `refine-common` | - | 统一响应、异常、用户上下文、主从路由 |
| `refine-contracts` | - | Feign DTO 和 RabbitMQ 事件线协议 |

## 快速开始

先创建本地环境文件并替换所有 `change-me` 值：

```powershell
Copy-Item .env.example .env
```

仅启动基础设施，Java 服务从 IDE 或 Maven 运行：

```powershell
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml up -d
.\deploy\skywalking\install-agent.ps1

# 在每个 IDE/Maven 进程中增加 VM option，并设置对应服务名：
# -javaagent:<项目目录>\.runtime\skywalking\agent\skywalking-agent.jar
# SW_AGENT_NAME=refine-identity-service
# SW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800

mvn -pl refine-identity-service spring-boot:run
mvn -pl refine-learning-service spring-boot:run
mvn -pl refine-ai-service spring-boot:run
mvn -pl refine-gateway spring-boot:run
```

完整容器化启动：

```powershell
mvn verify
docker compose -p refine-microservices --env-file .env -f docker-compose.yml up -d --build
```

验证构建和运行状态：

```powershell
mvn verify
.\deploy\scripts\health-check.ps1
.\deploy\scripts\verify-skywalking.ps1
.\deploy\scripts\check-replication.ps1
.\deploy\scripts\verify-schema-isolation.ps1
```

控制台地址：Nacos `http://localhost:8848/nacos`，Sentinel `http://localhost:8858`，RabbitMQ `http://localhost:15672`，SkyWalking `http://localhost:8088`，BanyanDB `http://localhost:17913`。

Flyway 演示账号为 `demo@refine.local` / `RefineDemo123`，只用于本地演示，不应部署到真实环境。

详细设计见 [架构说明](docs/ARCHITECTURE.md) 和 [运行手册](docs/OPERATIONS.md)。

## 回退

停止新版 Compose 后重新启动旧 `refine-app:8091`。新版使用独立端口、目录和数据库，不需要回滚旧单体文件。
