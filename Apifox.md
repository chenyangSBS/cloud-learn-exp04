在IDEA里把Spring控制器（Controller）层接口**自动导入Apifox**，核心是用官方**Apifox Helper**插件，一键解析注解、同步到Apifox项目，零代码侵入。下面是完整步骤+常见问题：

### 一、安装IDEA插件（Apifox Helper）
1. 打开IDEA → **File → Settings → Plugins**（Mac：Preferences → Plugins）
2. 切换到**Marketplace**，搜索 **Apifox Helper**，点击**Install**，安装后**重启IDEA**
3. 确认插件已启用（Plugins → Installed里能看到）

### 二、获取Apifox的Token与项目ID（关键配置）
#### 1. 获取个人访问令牌（Token）
- 打开Apifox网页/客户端 → 右上角**头像 → 账号设置 → API访问令牌**
- 点击**新建令牌**，设置权限（建议全选）、有效期，**复制生成的Token**（只显示一次，妥善保存）

#### 2. 获取目标项目ID
- 进入Apifox里要导入的项目 → 左侧**项目设置 → 基本设置**
- 复制**项目ID**（一串数字）

### 三、IDEA插件配置（关联Apifox）
1. IDEA → **Settings → Apifox Helper**（安装后才出现）
2. 填写3项核心配置：
   - **Apifox服务器地址**：默认 `https://api.apifox.cn`（SaaS版不用改）
   - **个人访问令牌**：粘贴刚才复制的Token → 点**测试令牌**，显示「成功」
   - **模块项目ID配置**：格式 `模块名:项目ID[,目录名]`
     示例（单模块、导入到根目录）：
     ```
     springboot-demo:1234567
     ```
     示例（多模块、指定目录）：
     ```
     admin:1234567,后台接口
     api:1234567,开放接口
     ```
3. 点击**Apply → OK**保存配置

### 四、一键同步Controller接口到Apifox（3种方式）
#### 方式1：同步单个Controller文件（最常用）
- 打开你的Controller（如UserController.java）
- 编辑区**右键 → Upload to Apifox**
- 等待解析完成，弹出「上传成功」提示

#### 方式2：同步整个模块所有Controller
- 左侧Project面板，右键点击**模块根目录**（如src/main/java下的模块）
- 选择 **Upload to Apifox** → 自动扫描该模块下所有Controller并同步

#### 方式3：同步选中的部分接口
- 在Controller里选中**多个接口方法** → 右键 → **Upload to Apifox** → 仅同步选中接口

### 五、验证与效果
1. 回到Apifox对应项目 → 刷新接口列表
2. 自动生成：
   - 接口路径、请求方法（GET/POST/PUT/DELETE）
   - 请求参数、响应结构（从`@GetMapping`/`@PostMapping`/`@RequestBody`/`@RequestParam`等注解解析）
   - 接口描述（从Javadoc、`@ApiOperation`/`@Schema`等Swagger注解读取）

### 六、支持的注解与框架（自动解析）
- Spring Boot/Spring MVC：`@RestController`/`@Controller`、`@RequestMapping`/`@GetMapping`/`@PostMapping`、`@RequestParam`/`@PathVariable`/`@RequestBody`、`@ResponseStatus`
- Swagger/OpenAPI 3：`@Operation`、`@Parameter`、`@Schema`、`@ApiResponse`
- JSR-380校验：`@NotNull`/`@NotBlank`/`@Size`等（自动生成参数校验规则）

### 七、常见问题与排查
1. 上传失败/令牌无效
   - 检查Token是否复制完整、未过期、权限足够
   - 确认项目ID正确、服务器地址无误（SaaS默认https://api.apifox.cn）
2. 接口没解析出来
   - 确保Controller用`@RestController`/`@Controller`+`@RequestMapping`
   - 接口方法必须有`@GetMapping`/`@PostMapping`等请求注解
   - 重启IDEA、重新配置插件
3. 重复上传/覆盖
   - 插件会**按接口路径+方法**匹配，已存在则更新，不存在则新增
   - 可在Apifox里手动整理目录
