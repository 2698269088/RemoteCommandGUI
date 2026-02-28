# Remote Command GUI 远程命令图形界面系统

## 📖 项目简介

Remote Command GUI 是一个功能强大的远程命令控制系统，采用Java开发，提供图形界面和命令行两种操作方式。该系统支持通过HTTP、FTP、UDP等多种协议对网络中的客户端设备进行远程管理和控制，实现了跨平台的远程设备管理解决方案。

## 🚀 主要特性

### 🔧 核心功能
- **双模式操作**：支持纯命令行模式和图形界面模式
- **混合模式**：同时支持GUI和控制台操作
- **多协议支持**：HTTP、FTP、UDP三种通信协议
- **广播控制**：支持向局域网内所有设备发送广播命令
- **精确控制**：可向指定IP地址的设备发送定向命令
- **进程管理**：远程查看和管理客户端进程
- **文件传输**：通过FTP协议实现文件上传下载
- **实时日志**：UDP日志接收，实时监控客户端状态
- **安全控制**：程序和路径黑白名单管理

### 🌐 通信协议

#### HTTP服务 (端口: 6742, 80, 21997)
- 提供Web接口访问
- 支持命令文件下载 (`/rc/command.bat`)
- 多端口监听确保兼容性

#### FTP服务 (端口: 6744)
- 支持用户认证 (motrc/pw777888 或 anonymous)
- 文件上传功能
- 自动保存客户端上传的日志文件

#### UDP服务 (发送端口: 6743, 接收端口: 6746)
- 高效的广播通信
- 实时日志接收
- 进程状态监控

## 📋 系统架构

### Java客户端架构
```
Remote Command GUI (Java)
├── src/main/java/top/mcocet/rcgui/
│   ├── HybridApplication.java    # 混合模式主程序
│   ├── Main.java                 # 命令行模式主程序
│   ├── gui/
│   │   ├── GuiLauncher.java      # GUI启动器
│   │   └── MainWindow.java       # 主窗口界面
│   ├── handler/
│   │   └── CommandHandler.java   # 命令处理逻辑
│   ├── network/
│   │   └── NetworkHelper.java    # 网络辅助工具
│   ├── server/
│   │   ├── FtpServer.java        # FTP服务模块
│   │   ├── HttpServerManager.java # HTTP服务模块
│   │   └── UdpServer.java        # UDP服务模块
│   ├── util/
│   │   └── Logger.java           # 日志记录模块
│   └── Main.java                 # 程序入口点
```

### C#服务端架构
```
Remote Command Server (C#)
├── Program.cs              # 主程序入口和命令解析
├── CommandHandler.cs       # 命令处理逻辑
├── HttpServer.cs          # HTTP服务模块
├── FtpServer.cs           # FTP服务模块
├── UdpServer.cs           # UDP服务模块
├── Logger.cs              # 日志记录模块
└── NetworkHelper.cs       # 网络辅助工具
```

## 🔧 安装与部署

### 系统要求
- **Java客户端**：Java 8或更高版本
- **C#服务端**：Windows操作系统，.NET Framework 4.0或更高版本
- 管理员权限（用于网络接口绑定）

### Java客户端编译构建
```bash
# 使用Maven编译项目
mvn clean package

# 生成的JAR文件位置
target/RemoteCommandGUI-console.jar    # 命令行版本
target/RemoteCommandGUI-gui.jar        # GUI版本  
target/RemoteCommandGUI-hybrid.jar     # 混合版本
```

### C#服务端编译构建
```bash
# 使用Visual Studio打开解决方案文件
Remote Command Server.sln

# 或使用命令行编译
msbuild "Remote Command Server.sln" /p:Configuration=Release
```

## 🎮 使用指南

### 启动方式

#### 方式一：混合模式（推荐）
```bash
# 同时启动GUI和命令行界面
java -jar RemoteCommandGUI-hybrid.jar
```

#### 方式二：纯命令行模式
```bash
# 仅启动命令行界面
java -jar RemoteCommandGUI-console.jar
```

#### 方式三：纯GUI模式
```bash
# 仅启动图形界面
java -jar RemoteCommandGUI-gui.jar
```

### 命令行操作

程序启动后会显示命令提示符，支持以下基础命令：

#### 服务控制
```
> start http     # 启动HTTP服务
> start ftp      # 启动FTP服务  
> start udprcv   # 启动UDP日志接收服务
> stop http      # 停止HTTP服务
> stop ftp       # 停止FTP服务
> stop udprcv    # 停止UDP日志接收服务
```

#### 网络命令
```
> broadcast runcmd    # 发送运行命令广播
> broadcast update    # 发送更新命令广播
> cmd [命令]          # 广播命令到所有设备
> cmd [IP] [命令]     # 向指定IP发送命令
> link [IP]          # 发送服务器地址信息
```

#### 进程管理
```
> process list [IP]                    # 查看进程列表
> process app [IP]                     # 查看应用进程
> process taskkill "进程名" [IP]        # 结束指定进程
> process path_black add "路径" [IP]   # 添加黑名单路径
> process path_white add "路径" [IP]   # 添加白名单路径
> process path_mode whitelist [IP]     # 设置白名单模式
> process path_mode blacklist [IP]     # 设置黑名单模式
```

#### 配置管理
```
> restore config [IP]              # 还原所有配置
> disable nic_detection [IP]       # 关闭网卡检测
> disable all_features [IP]        # 关闭所有功能
```

#### 其他命令
```
> network        # 显示网络接口信息
> gui           # 启动图形界面（命令行模式下）
> help          # 显示帮助信息
> quit/exit     # 退出程序
```

### 图形界面操作

GUI界面提供直观的操作面板：
- **服务控制面板**：一键启动/停止各种服务
- **网络管理面板**：查看网络状态和发送命令
- **进程管理面板**：远程进程监控和管理
- **日志显示面板**：实时查看系统日志

### 示例操作流程

1. **启动所有服务**
```bash
> start http
> start ftp
> start udprcv
```

2. **查看所有客户端进程**
```bash
> process list
```

3. **向特定设备发送命令**
```bash
> cmd 192.168.1.100 dir C:\
```

4. **结束远程进程**
```bash
> process taskkill "notepad.exe" 192.168.1.100
```

5. **设置安全策略**
```bash
> process path_black add "C:\Windows\System32\*" 192.168.1.100
> process path_mode blacklist 192.168.1.100
```

## 📊 端口配置

| 协议 | 端口 | 功能 |
|------|------|------|
| HTTP | 6742 | 本地回环访问 |
| HTTP | 80 | 局域网访问 |
| HTTP | 21997 | 备用端口 |
| FTP | 6744 | 文件传输 |
| UDP | 6743 | 命令发送 |
| UDP | 6746 | 日志接收 |

## 📝 日志系统

### 日志文件
- `server.log` - 服务器运行日志
- `logs/` 目录 - 客户端上传的日志文件

### 日志格式
```
[2026-02-28 14:30:25] 已向 192.168.1.100 发送命令: dir C:\
[2026-02-28 14:30:26] [来自 192.168.1.100 的日志]: 命令执行完成
```

## 🔒 安全特性

### 认证机制
- FTP服务支持用户名密码认证
- 默认账户：motrc/pw777888
- 支持匿名访问（anonymous）

### 安全控制
- **程序黑白名单**：控制允许运行的程序
- **路径黑白名单**：限制文件访问路径
- **模式切换**：灵活的安全策略管理
- **网络隔离**：支持局域网内安全通信

## ⚠️ 注意事项

1. **权限要求**：需要管理员权限才能绑定到低端口号
2. **防火墙设置**：确保相应端口在防火墙中开放
3. **网络环境**：建议在受控的局域网环境中使用
4. **客户端配合**：需要客户端程序来接收和执行命令
5. **安全性**：本系统具有强大控制能力，请谨慎使用

## 🛠️ 开发说明

### 项目结构详解

**Java客户端模块**：
- `gui/` - 图形用户界面相关类
- `handler/` - 命令处理逻辑
- `network/` - 网络通信工具
- `server/` - 各种服务实现
- `util/` - 工具类和日志系统

**C#服务端模块**：
- 对应相同的模块划分，提供服务端功能

### 扩展开发
开发者可以根据需要：
- 添加新的通信协议支持
- 扩展命令类型和功能
- 增强安全认证机制
- 优化性能和稳定性
- 开发对应的客户端程序

## 📞 技术支持

如遇到问题，请检查：
1. 端口是否被占用
2. 防火墙设置是否正确
3. 网络连接是否正常
4. 客户端是否在线
5. 权限是否足够

## 📄 许可证

本项目仅供学习和研究使用，请遵守相关法律法规。

---

*Remote Command GUI v1.0.0*
最后更新：2026年2月28日