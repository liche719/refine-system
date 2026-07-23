# 运行与故障演练

## 启动顺序

1. 从 `.env.example` 创建 `.env`，为 JWT、内部令牌、Gateway 令牌、数据库、RabbitMQ、邮箱和远程 AI/OCR 设置独立配置。
2. 启动 MySQL 主从、Nacos、Sentinel、RabbitMQ、Identity Redis、AI Redis、PgVector、BanyanDB 和 SkyWalking OAP/UI。
3. 等待 `mysql-replica-init`、`nacos-config-init` 以退出码 0 完成。
4. 按 identity、learning、ai、gateway 顺序启动 Java 服务。
5. 运行 `deploy/scripts/health-check.ps1`，确认业务服务、SkyWalking OAP 和 UI 全部可用。
6. 通过网关发送一轮冒烟请求，再运行 `deploy/scripts/verify-skywalking.ps1`，确认四个服务已经上报。

完整 Compose 已在 Java 镜像中安装 SkyWalking Agent。IDE/Maven 模式先执行 `deploy/skywalking/install-agent.ps1`，然后为每个进程增加 `-javaagent` VM option，并设置该服务自己的 `SW_AGENT_NAME`。

## SkyWalking

SkyWalking UI 地址为 `http://localhost:8088`，OAP HTTP/gRPC 端口为 `12800/11800`。BanyanDB 仅作为观测数据存储，业务服务不直接访问它。

Agent 默认全量采样以便本地演示，可通过 `.env` 中的 `SKYWALKING_AGENT_SAMPLE_N_PER_3_SECS` 限制每个服务实例三秒内的采样数。生产环境不应保留全量采样。

```powershell
.\deploy\scripts\health-check.ps1
.\deploy\scripts\verify-skywalking.ps1
```

第二个脚本查询 OAP GraphQL；如果缺少服务，先通过 Gateway 访问对应接口产生 Trace。日志格式包含 SkyWalking `traceId`，可从调用链跳转到相同 Trace 的应用日志。

## 主从验证

查看复制线程、GTID 和延迟：

```powershell
.\deploy\scripts\check-replication.ps1
```

验证主库写入能从从库读取：

```powershell
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml exec -T mysql-primary mysql -uroot -p -e "INSERT INTO learning_db.UserData(user_id) VALUES('replica-check') ON DUPLICATE KEY UPDATE user_id=user_id"
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml exec -T mysql-replica mysql -uroot -p -e "SELECT * FROM learning_db.UserData WHERE user_id='replica-check'"
```

验证账号隔离：

```powershell
.\deploy\scripts\verify-schema-isolation.ps1
```

## 从库故障回退

`FallbackDataSource` 保留从库不可用时回退主库的能力。当前错题、知识点、概览等用户刚写入后会立即展示的查询固定使用主库，以避免异步复制窗口造成旧数据；将来新增 `@ReadReplica` 读模型时，可按以下步骤演练回退：

```powershell
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml stop mysql-replica
# 调用新增的 @ReadReplica 读模型，确认日志包含回退信息。
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml start mysql-replica
docker compose -p refine-microservices --env-file .env -f docker-compose.infra.yml run --rm mysql-replica-init
```

日志应包含 `Replica unavailable; routing read to primary`。主库故障不会自动切换。

## 手动提升从库

先停止所有写入并确认复制追平，再执行：

```powershell
.\deploy\scripts\promote-replica.ps1 -Force
```

脚本停止并重置复制、关闭从库只读。随后在各服务的生产 Nacos 配置中更新 `refine.datasource.primary-url` 并重启服务。脚本故意不自动 fence 旧主库，也不自动重建复制拓扑。

## RabbitMQ 重试与 DLQ

AI 队列：

- `refine.ai.identity.user.logged-in.v1`
- `refine.ai.learning.activity.recorded.v1`
- 对应 `.dlq` 队列

制造无法反序列化或消费异常的消息后，确认主队列完成 3 次消费尝试，消息进入 `.dlq`。重新投递前应修复消息或消费者；相同 `eventId` 的成功消息会被 `consumed_events` 去重。

## Sentinel

`nacos-config-init` 将规则发布到 `REFINE` group。网关对登录、OCR、题目生成和 AI 总路由分别限流；AI 的 Feign 规则限制内部 learning 调用。限流响应是 HTTP 429，服务不可用时 Feign fallback 快速返回 503 或空的非关键建议列表，不存在无限重试。

在完整 Compose 健康后，对公开登录路由并发发起请求，验证网关既放行阈值内流量，也对超额流量返回兼容的 HTTP 429/JSON `code=429` 响应：

```powershell
.\deploy\scripts\load-test-sentinel.ps1
```

可通过 `-Requests`、`-TimeoutSec` 和 `-MinimumRateLimited` 调整压力和断言。脚本默认使用本地 Flyway 演示账号，不应用于生产环境。

## 文本编码检查

工程文本统一使用无 BOM UTF-8，并由 `.editorconfig` 固化。Windows PowerShell 5 读取无 BOM 文件时需要显式指定编码，例如 `Get-Content -Encoding UTF8 docs/OPERATIONS.md`。提交前执行：

```powershell
.\deploy\scripts\check-text-encoding.ps1
```

脚本会拒绝非法 UTF-8、替换字符、私用区字符和常见的二次转码乱码标记。

## RAG 幂等检查

首次启动 AI 后记录：

```sql
SELECT source_path, checksum, indexed_at, updated_at FROM knowledge_embeddings;
```

不修改 `docs/rag` 重启 AI，行数和 `updated_at` 应保持不变；修改文件后仅对应路径的 checksum 与 `updated_at` 改变。源码验收可运行：

```powershell
rg "TRUNCATE TABLE|DELETE FROM knowledge_embeddings" .
```

## 回退

```powershell
docker compose -p refine-microservices --env-file .env -f docker-compose.yml down
```

随后按旧项目方式启动 `refine-app` 的 8091 端口。不要删除新版 volume，除非明确不再需要演示数据。

SkyWalking Agent 采用 fail-open 行为；OAP 暂时不可用不会阻止业务服务启动。需要临时关闭链路采集时，从 Java 启动参数中移除 `-javaagent` 即可。

## 本地联调清单

服务全部启动后，先确认 Gateway 与三个业务服务健康：

```powershell
8080, 8101, 8102, 8103 | ForEach-Object {
  Invoke-WebRequest "http://localhost:$_/actuator/health" -UseBasicParsing
}
```

前端通过 `http://localhost:5173` 访问，并且只应代理 `/api/**` 到 Gateway。使用本地演示账号完成登录后，按以下顺序检查：学习概览、错题列表、知识点库、错题详情刷新、AI 解题 SSE、带题目上下文的问答、OCR 上传入库、生成练习题和判题 SSE。

Gateway 安全边界可用伪造身份头验证；以下请求应返回 `401`，不能因为客户端提供 `X-User-Id` 而被识别为已登录：

```powershell
Invoke-WebRequest http://localhost:8102/api/v1/feedback/review/list -Headers @{ "X-User-Id" = "forged" } -UseBasicParsing
Invoke-WebRequest http://localhost:8103/api/v1/learning-analysis/get -Headers @{ "X-User-Id" = "forged" } -UseBasicParsing
Invoke-WebRequest http://localhost:8080/api/v1/feedback/review/list -Headers @{ "X-User-Id" = "forged" } -UseBasicParsing
```
