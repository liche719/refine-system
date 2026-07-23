# AI、OCR 与邮件配置

## 密钥

`.env` 只保存敏感值：

```dotenv
OPENAI_API_KEY=
OPENAI_EMBEDDING_API_KEY=
OCR_API_KEY=
MAIL_PASSWORD=
```

Chat、Embedding 和 OCR 使用独立密钥，避免供应商、额度和权限相互影响。密钥不得写入 YAML、Compose、前端代码或 Git。

## LangChain4j OpenAI

普通对话、SSE 和远程 Embedding 使用 LangChain4j OpenAI Provider，并沿用 `langchain4j.open-ai.*` 属性结构。项目基线是 Spring Boot 3.3.5，而 LangChain4j 1.18 Starter 依赖 Spring Boot 3.5 API，因此模型 Bean 由项目按同一属性结构显式创建。

非敏感配置位于 `refine-ai-service/src/main/resources/application.yml`：

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.example.com/v1
      api-key: ${OPENAI_API_KEY}
      model-name: chat-model
      timeout: PT30S
      max-retries: 2
    streaming-chat-model:
      base-url: https://api.example.com/v1
      api-key: ${OPENAI_API_KEY}
      model-name: chat-model
      timeout: PT120S
    embedding-model:
      base-url: https://embedding.example.com/v1
      api-key: ${OPENAI_EMBEDDING_API_KEY}
      model-name: embedding-model
      dimensions: 1536
```

`base-url` 只写到 `/v1`，不要包含 `/chat/completions` 或 `/embeddings`。模型名、维度、超时和重试属于非敏感配置，生产环境可由 Nacos 覆盖。

## OCR

OCR 使用独立的 OpenAI-compatible 多模态模型，同样由 LangChain4j 调用：

```yaml
refine:
  ocr:
    provider: openai
    base-url: https://vision.example.com/v1
    model-name: vision-model
    api-key: ${OCR_API_KEY}
    timeout: PT60S
    max-retries: 2
```

更换 OCR 供应商时只修改 YAML 或 Nacos 中的 Base URL 和模型名。只要供应商兼容 OpenAI 多模态消息格式，`.env` 始终使用通用的 `OCR_API_KEY`。

## 邮件验证码

Identity 服务始终通过真实 QQ SMTP 发送验证码，不再依赖本地 SMTP 模拟器。QQ 邮箱授权码只保存在 `.env` 的 `MAIL_PASSWORD`：

```dotenv
MAIL_PASSWORD=你的QQ邮箱授权码
```

非敏感的邮箱账号和 SMTP 参数位于 `refine-identity-service/src/main/resources/application.yml`，也可以由 Nacos 的 `refine-identity-service.yml` 覆盖：

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: liangchaowen6@qq.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

refine:
  mail:
    enabled: true
    from: ${spring.mail.username}
    code-ttl: PT5M
```

`MAIL_PASSWORD` 不是 QQ 登录密码，而是在 QQ 邮箱“设置 -> 账号 -> POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV 服务”中开启 SMTP 后生成的授权码。本机从 IDE/Maven 启动时，需要把 `MAIL_PASSWORD` 加入进程环境变量；Compose 会从根目录 `.env` 注入。生产 Profile 会在启动时测试 SMTP 连接，授权码或网络错误会直接导致启动失败。
