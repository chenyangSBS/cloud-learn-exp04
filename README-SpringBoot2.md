# SpringBoot进阶实践：用 Spring Boot 完成 SSE 流式推送 + MinIO 文件上传 + Docker 依赖环境

在基础项目 `cloud-learn-exp04` 的之上，我们聚焦三件事：

- 流式接口（SSE）：让服务端把“课程事件”实时推送给客户端
- MinIO 文件上传：把课程封面上传到对象存储，并返回可访问的链接
- Docker 基础：用 Docker Compose 一键拉起 MinIO，统一运行环境

本文件用于课堂练习指导，重点是做什么、为什么、怎么验证、以及如何排错。

---

## 学习目标

- 理解 Spring Boot 中“Controller → Service → 外部依赖（MinIO）”的分层职责
- 掌握 multipart 文件上传接口的请求方式与验收方法
- 掌握 SSE 的订阅/推送模型，理解 `SseEmitter` 的角色与生命周期
- 会用 Docker Compose 启动依赖服务，并能快速定位连接错误（Connection refused）
- 理解“私有桶 + 预签名 URL”的临时授权访问模式

---

## 项目结构（只关注与本次实践内容相关的部分）

- `src/main/java/cs/sbs/web/controller/`
  - `CourseController`：对外提供上传封面与 SSE 订阅接口
- `src/main/java/cs/sbs/web/service/`
  - `OssService`：对象存储服务接口
  - `CourseSseService`：SSE 推送服务接口
  - `CourseService`：课程业务接口（在业务动作中触发 SSE 推送）
- `src/main/java/cs/sbs/web/service/impl/`
  - `MinioOssServiceImpl`：`OssService` 的 MinIO 实现
  - `CourseSseServiceImpl`：`CourseSseService` 的内存实现
  - `CourseServiceImpl`：课程业务实现（在 create/update/delete 等动作中发布事件）
- `src/main/java/cs/sbs/web/dto/`
  - `MinioProperties`：MinIO 配置属性绑定（`app.minio`）
  - `OssObjectInfo`：上传结果（bucket/objectKey/url）
  - `CourseEvent`：SSE 推送的数据载体
- `src/main/resources/application.yml`
  - `app.minio.*`：MinIO 连接与预签名 URL 配置
- `docker-compose.yml`
  - `minio`：MinIO 服务
  - `minio-init`：初始化容器，用于自动创建 bucket

---

## 第 0 步：准备环境与启动项目

任务项：

- 确保 Java 与 Maven 可用，并能启动本项目
- 确保 Docker 与 Docker Compose 可用

完成标准：

- `mvn test` 能通过
- 项目启动后，能访问 `GET /api/courses` 返回课程列表

提示：

- Docker 安装与验收请参考 `README-Docker.md`

---

## 第 1 步：用 Docker Compose 启动 MinIO（对象存储）

任务项：

- 在项目根目录（包含 `docker-compose.yml`）启动 MinIO
- 确认 MinIO API 与 Console 端口已暴露
- 确认 bucket 已自动创建（由 `minio-init` 容器完成）

完成标准：

- MinIO API 就绪：`http://localhost:9000/minio/health/ready` 返回 200
- MinIO Console 可打开：`http://localhost:9001`
- 默认账号/密码可登录：`minioadmin / minioadmin`

理解要点：

- `minio` 是真正的存储服务；`minio-init` 只负责“启动后自动创建 bucket”，执行完会退出

排错要点：

- 如果后续上传报 `Connection refused`，优先检查 MinIO 是否真的启动、端口是否被占用、Spring Boot 配置是否指向了正确的 endpoint

---

## 第 2 步：理解 Spring Boot 如何读取 MinIO 配置（application.yml → 配置类）

任务项：

- 在 `application.yml` 中确认存在 `app.minio.*` 配置项
- 理解“endpoint”与“public-endpoint”的区别

完成标准：

- 你能清楚回答：Spring Boot 运行时访问 MinIO 用哪个地址？返回给浏览器的访问链接用哪个地址？

关键配置解释：

- `app.minio.endpoint`
  - 语义：Spring Boot 后端用它来连接 MinIO（上传、检查 bucket 是否存在、生成预签名 URL）
  - 本地运行 Spring Boot 时：通常是 `http://localhost:9000`
- `app.minio.public-endpoint`
  - 语义：生成给客户端使用的 URL 的“对外可访问地址”
  - 本地环境：通常也使用 `http://localhost:9000`
  - 常见坑：如果把它配置成 `http://minio:9000`，浏览器访问会失败（因为 `minio` 是容器内部网络名）
- `app.minio.presign-expiry-seconds`
  - 语义：预签名 URL 的有效期（秒）

---

## 第 3 步：实现“课程封面上传到 MinIO”的业务流程（不贴代码，按流程做）

目标：

- 提供一个课程封面上传接口
- 上传成功后，更新课程的 `coverUrl`，并把更新后的课程返回给调用方

接口验收口径：

- 请求方式：`multipart/form-data`
- 参数名：`file`
- 路径：`POST /api/courses/{id}/cover`

上传流程图（理解顺序即可）：

```text
Client
  │  POST /api/courses/{id}/cover  (multipart: file)
  v
CourseController
  │  1) 校验 file 不为空
  │  2) 校验 courseId 存在（防止把文件上传成“孤儿对象”）
  │  3) 生成 objectKey（建议包含 courseId + 随机串）
  v
OssService（MinIO 实现）
  │  4) 确保 bucket 存在
  │  5) putObject 上传
  │  6) 生成 GET 预签名 URL（带过期时间）
  v
CourseService
  │  7) updateCoverUrl：把 url 写回课程数据
  v
Response：返回更新后的 Course（包含 coverUrl）
```

任务项（建议按顺序完成）：

- 任务项：设计 objectKey 规则
  - 要求：能区分课程、避免重名覆盖、尽量可读
  - 建议：`courses/{courseId}/cover/{uuid}-{filename}`
- 任务项：完成上传后的“回写”
  - 要求：上传成功之后，课程对象里能看到 `coverUrl`
- 任务项：思考返回值
  - 要求：接口响应中至少包含 `coverUrl`，便于前端直接展示

完成标准（用 curl 或 Apifox 验证）：

- 上传成功返回 200
- 返回体中 `data.coverUrl` 是一个可访问的链接
- 用浏览器打开 `coverUrl` 能看到图片（在有效期内）

常见错误与定位方式：

- 404 + “No static resource api/courses/cover”
  - 原因：调用了错误的路径（缺少 `{id}`）
  - 正确路径：`/api/courses/1/cover`（示例）
- 500/503 + “Connection refused”
  - 原因：MinIO 未启动或 endpoint 配错
  - 先检查：MinIO 是否在本机 9000 端口正常监听；再检查 `app.minio.endpoint`
- 返回的 `coverUrl` 打不开
  - 重点检查：`app.minio.public-endpoint` 是否为浏览器可访问地址（通常是 `localhost:9000`）

---

## 第 4 步：理解“私有桶 + 预签名 URL”的临时授权访问

目标：

- 让桶保持“默认不公开”（更符合真实项目的安全边界）
- 让前端依然能访问文件：通过“预签名 URL”实现临时授权

要点说明：

- 私有桶：不允许匿名访问；没有签名/凭证就会被拒绝
- 预签名 URL：由后端生成的一条“带签名 + 带有效期 + 限定方法”的访问链接
  - 带签名：MinIO 能验证“这条链接确实是可信系统生成的”
  - 带有效期：过期自动失效
  - 限定方法：可限定为 GET（访问）或 PUT（上传）

讨论点：

- 为什么不把 accessKey/secretKey 发给前端？（密钥泄漏风险）
- 为什么不把桶设成公开？（长期暴露、不可控传播）
- 预签名 URL 的有效期应该多长？（安全 vs 体验的折中）

---

## 第 5 步：实现 SSE（流式推送）订阅与事件发布

目标：

- 客户端订阅某个课程的事件流
- 当课程发生变化时（新增/更新/删除/学习人数变化/封面上传完成），服务端主动推送事件给订阅者

接口验收口径：

- 路径：`GET /api/courses/{id}/events`
- 响应类型：`text/event-stream`

SSE 流程图（理解顺序即可）：

```text
Client
  │  GET /api/courses/{id}/events
  v
CourseController
  │  1) 校验 courseId 存在
  │  2) 调用 CourseSseService.subscribe(courseId)
  v
CourseSseService（维护 emitter 列表）
  │  3) 为该 courseId 注册 emitter（一个订阅者对应一个 emitter）
  │  4) 连接断开/超时/异常时移除 emitter（避免泄漏）
  v
后续业务动作发生时
  │  5) CourseServiceImpl.publish(courseId, type, data)
  v
Client 持续收到事件（不需要轮询）
```

任务项（建议按顺序完成）：

- 任务项：定义事件的最小结构
  - 建议包含：`courseId`、`type`、`data`、`timestamp`
- 任务项：明确“什么时候发布事件”
  - 建议覆盖：create/update/delete、学习人数 +1、封面上传完成（coverUrl 更新）
- 任务项：保证服务端不会因为某个客户端断开而影响其他客户端
  - 思路：推送失败则清理该 emitter，其他 emitter 继续推送

完成标准（建议用两个终端/两个客户端验证）：

- 客户端 A 订阅：持续保持连接不退出
- 客户端 B 执行业务操作（例如学习人数 +1 或上传封面）
- 客户端 A 能实时看到事件推送（无需刷新、无需轮询）

常见错误与定位方式：

- 订阅能连上但没有任何事件
  - 检查：业务动作是否真的触发了 publish
  - 检查：订阅的是不是同一个 courseId
- 连接容易断
  - 检查：是否有代理/网关超时
  - 检查：是否配置了过短的超时策略

---

## 附：建议的验收清单

- Docker 侧
  - MinIO 9000/9001 可访问，health ready 返回 200
  - bucket 已创建（可在 MinIO Console 看到）
- 上传侧
  - `POST /api/courses/1/cover` 上传成功
  - 返回 `coverUrl` 可访问（在有效期内）
- SSE 侧
  - `GET /api/courses/1/events` 可建立连接
  - 执行一次课程更新/学习人数 +1/封面上传，订阅端能收到事件

