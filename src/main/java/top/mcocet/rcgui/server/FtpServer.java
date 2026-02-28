package top.mcocet.rcgui.server;

import top.mcocet.rcgui.util.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FTP服务器类
 * 提供简化的FTP服务功能
 */
public class FtpServer {
    private static ServerSocket ftpServerSocket;
    private static boolean ftpRunning = false;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // FTP端口配置
    private static final int FTP_PORT = 6744;
    
    /**
     * 启动FTP服务器
     */
    public static void start() {
        if (ftpRunning) {
            Logger.log("FTP服务已经在运行中");
            return;
        }
        
        try {
            ftpServerSocket = new ServerSocket(FTP_PORT);
            ftpRunning = true;
            
            // 启动处理FTP连接的任务
            executorService.submit(FtpServer::handleFtpConnections);
            
            Logger.log("FTP服务已启动，监听端口 " + FTP_PORT);
            Logger.log("支持的用户: motrc/pw777888 或 anonymous");
        } catch (IOException e) {
            Logger.error("启动FTP服务失败: " + e.getMessage());
            ftpRunning = false;
        }
    }
    
    /**
     * 停止FTP服务器
     */
    public static void stop() {
        if (!ftpRunning) {
            Logger.log("FTP服务未运行");
            return;
        }
        
        try {
            ftpRunning = false;
            if (ftpServerSocket != null) {
                ftpServerSocket.close();
            }
            Logger.log("FTP服务已停止");
        } catch (IOException e) {
            Logger.error("停止FTP服务时出错: " + e.getMessage());
        }
    }
    
    /**
     * 处理FTP连接
     */
    private static void handleFtpConnections() {
        Logger.log("开始监听FTP连接");
        
        while (ftpRunning) {
            try {
                Socket clientSocket = ftpServerSocket.accept();
                Logger.log("接受到新的FTP连接: " + clientSocket.getInetAddress().getHostAddress());
                
                // 为每个客户端连接创建新线程处理
                executorService.submit(() -> handleFtpClient(clientSocket));
                
            } catch (SocketException e) {
                // 通常是服务器套接字被关闭
                if (ftpRunning) {
                    Logger.error("FTP服务器套接字异常: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (ftpRunning) {
                    Logger.error("接受FTP连接时出错: " + e.getMessage());
                }
            }
        }
        
        Logger.log("FTP监听器已关闭");
    }
    
    /**
     * 处理单个FTP客户端
     */
    private static void handleFtpClient(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        Logger.log("开始处理FTP客户端: " + clientIp);
        
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)
        ) {
            // 发送欢迎信息
            writer.println("220 Remote Command Server FTP Service Ready");
            Logger.log("[" + clientIp + "] 发送欢迎信息: 220 Remote Command Server FTP Service Ready");
            
            boolean isLoggedIn = false;
            String userName = null;
            String currentDirectory = "/";
            
            // 持续读取客户端命令
            String command;
            while ((command = reader.readLine()) != null && !clientSocket.isClosed()) {
                Logger.log("[" + clientIp + "] 收到命令: " + command);
                
                String[] parts = command.trim().split("\\s+", 2);
                String cmd = parts[0].toUpperCase();
                String args = parts.length > 1 ? parts[1] : "";
                
                switch (cmd) {
                    case "USER":
                        // 检查用户名
                        userName = args.trim();
                        if (!userName.isEmpty() && (userName.equals("motrc") || userName.equals("anonymous"))) {
                            writer.println("331 Username OK, need password");
                            Logger.log("[" + clientIp + "] 用户名验证通过: " + userName);
                        } else {
                            writer.println("530 Invalid username");
                            Logger.log("[" + clientIp + "] 用户名验证失败: " + userName);
                        }
                        break;
                        
                    case "PASS":
                        // 密码验证
                        String password = args.trim();
                        if (userName != null) {
                            if (userName.equals("motrc") && password.equals("pw777888")) {
                                isLoggedIn = true;
                                writer.println("230 User logged in, proceed");
                                Logger.log("[" + clientIp + "] 用户认证成功");
                            } else if (userName.equals("anonymous")) {
                                isLoggedIn = true;
                                writer.println("230 Anonymous login successful");
                                Logger.log("[" + clientIp + "] 匿名用户登录成功");
                            } else {
                                writer.println("530 Login incorrect");
                                Logger.log("[" + clientIp + "] 用户认证失败: " + userName);
                            }
                        } else {
                            writer.println("503 Bad sequence of commands");
                        }
                        break;
                        
                    case "TYPE":
                        // 类型设置
                        if (args.toUpperCase().startsWith("I")) {
                            writer.println("200 Type set to I (binary)");
                            Logger.log("[" + clientIp + "] 设置传输类型为二进制");
                        } else {
                            writer.println("200 Type set to A (ascii)");
                            Logger.log("[" + clientIp + "] 设置传输类型为ASCII");
                        }
                        break;
                        
                    case "QUIT":
                        writer.println("221 Goodbye");
                        Logger.log("[" + clientIp + "] 客户端退出连接");
                        return;
                        
                    case "STOR":
                        // 存储文件
                        if (!isLoggedIn) {
                            writer.println("530 Not logged in");
                            Logger.log("[" + clientIp + "] 未登录尝试上传文件");
                            break;
                        }
                        
                        String fileName = args.trim();
                        if (fileName.isEmpty()) {
                            writer.println("501 Syntax error in parameters");
                            Logger.log("[" + clientIp + "] STOR命令参数错误");
                            break;
                        }
                        
                        Logger.log("[" + clientIp + "] 准备接收文件: " + fileName);
                        
                        try {
                            // 创建日志目录
                            Path logDirectory = Paths.get("logs");
                            if (!Files.exists(logDirectory)) {
                                Files.createDirectories(logDirectory);
                                Logger.log("[" + clientIp + "] 创建日志目录");
                            }
                            
                            Path filePath = logDirectory.resolve(clientIp + "_" + fileName);
                            Logger.log("[" + clientIp + "] 文件保存路径: " + filePath.toString());
                            
                            // 先发送准备接收文件的响应
                            writer.println("150 Opening binary mode data connection for STOR");
                            
                            // 接收文件数据
                            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                                // 使用缓冲区读取数据
                                byte[] buffer = new byte[4096];
                                InputStream inputStream = clientSocket.getInputStream();
                                int bytesRead;
                                
                                // 循环读取直到连接关闭或没有更多数据
                                while ((bytesRead = inputStream.read(buffer)) > 0) {
                                    fos.write(buffer, 0, bytesRead);
                                    
                                    // 检查是否还有更多数据（简单检查）
                                    if (inputStream.available() == 0) {
                                        // 等待一小段时间看是否有更多数据到达
                                        Thread.sleep(50);
                                        if (inputStream.available() == 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            writer.println("226 Transfer complete");
                            Logger.log("[" + clientIp + "] 文件传输完成: " + fileName);
                        } catch (Exception e) {
                            Logger.error("[" + clientIp + "] FTP数据传输错误: " + e.getMessage());
                            writer.println("425 Can't open data connection");
                        }
                        break;
                        
                    case "PWD":
                    case "XPWD":
                        // 返回当前目录
                        writer.println("257 \"" + currentDirectory + "\" is current directory");
                        Logger.log("[" + clientIp + "] 返回当前目录: " + currentDirectory);
                        break;
                        
                    case "SYST":
                        // 返回系统信息
                        writer.println("215 UNIX Type: L8");
                        Logger.log("[" + clientIp + "] 返回系统类型");
                        break;
                        
                    case "PORT":
                    case "EPRT":
                    case "PASV":
                    case "EPSV":
                        // 数据连接相关命令，简单回复
                        writer.println("502 Command not implemented");
                        Logger.log("[" + clientIp + "] 不支持的数据连接命令: " + cmd);
                        break;
                        
                    default:
                        writer.println("502 Command not implemented");
                        Logger.log("[" + clientIp + "] 未实现的命令: " + cmd);
                        break;
                }
            }
        } catch (Exception e) {
            Logger.error("处理FTP客户端 " + clientIp + " 时出错: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                Logger.log("[" + clientIp + "] 客户端连接已关闭");
            } catch (IOException e) {
                Logger.error("关闭FTP客户端 " + clientIp + " 连接时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查FTP服务是否正在运行
     * @return 运行状态
     */
    public static boolean isRunning() {
        return ftpRunning;
    }
    
    /**
     * 获取FTP服务端口
     * @return FTP端口号
     */
    public static int getPort() {
        return FTP_PORT;
    }
}