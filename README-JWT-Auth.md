# SpringBoot进阶实践：用 Spring Security + JWT 实现登录与接口鉴权（含角色权限区分）

在基础项目 `cloud-learn-exp04` 的之上，我们聚焦一件事：用 **JWT（JSON Web Token）** 做认证与授权，让接口具备“登录后才能访问”、并且能区分 **ADMIN / STUDENT** 的访问权限。

本文件用于课堂练习指导，重点是做什么、为什么、怎么验证、以及如何排错。

---

## 学习目标

- 理解“认证 Authentication”和“授权 Authorization”的区别
- 理解 Spring Security 在 Web 请求链路中的位置：FilterChain → SecurityContext
- 掌握 JWT 的最小结构与生命周期：签发、携带、验签、过期
- 能实现“用户名密码登录 → 返回 Token → 携带 Bearer Token 访问接口”
- 能为不同业务接口设置不同角色权限（ADMIN/STUDENT）
- 能区分并解释常见结果：401（未认证/Token 无效）与 403（已认证但无权限）

---

## 项目结构（只关注与本次实践内容相关的部分）

- `src/main/java/cs/sbs/web/controller/`
  - `AuthController`：登录接口（用户名密码 → 颁发 Token）
  - `CourseController`：演示“不同接口不同角色权限”的业务接口
- `src/main/java/cs/sbs/web/config/`
  - `SecurityConfig`：SecurityFilterChain 配置（禁用 session、放行登录、注册 JWT 过滤器、角色权限规则）
  - `JwtAuthenticationFilter`：从请求头解析 Bearer Token，并把认证信息写入 SecurityContext
  - `JwtProperties`：JWT 配置属性绑定（`app.security.jwt`）
- `src/main/java/cs/sbs/web/service/`
  - `JwtService`：JWT 签发与解析的抽象
- `src/main/java/cs/sbs/web/service/impl/`
  - `JwtServiceImpl`：JWT 具体实现（HS256 签名、roles 写入 claim、验签与解析）
- `src/main/java/cs/sbs/web/dto/`
  - `LoginRequest`：登录请求体
  - `TokenResponse`：登录响应体（tokenType/accessToken/expiresAt）
- `src/main/resources/application.yml`
  - `app.security.jwt.*`：issuer/secret/ttl 配置
- `src/test/java/cs/sbs/web/controller/`
  - `CourseControllerTest`：演示“先登录拿 token，再访问受保护接口”的测试方式

---

## 第 0 步：准备环境与启动项目

任务项：

- 确保 Java 与 Maven 可用，并能启动本项目
- 理解本项目使用“内存用户”做演示，不依赖数据库

完成标准：

- `mvn test` 能通过
- 项目启动后，访问任何受保护接口时都会要求先登录并携带 Token

---

## 第 1 步：认识 JWT 认证链路（你要实现的是什么）

目标：

- 客户端登录一次拿到 Token
- 后续每次请求都在请求头携带 Token
- 服务端对 Token 验签并解析出“当前用户是谁、有什么角色”，然后决定是否放行请求

JWT 认证链路图（理解顺序即可）：

```text
Client
  │  1) POST /api/auth/login  (username/password)
  v
AuthController
  │  2) 交给 AuthenticationManager 校验用户名密码
  │  3) 校验通过后调用 JwtService.issueToken(...) 生成 JWT
  v
Response
  │  4) 返回 accessToken + expiresAt
  v
Client
  │  5) 后续请求都带：Authorization: Bearer <accessToken>
  v
JwtAuthenticationFilter
  │  6) 解析 header，取出 token
  │  7) JwtService.parseToken(...) 验签 + 解析 claims
  │  8) 写入 SecurityContext（告诉 Spring：当前请求已认证）
  v
Security（授权判断）
  │  9) 根据角色与规则判断是否允许访问某个接口
  v
Controller
```

---

## 第 2 步：配置 JWT 参数（application.yml → 配置类）

任务项：

- 在 `application.yml` 中确认存在 `app.security.jwt.*` 配置项
- 理解这三个参数分别影响什么：issuer / secret / access-token-ttl-seconds

完成标准：

- 你能解释：为什么 issuer 要校验、为什么 secret 不能太短、ttl 如何影响“过期后 401”

关键配置解释：

- `app.security.jwt.issuer`
  - 语义：Token 的签发方标识；解析 Token 时要求 issuer 必须一致，防止“别人系统的 token”被误当成你的
- `app.security.jwt.secret`
  - 语义：对称密钥（HS256）签名用；后端签发与验签必须一致
  - 约束：过短会导致启动或签名时报错（HS256 需要足够强度的 key）
  - 安全提示：生产环境必须用环境变量注入，避免写死在仓库中
- `app.security.jwt.access-token-ttl-seconds`
  - 语义：accessToken 的有效期（秒）
  - 结果：超过有效期后，带旧 token 访问会被判定为未认证（401）

---

## 第 3 步：准备“演示用用户体系”（内存用户 + 角色）

目标：

- 为课堂演示准备两个用户，并且角色不同：
  - STUDENT：模拟学生端
  - ADMIN：模拟后台管理端

任务项：

- 在安全配置中定义两个内存用户（InMemoryUserDetailsManager）
- 明确每个用户的角色是什么，后续权限规则就基于这些角色生效

完成标准：

- 你能用同一套登录接口分别登录 student 与 admin，并拿到两种 token
- 你能解释：token 里为什么要包含角色（roles claim），不包含会发生什么

理解要点：

- Spring Security 的“角色”本质上是 `GrantedAuthority`
- 使用 `roles("STUDENT")` 的用户，实际权限名会带 `ROLE_` 前缀（例如 `ROLE_STUDENT`）
- 本项目把“角色列表”写进 JWT 的 `roles` claim，并在解析时还原为 authorities

---

## 第 4 步：实现登录接口（用户名密码 → 颁发 Token）

目标：

- 提供登录端点：`POST /api/auth/login`
- 入参：JSON，包含 username/password
- 出参：返回 `tokenType/accessToken/expiresAt`

任务项：

- 登录时做请求体校验（用户名与密码不能为空）
- 调用 AuthenticationManager 做用户名密码认证
- 认证成功后签发 token；认证失败返回 401（用户名或密码错误）

完成标准（用 Apifox 或任意 HTTP 客户端验证）：

- 用正确账号密码登录返回 200，并能从响应中拿到 accessToken
- 用错误密码登录返回 401

排错要点：

- 登录一直 401：先确认用户名密码是否和“内存用户”的配置一致
- 登录 400：说明请求体字段缺失或为空，触发了校验

---

## 第 5 步：实现 JwtService（签发与解析的边界）

目标：

- 把 JWT 的“生成/解析”从 Controller 与 Filter 中剥离出来，形成独立服务
- 签发时写入最关键的字段：issuer、subject、iat、exp、roles
- 解析时做两件事：验签 + 校验 issuer + 读取 subject/roles 组装 Authentication

任务项：

- 签发 token：从当前 Authentication 中拿到用户名与角色列表，写入 claim
- 解析 token：验签成功后，把 subject 作为“当前用户标识”，把 roles 还原为 authorities

完成标准：

- 登录拿到 token 后，服务端能从 token 里解析出 username 与 roles
- token 被篡改、issuer 不匹配、过期等情况都必须判定为无效

理解要点：

- 本项目使用 HS256：对称密钥签名，签发与验签共享同一 secret
- roles 以字符串列表形式存入 claim；解析时逐个转换为 `GrantedAuthority`

---

## 第 6 步：实现 JWT 过滤器（把 token 变成“当前请求已登录”）

目标：

- 对每个请求检查 `Authorization` 请求头
- 符合 `Bearer <token>` 格式才尝试解析
- 解析成功则把 Authentication 放入 SecurityContext，让后续授权判断可用

任务项：

- 处理三类情况：
  - 没有 Authorization：不做任何事，交给后面的规则决定是否需要认证
  - 有 Authorization 但格式不对/为空：当作未携带 token
  - 有 token 但解析失败：返回 401（未认证）

完成标准：

- 不带 token 访问受保护接口：401
- 带合法 token 访问受保护接口：可以进入 Controller
- 带非法 token（随便改几个字符）访问：401

说明：

- 当前实现对“token 解析失败”会直接返回 401 状态码，不额外输出统一响应体；这在课堂上便于观察“Filter 层提前拦截”的效果

---

## 第 7 步：配置 SecurityFilterChain（无 Session + 放行登录 + 注入过滤器）

目标：

- 关闭 CSRF（本项目 API 以 token 鉴权为主）
- 关闭 session（Stateless），每次请求都基于 token 认证
- 放行登录端点与 swagger 相关资源
- 注册 JwtAuthenticationFilter 到合适的位置

任务项：

- 明确“哪些路径无需登录”：例如登录接口、API 文档、错误页等
- 除放行路径以外的接口，都要求“先通过认证”，再进入 Controller

完成标准：

- `POST /api/auth/login` 无需携带 token 也能访问
- `GET /api/courses` 不带 token 会被拦截
- 携带 token 后可访问受保护接口

---

## 第 8 步：实现“不同角色访问不同接口”（ADMIN / STUDENT）

目标：

- 把“谁能做什么”清晰落在代码里，学生能直接看到权限规则
- 同一个 Controller 中，不同方法对应不同角色权限

本项目演示规则（你应能解释原因）：

- ADMIN 与 STUDENT 都可访问（读、订阅、学习行为）
  - 查询课程列表、按 id 查询、搜索、订阅 SSE、学习人数 +1
- 仅 ADMIN 可访问（写、改、删、上传）
  - 新建课程、更新课程、删除课程、上传文件、上传课程封面

任务项：

- 在 Controller 方法上声明权限（用于直观教学）
- 在 SecurityConfig 中也配置相同的路径级授权规则（用于保证“先鉴权再校验”）

完成标准：

- STUDENT 登录后访问“只允许 ADMIN”的接口，返回 403
- ADMIN 登录后能正常访问写接口
- STUDENT/ADMIN 都能正常访问读接口

排错要点（401 vs 403 快速判断）：

- 401：你没登录，或 token 无效/过期，服务端无法确认“你是谁”
- 403：你已登录，服务端知道“你是谁”，但你没有访问这个接口的权限

---

## 第 9 步：测试验收（推荐把“先登录再访问”写进测试）

目标：

- 用自动化测试固化 JWT 鉴权流程，避免后续改动把安全链路改坏

任务项：

- 测试中先调用登录接口拿到 token
- 访问受保护接口时带上 `Authorization: Bearer <token>`
- 覆盖至少两类断言：
  - 正常访问：200
  - 越权访问：403

完成标准：

- `mvn test` 全绿

---

## 附：建议的验收清单

- 登录侧
  - student / admin 都能登录成功并拿到 token
  - 密码错误时返回 401
- 鉴权侧
  - 不带 token 访问受保护接口返回 401
  - token 被篡改返回 401
  - token 过期返回 401
- 授权侧
  - STUDENT 调用 ADMIN-only 接口返回 403
  - ADMIN 能调用写接口；ADMIN/STUDENT 都能调用读接口

---

## 常见错误与排错要点

- 现象：启动时报“secret 太短/非法 key”类似错误
  - 排查：检查 `app.security.jwt.secret` 是否足够长；HS256 需要足够强度的 key
- 现象：登录成功，但带 token 访问仍然 401
  - 排查：确认请求头格式必须是 `Authorization: Bearer <token>`（Bearer 后有空格）
  - 排查：确认 token 没有换行、没有被复制截断
  - 排查：确认 issuer 与后端配置一致（issuer 不匹配会被拒绝）
- 现象：能访问读接口，但访问写接口返回 403
  - 解释：已认证但无权限；确认使用的是 admin 账号登录
- 现象：一段时间后都开始 401
  - 解释：token 过期；重新登录获取新 token，或调大 ttl（权衡安全与体验）

