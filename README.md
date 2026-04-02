# 实践教程4：基于Spring Boot 的 RESTful API 与数据校验

## 📖 业务背景

欢迎来到“智学云在线学习平台”的第四次实践。本次实践的重点是：把原本只能在控制台运行的功能，升级为可以对外提供 Web 接口的服务，让前端/第三方系统可以通过 HTTP 调用我们的课程管理能力。

为什么选择 Spring Boot？

- **内嵌 Web 服务器**：无需单独部署 Tomcat/Jetty，直接运行即可提供 HTTP 服务。
- **自动配置**：基于约定优于配置，减少大量 Spring XML/繁琐配置工作。
- **Starter 依赖**：按功能引入依赖（如 Web、Validation），避免手动拼装依赖版本。
- **更贴近真实项目**：用主流方式构建 RESTful API、统一响应、参数校验与异常处理。

本项目刻意 **不引入数据库**：为了让同学把注意力集中在“控制层如何对外提供接口”和“参数校验/异常处理”上，数据层使用 **内存仓库 + 启动初始化数据** 来替代数据库。

## 🎯 学习目标

- 掌握 Spring Boot 项目结构与启动方式
- 理解 RESTful API 的路径设计与 HTTP 方法语义（GET/POST/PUT/DELETE/PATCH）
- 会在 Controller 层接收并解析请求参数（PathVariable / RequestParam / RequestBody）
- 会使用 Validation 对请求体做数据校验，并返回可读的错误信息
- 会设计统一的 API 响应格式，并用全局异常处理器兜底

## ✅ 你将完成的功能

围绕“课程（Course）”资源提供一组 RESTful 接口：

- 查询全部课程：`GET /api/courses`
- 按 id 查询课程：`GET /api/courses/{id}`
- 按关键词搜索课程：`GET /api/courses/search?keyword=Java`
- 新增课程：`POST /api/courses`
- 更新课程：`PUT /api/courses/{id}`
- 删除课程：`DELETE /api/courses/{id}`
- 学习人数 +1：`PATCH /api/courses/{id}/students`

## 🧭 推荐完成顺序（一步步做）

下面是按实践节奏拆解的步骤。每一步都尽量说明“要做什么、为什么做、做完如何验证”，避免直接贴大段代码。

在开始动手之前，先对项目结构有一个整体认识（后续每一步新增的类，基本都会落在对应的目录里）：

```
cloud-learn-exp04/

├── pom.xml
└── src/
    ├── main/
    │   ├── java/cs/sbs/web/
    │   │   ├── config/                 # 启动初始化数据等配置类
    │   │   │   └── CourseDataInitializer.java
    │   │   ├── controller/             # 控制层：对外提供 Web 接口（路径、方法、参数、返回）
    │   │   │   └── CourseController.java
    │   │   ├── dto/                    # DTO：请求体/统一响应（含参数校验）
    │   │   │   ├── ApiResponse.java
    │   │   │   └── CourseDTO.java
    │   │   ├── entity/                 # 领域实体（普通 Java 对象，不涉及数据库映射）
    │   │   │   └── Course.java
    │   │   ├── exception/              # 异常类型与全局异常处理
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── ResourceNotFoundException.java
    │   │   ├── repository/             # 数据访问层（本实践使用内存实现）
    │   │   │   ├── CourseRepository.java
    │   │   │   └── InMemoryCourseRepository.java
    │   │   ├── service/                # 业务动作的聚合与编排
    │   │   │   ├── CourseService.java
    │   │   │   └── CourseServiceImpl.java
    │   │   └── Application.java        # Spring Boot 启动类
    │   └── resources/                  # 资源目录
    │       └── application.yml
    └── test/
        └── java/cs/sbs/web/
            └── controller/
                └── CourseControllerTest.java
```

### IDEA 与 Apifox 调试指引

- 下载[Apifox](https://apifox.com/)，并在IDEA中安装相关插件，确保本地服务访问与调试更顺畅。请参考 Apifox.md 中的指引。


### 第 1 步：创建 Spring Boot 项目骨架

任务项：

- 基于 Spring Initializer 生成项目，设置项目名 `cloud-learn-exp04`。
- 选择 Spring Boot 3.5+，Java 17+。
- 添加 Web 与 Validation 相关依赖（用于 REST API 与参数校验）。

完成标准：

- 能成功编译打包：`mvn -DskipTests package`
- 能启动并监听 8080 端口：`mvn spring-boot:run` / 使用IDEA直接运行 `Application` 类

提示：

- 本项目使用 `org.springframework.boot:spring-boot-starter-parent` 来统一管理依赖版本。

### 第 2 步：写一个最小可运行的启动类

任务项：

- 创建应用入口类（带 `@SpringBootApplication`）。
- 保证应用启动后控制台能看到启动成功信息（便于同学确认“我已跑起来了”）。

完成标准：

- 运行后浏览器访问 `http://localhost:8080/` 不再是连接失败（即服务已启动）。

### 第 3 步：定义 Course 领域对象（内存版）

任务项：

- 创建 `Course` 类，包含课程常用字段（如标题、讲师、价格、时长、学习人数、创建时间等）。
- 这里不使用 JPA 注解、不映射数据库表，保持它是一个普通 Java 对象。

完成标准：

- 后续接口能返回 Course 的 JSON 数据。

### 第 4 步：用 DTO 承载请求体，并添加校验规则

任务项：

- 创建 `CourseDTO` 用于接收“新增/更新课程”的请求体。
- 在 DTO 字段上添加校验注解（例如：标题不能为空、价格必须大于 0、时长至少 1 小时等）。
- 在 Controller 的入参上使用 `@Valid` 触发校验。

完成标准：

- 当请求体不合法时，接口能返回 **400**，并给出字段级错误提示。

### 第 5 步：设计统一 API 响应格式

任务项：

- 创建 `ApiResponse<T>`：统一包含 `success/message/data/timestamp/errorCode` 等字段。
- Controller 正常返回时统一包装成 `ApiResponse.success(...)`。

完成标准：

- 无论查询/新增/更新/删除，返回结构一致，便于前端处理。

### 第 6 步：实现内存仓库（替代数据库）

任务项：

- 定义 `CourseRepository` 接口，表达本实践需要的最小数据操作能力（查全部、按 id 查、保存、删除、按标题模糊查等）。
- 写一个 `InMemoryCourseRepository` 实现类：内部用 Map 保存数据，用自增 id 生成新课程 id。

完成标准：

- 新增后能在“查询全部”里看到新增的数据。
- 删除后再次查询该 id 能返回不存在。

### 第 7 步：在启动时初始化一些课程数据

任务项：

- 写一个启动执行器（如 `ApplicationRunner`），在应用启动后向内存仓库塞入几条课程数据。
- 这样同学一启动就能立刻调用查询接口看到数据，不需要先手动新增。

完成标准：

- 启动后调用 `GET /api/courses` 返回至少 8 条课程。

### 第 8 步：实现 Service + Controller（本次核心）

任务项（Service 层）：

- 把业务动作放到 Service：查列表、查详情、搜索、新增、更新、删除、学习人数 +1。
- Service 里要处理“资源不存在”的情况（例如：更新/删除/查询一个不存在的 id）。

任务项（Controller 层，重点体会）：

- 使用 `@RestController` + `@RequestMapping("/api/courses")` 定义资源根路径。
- 用不同注解映射 HTTP 方法：
  - `@GetMapping`：查询
  - `@PostMapping`：新增
  - `@PutMapping`：整体更新
  - `@DeleteMapping`：删除
  - `@PatchMapping`：局部更新（学习人数 +1）
- 熟悉三类入参来源：
  - 路径参数：`/api/courses/{id}` → `@PathVariable`
  - 查询参数：`?keyword=Java` → `@RequestParam`
  - JSON 请求体：POST/PUT body → `@RequestBody` + `@Valid`

完成标准：

- 能通过浏览器/curl/Postman 正确调用所有接口，返回结构统一。

### 第 9 步：加入全局异常处理（让接口更“像真实项目”）

任务项：

- 用 `@RestControllerAdvice` 统一处理：
  - 参数校验失败（`MethodArgumentNotValidException`）：返回 400 + 字段错误 Map
  - 资源不存在：返回 404
  - 其他异常：返回 500
- 让 Controller 更专注于“请求 → 调用 service → 返回结果”，异常交给统一处理器兜底。

完成标准：

- 传入非法 JSON 时返回 400，且 body 中包含字段错误。
- 访问不存在的课程 id 时返回 404。

## 🧪 快速验证（建议同学逐步验证）

启动：

```bash
mvn spring-boot:run
```

查询全部课程：

```bash
curl http://localhost:8080/api/courses
```

查询单个课程：

```bash
curl http://localhost:8080/api/courses/1
```

触发校验失败（看 400 和字段错误）：

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "instructor": "Test",
    "price": -10,
    "duration": -5
  }'
```

## 🚫 本实践刻意不做的事情

- 不接入数据库（不写 JPA、不写 DataSource、不写 SQL 初始化脚本）
- 不引入复杂的鉴权/网关/分布式组件

目标是让同学把注意力聚焦在：**Controller 设计、RESTful 风格、参数校验、统一响应与异常处理** 上。
