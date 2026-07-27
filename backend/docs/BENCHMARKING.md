# 本地性能基准

该基准在固定的本地 Docker 环境中采集 Refine 经过 Gateway 的端到端数据。结果只代表当前机器、当前 Compose 资源和当前压测数据集，不能外推为生产容量。

## 前置条件

1. 使用完整 Compose 启动服务，并确认 Gateway、Identity、Learning、AI 容器健康。
2. 使用 Flyway 演示账号 `demo@refine.local`。
3. 关闭高频 AI 请求，避免远程模型调用干扰本地读接口数据。
4. 本地压测数据只允许在开发环境生成，执行完成后必须清理。

## 固定数据集

错题列表优化使用 20,000 条带 `benchmark-` 前缀的本地错题数据。脚本不会被 Flyway 执行，重复运行会先删除旧的基准数据。

```powershell
# 生成数据，等待 MySQL 主从复制完成后再测试读取
.\deploy\scripts\prepare-api-benchmark-data.ps1

# 压测完成后清理
.\deploy\scripts\prepare-api-benchmark-data.ps1 -Cleanup
```

## 执行方式

在 `backend` 目录运行：

```powershell
.\deploy\scripts\run-api-benchmark.ps1 -Scenario mistake-list -Warmup 100 -Requests 1000 -Concurrency 20
.\deploy\scripts\run-api-benchmark.ps1 -Scenario overview -Warmup 100 -Requests 1000 -Concurrency 20
.\deploy\scripts\run-api-benchmark.ps1 -Scenario knowledge-points -Warmup 100 -Requests 1000 -Concurrency 20
```

结果写入 Git 已忽略的 `.runtime/benchmarks`。每份结果包含请求数、并发数、端到端吞吐、成功和失败数量、HTTP 状态分布、平均延迟及 p50、p95、p99 延迟。

## 数据使用规则

- 固定 Java 版本、Docker 资源、Compose 配置、数据集、预热次数、请求数和并发数。
- 每个场景至少执行三轮，使用中位数结果，并记录最差一轮错误率。
- 简历中的提升比例只能来自同一环境、同一场景、优化前后的对比。
- AI、OCR、邮件等依赖远程服务的接口不作为 Java 服务性能指标。
- 登录高并发使用 `load-test-sentinel.ps1` 验证 Sentinel 保护行为，不把限流后的 429 计入业务吞吐。

## 已复核的本地对比

日期为 2026-07-26。本机完整 Compose 环境通过 Gateway 压测错题列表，固定为 20,000 条错题数据、100 次预热、1,000 次正式请求、20 并发、前后各连续三轮，采用中位数。

| 指标 | 优化前 | 优化后 | 变化 |
| --- | ---: | ---: | ---: |
| 吞吐 | 16.74 请求每秒 | 40.56 请求每秒 | 提升 142.29% |
| p50 延迟 | 1107 毫秒 | 470 毫秒 | 降低 57.54% |
| p95 延迟 | 1980 毫秒 | 767 毫秒 | 降低 61.26% |
| p99 延迟 | 2688 毫秒 | 968 毫秒 | 降低 63.99% |
| 失败请求 | 0 | 0 | 无变化 |

优化内容为在 `MistakeQuestion` 添加 `user_id`、`question_status`、`update_time DESC` 联合索引。优化前执行计划使用用户前缀扫描并执行 filesort。优化后使用联合索引且不再 filesort，`EXPLAIN ANALYZE` 显示列表页实际仅读取 10 行。

可通过以下命令重新计算已保存的对比结果：

```powershell
.\deploy\scripts\summarize-api-benchmark.ps1
```

这些数据仅可表述为本地固定环境基准结果，不能表述为生产容量承诺。
