# SpringBoot进阶实践：用 Spring Security Basic Auth 实现接口保护（含角色权限区分）

在基础项目 `cloud-learn-exp04` 的之上，我们聚焦一件事：用 **Spring Security 的 Basic Auth** 为 API 加上认证与授权能力，让接口具备“需要登录才能访问”，并且能区分 **ADMIN / STUDENT** 的访问权限。

本文件用于课堂练习指导，重点是做什么、为什么、怎么验证、以及如何排错。

---

## 学习目标

- 理解“认证 Authentication”和“授权 Authorization”的区别
- 理解 Basic Auth 的工作方式：每次请求携带用户名密码（HTTP Header）
- 理解 Spring Security 的核心位置：SecurityFilterChain 负责拦截与放行
- 掌握“无 Session（Stateless）”API 的典型配置方式
- 能为不同业务接口设置不同角色权限（ADMIN/STUDENT）
- 能区分并解释常见结果：401（未认证/凭证错误）与 403（已认证但无权限）

---

## 项目结构（只关注与本次实践内容相关的部分）

- `src/main/java/cs/sbs/web/config/`
  - `SecurityConfig`：SecurityFilterChain 配置（关闭 CSRF、无 Session、Basic Auth、角色权限规则、内存用户）
- `src/main/java/cs/sbs/web/controller/`
  - `CourseController`：演示“不同接口不同角色权限”的业务接口（方法级权限声明）
- `src/test/java/cs/sbs/web/controller/`
  - `CourseControllerTest`：演示“请求带 Basic Auth 凭证”访问接口的测试方式

---

## 第 0 步：准备环境与启动项目

任务项：

- 确保 Java 与 Maven 可用，并能启动本项目
- 理解本项目使用“内存用户”做演示，不依赖数据库

完成标准：

- `mvn test` 能通过
- 项目启动后，访问受保护接口时会要求提供 Basic Auth 凭证

---

## 第 1 步：认识 Basic Auth 的认证链路（你要实现的是什么）

目标：

- 客户端每次请求都携带用户名密码
- 服务端验证用户名密码正确后，把“当前用户是谁、有什么角色”放入当前请求上下文
- 再根据接口的角色规则判断是否放行

Basic Auth 链路图（理解顺序即可）：

```text
Client
  │  1) 发起请求（例如 GET /api/courses）
  │  2) 请求头携带 Authorization: Basic ...
  v
Spring Security FilterChain
  │  3) BasicAuthenticationFilter 解析凭证
  │  4) 交给 AuthenticationManager 验证用户名密码
  │  5) 验证成功后写入 SecurityContext（当前请求已认证）
  v
授权判断（Authorization）
  │  6) 根据角色规则判断：是否允许访问该接口
  v
Controller
```

理解要点：

- Basic Auth 不需要“登录接口”，也不需要服务器保存 session；它依赖“每次请求都带凭证”
- Basic Auth 的凭证是可以被截获/重放的，所以真实项目必须配合 HTTPS 使用

---

## 第 2 步：准备“演示用用户体系”（内存用户 + 角色）

目标：

- 为课堂演示准备两个用户，并且角色不同：
  - STUDENT：模拟学生端
  - ADMIN：模拟后台管理端

任务项：

- 在 `SecurityConfig` 中定义两个内存用户（InMemoryUserDetailsManager）
- 为用户设置角色（roles），后续权限规则将基于角色生效
- 使用 PasswordEncoder 对密码进行编码（演示“存储的不是明文密码”的正确姿势）

完成标准：

- 你能清楚说出有哪些演示账号、分别是什么角色
- 你能解释：为什么 Spring Security 需要 PasswordEncoder，为什么不建议明文密码

理解要点：

- 使用 `roles("STUDENT")` 时，实际权限名会带 `ROLE_` 前缀（例如 `ROLE_STUDENT`）
- 后续权限判断中使用 `hasRole("STUDENT")` / `hasAnyRole(...)` 时，Spring 会自动处理 `ROLE_` 前缀

---

## 第 3 步：配置 SecurityFilterChain（无 Session + Basic Auth + 放行白名单）

目标：

- 关闭 CSRF（本项目是 API 场景为主，不使用表单与浏览器 cookie）
- 关闭 Session（Stateless），避免服务器保存登录态
- 启用 Basic Auth（让 Spring Security 自动完成凭证解析与认证）
- 对 swagger / error 等路径做放行（便于开发与调试）

任务项：

- 找到 `SecurityConfig` 的 `SecurityFilterChain` 配置入口
- 明确“哪些路径无需认证”：例如 swagger 相关接口与错误页
- 除放行路径以外的接口都要求认证

完成标准：

- 不带凭证访问受保护接口：返回 401
- 带正确凭证访问受保护接口：可以进入 Controller
- 带错误密码访问：返回 401

补充理解（401 的表现）：

- 401 往往会带 `WWW-Authenticate: Basic`，浏览器可能会弹出用户名密码输入框

---

## 第 4 步：实现“不同角色访问不同接口”（ADMIN / STUDENT）

目标：

- 把“谁能做什么”清晰落在代码里，学生能直接看到权限规则
- 同一个 Controller 中，不同方法对应不同角色权限

本项目演示规则（你应能解释原因）：

- ADMIN 与 STUDENT 都可访问（读、订阅、学习行为）
  - 查询课程列表、按 id 查询、搜索、订阅 SSE、学习人数 +1
- 仅 ADMIN 可访问（写、改、删、上传）
  - 新建课程、更新课程、删除课程、上传文件、上传课程封面

任务项：

- 在 `SecurityConfig` 中配置基于“请求路径 + HTTP Method”的授权规则
- 在 `CourseController` 中用方法级权限声明，让权限语义更直观（配合 `@EnableMethodSecurity` 生效）

完成标准：

- STUDENT 访问“只允许 ADMIN”的接口：返回 403
- ADMIN 能正常访问写接口
- ADMIN / STUDENT 都能正常访问读接口

排错要点（401 vs 403 快速判断）：

- 401：你没提供凭证，或凭证错误，服务端无法确认“你是谁”
- 403：你已通过认证，服务端知道“你是谁”，但你没有访问这个接口的权限

---

## 第 5 步：接口验收（推荐用 Apifox 或 Postman）

目标：

- 在调用端正确配置 Basic Auth
- 用同一套接口分别验证 student 与 admin 的权限差异

任务项：

- 在请求中启用 Basic Auth，并填写用户名/密码
- 用 STUDENT 账号请求“读接口”和“写接口”，观察 200/403 的差异
- 用 ADMIN 账号请求写接口，观察 2xx 结果

完成标准：

- 读接口：student 与 admin 都能访问
- 写接口：只有 admin 能访问

提示：

- 如果使用 curl，可使用“带用户名密码”的方式请求；如果使用浏览器，访问受保护接口会出现 Basic 认证弹窗

---

## 第 6 步：测试验收（推荐把“Basic Auth 凭证”写进测试）

目标：

- 用自动化测试固化 Basic Auth 的访问方式，避免后续改动把安全配置改坏

任务项：

- 测试用例中为请求附加 Basic Auth 凭证（student/admin 两组）
- 覆盖至少两类断言：
  - 正常访问：200
  - 越权访问：403（student 调用 ADMIN-only 接口）

完成标准：

- `mvn test` 全绿

---

## 附：建议的验收清单

- 认证侧
  - 不带凭证访问受保护接口返回 401
  - 密码错误返回 401
- 授权侧
  - STUDENT 调用 ADMIN-only 接口返回 403
  - ADMIN 能调用写接口；ADMIN/STUDENT 都能调用读接口
- 白名单侧
  - swagger 文档相关路径无需认证即可访问

---

## 常见错误与排错要点

- 现象：所有接口都 401
  - 排查：确认请求是否真的带了 Basic Auth；用户名密码是否和内存用户一致
  - 排查：确认调用的是受保护接口还是白名单接口
- 现象：student 能访问读接口，但访问写接口 403
  - 解释：已认证但无权限；这正是角色授权在生效
- 现象：浏览器访问接口弹出输入框但输入后仍失败
  - 排查：确认账号密码正确；注意密码大小写与空格
  - 排查：如果走了代理/网关，确认是否正确透传 Authorization 头

