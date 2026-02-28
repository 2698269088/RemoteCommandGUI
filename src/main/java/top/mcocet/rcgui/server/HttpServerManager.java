package top.mcocet.rcgui.server;

import top.mcocet.rcgui.util.Logger;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP服务器类
 * 提供HTTP服务功能
 */
public class HttpServerManager {
    private static HttpServer httpServer;
    private static boolean httpRunning = false;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // HTTP端口配置
    private static final int[] HTTP_PORTS = {6742, 80, 21997};
    
    /**
     * 启动HTTP服务器
     */
    public static void start() {
        if (httpRunning) {
            Logger.log("HTTP服务已经在运行中");
            return;
        }

        Exception lastException = null;
        
        // 尝试在不同端口启动HTTP服务器
        for (int port : HTTP_PORTS) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(port), 0);
                httpRunning = true;
                
                // 添加上下文处理器
                httpServer.createContext("/rc/command.bat", new CommandHandler());
                httpServer.setExecutor(executorService);
                httpServer.start();
                
                Logger.log("HTTP服务已启动，监听端口: " + port);
                Logger.log("监听地址:");
                Logger.log("  http://127.0.0.1:" + port + "/rc/command.bat");
                Logger.log("  http://localhost:" + port + "/rc/command.bat");
                
                return; // 成功启动后退出循环
                
            } catch (IOException e) {
                lastException = e;
                Logger.warn("无法在端口 " + port + " 启动HTTP服务: " + e.getMessage());
            }
        }
        
        // 如果所有端口都失败
        httpRunning = false;
        Logger.error("启动HTTP服务失败，所有配置端口均不可用: " + lastException.getMessage());
    }
    
    /**
     * 停止HTTP服务器
     */
    public static void stop() {
        if (!httpRunning) {
            Logger.log("HTTP服务未运行");
            return;
        }
        
        try {
            httpRunning = false;
            if (httpServer != null) {
                httpServer.stop(0);
            }
            Logger.log("HTTP服务已停止");
        } catch (Exception e) {
            Logger.error("停止HTTP服务时出错: " + e.getMessage());
        }
    }
    
    /**
     * HTTP请求处理器 - 处理命令文件请求
     */
    static class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestMethod = exchange.getRequestMethod();
                String requestUri = exchange.getRequestURI().toString();
                
                Logger.log("收到HTTP请求: " + requestMethod + " " + requestUri);
                
                if ("GET".equals(requestMethod) && "/rc/command.bat".equals(requestUri)) {
                    // 返回命令文件内容
                    String commandContent = getOrCreateCommandFile();
                    
                    byte[] response = commandContent.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(200, response.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                    
                    Logger.log("成功返回命令文件内容");
                } else {
                    // 404 Not Found
                    String response = "404 Not Found";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    
                    Logger.log("请求路径未找到: " + requestUri);
                }
            } catch (Exception e) {
                Logger.error("处理HTTP请求时出错: " + e.getMessage());
                try {
                    String response = "500 Internal Server Error";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } catch (IOException ioException) {
                    Logger.error("发送错误响应时出错: " + ioException.getMessage());
                }
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * 获取或创建命令文件
     * @return 命令内容
     */
    private static String getOrCreateCommandFile() {
        String commandFilePath = "command.bat";
        
        try {
            Path path = Paths.get(commandFilePath);
            
            // 如果文件不存在，创建一个示例文件
            if (!Files.exists(path)) {
                String sampleCommand = "@echo off\r\n" +
                                     "echo Hello from Remote Command Server!\r\n" +
                                     "echo 此命令由远程服务器下发\r\n" +
                                     "echo 当前时间: %date% %time%\r\n" +
                                     "dir\r\n";
                
                Files.write(path, sampleCommand.getBytes(StandardCharsets.UTF_8));
                Logger.log("创建示例命令文件");
            }
            
            Logger.log("读取命令文件内容");
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            Logger.error("读取或创建命令文件时出错: " + e.getMessage());
            // 返回默认命令内容
            return "@echo off\r\n" +
                   "echo 命令文件读取失败\r\n" +
                   "echo 错误信息: " + e.getMessage() + "\r\n";
        }
    }
    
    /**
     * 检查HTTP服务是否正在运行
     * @return 运行状态
     */
    public static boolean isRunning() {
        return httpRunning;
    }
    
    /**
     * 获取当前运行的端口号
     * @return 端口号，如果未运行则返回-1
     */
    public static int getRunningPort() {
        if (httpRunning && httpServer != null) {
            return httpServer.getAddress().getPort();
        }
        return -1;
    }
    
    /**
     * 重启HTTP服务
     */
    public static void restart() {
        Logger.log("正在重启HTTP服务...");
        stop();
        try {
            Thread.sleep(1000); // 等待1秒确保端口释放
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }
}