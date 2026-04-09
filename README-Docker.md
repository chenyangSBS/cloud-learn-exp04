# Docker & Docker Compose 安装指引（Windows / macOS）

本指引用于让同学在本机安装并验证 Docker 与 Docker Compose，并用于运行本项目的 MinIO（docker-compose.yml）。

## ✅ 安装完成的验收标准

- 终端执行 `docker version` 能输出版本信息
- 终端执行 `docker compose version` 能输出版本信息（注意：是 `docker compose`，不是 `docker-compose`）
- 终端执行 `docker run --rm hello-world` 能成功运行并退出

---

## Windows（推荐：Docker Desktop + WSL2）

### 任务项：准备环境

- 确认电脑已开启虚拟化（BIOS/UEFI 中常见选项：Intel VT-x / AMD-V）
- Windows 版本建议 Windows 10/11（专业版/家庭版均可）

### 任务项：安装 Docker Desktop

- 访问 Docker 官方下载页，下载安装 Docker Desktop（Windows 版）：https://www.docker.com/products/docker-desktop/
- 安装过程中勾选/启用 WSL2 相关选项（如安装器提示）

### 任务项：启用/安装 WSL2（如系统未启用）

- 以管理员身份打开 PowerShell，执行：

```powershell
wsl --install
```

- 安装完成后重启电脑（如提示）

### 任务项：启动并完成首次配置

- 打开 Docker Desktop，等待状态变为 Running
- Docker Desktop Settings 中确保启用：
  - Use the WSL 2 based engine

### 任务项：验证安装

在 PowerShell / Windows Terminal 执行：

```powershell
docker version
docker compose version
docker run --rm hello-world
```

---

## macOS（推荐：Docker Desktop）

### 任务项：安装 Docker Desktop

- 访问 Docker 官方下载页，下载安装 Docker Desktop（macOS 版）：https://www.docker.com/products/docker-desktop/
- 根据你的芯片选择安装包：
  - Apple Silicon（M1/M2/M3…）
  - Intel

### 任务项：启动并授权

- 打开 Docker Desktop，按提示完成权限授权与首次启动
- 等待状态显示 Docker is running

### 任务项：验证安装

在 Terminal 执行：

```bash
docker version
docker compose version
docker run --rm hello-world
```

---

## 用 Docker Compose 运行本项目的 MinIO

### 任务项：启动 MinIO

在本项目目录（包含 `docker-compose.yml`）执行：

```bash
docker compose up -d
```

### 任务项：确认容器运行状态

```bash
docker compose ps
```

### 任务项：访问地址

- MinIO API：http://localhost:9000
- MinIO Console：http://localhost:9001
- 默认账号/密码：minioadmin / minioadmin

### 任务项：停止与清理

```bash
docker compose down
```

如果需要同时删除数据卷（会清空 MinIO 存储的数据）：

```bash
docker compose down -v
```

---

## 常见问题（快速排查）

### Windows：Docker Desktop 启动失败或提示 WSL 问题

- 确认 Windows 已启用 WSL2：`wsl -l -v`
- 重启 Docker Desktop 与电脑
- 确认 BIOS 虚拟化已开启

### macOS：docker 命令找不到

- 确认 Docker Desktop 已运行
- 重新打开终端，或重启电脑后再试

