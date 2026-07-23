# 配置约定

## 敏感配置

根目录 `.env` 只保存密码、Token 和 API Key。`.env.example` 是完整键清单：

- 数据库、RabbitMQ、PgVector 和邮箱密码
- JWT Secret、内部调用 Token 与 Gateway 令牌
- Chat、Embedding 和 OCR API Key

这些值由 Compose 注入容器，并在应用 YAML 中通过 `${KEY}` 引用。生产部署应优先使用平台 Secret 或密钥管理服务，不应提交真实 `.env`。

`GATEWAY_TOKEN` 与 `INTERNAL_TOKEN` 必须使用不同的随机值：前者只由 Gateway 转发业务请求时注入，后者只允许 AI 调用 Learning 的 `/internal/**` 接口。业务服务会拒绝缺失或不匹配的 Gateway 令牌。

## 非敏感配置

端口、主机、数据库用户名、Base URL、模型名、TTL、超时、重试和功能开关写在 YAML 或 Nacos：

- `application.yml`：本机开发默认值，基础设施连接 `localhost`
- `application-docker.yml`：完整 Compose 的容器网络地址
- `application-prod.yml`：生产环境额外启用 SMTP 启动连接检查
- Nacos：生产环境覆盖值和 Sentinel 动态规则

完整 Compose 只使用 `SPRING_PROFILES_ACTIVE=docker` 选择容器配置。SkyWalking Agent 的 `SW_AGENT_*` 属于 JVM Agent 启动参数，不是业务配置，因此仍由容器环境传入。

## Redis

- Identity Redis：本机 `localhost:6379`，保存邮箱验证码和 refresh token。
- AI Redis：本机 `localhost:6380`，保存 LangChain4j 会话记忆和生成题缓存。
- Docker Profile 分别连接 `identity-redis:6379` 与 `ai-redis:6379`。
- Learning 不使用 Redis，避免无意义的运行依赖。

## RAG

`refine.pgvector` 的 `chunk-size`、`chunk-overlap`、`semantic-candidates`、`lexical-candidates`、`result-limit`、`minimum-fused-score` 与 `reciprocal-rank-constant` 均为非敏感参数，可在 YAML 或 Nacos 调整。教材资料的审核字段和录入规范见 `docs/RAG_KNOWLEDGE_BASE.md`。

## 邮件

本机、Docker 和生产环境统一使用 QQ SMTP，不再启动本地 SMTP 模拟器。`MAIL_USERNAME` 与 QQ 邮箱授权码 `MAIL_PASSWORD` 都由 `.env` 提供；`smtp.qq.com:587`、STARTTLS、发件人和验证码 TTL 写在 Identity YAML/Nacos。Compose 要求两个变量均非空，生产 Profile 还会在启动时验证 SMTP 连接。
