package top.mcocet.rcgui;

import top.mcocet.rcgui.gui.MainWindow;

import static top.mcocet.rcgui.util.Logger.addLogListener;
import top.mcocet.rcgui.handler.CommandHandler;
import top.mcocet.rcgui.network.NetworkHelper;
import top.mcocet.rcgui.server.FtpServer;
import top.mcocet.rcgui.server.HttpServerManager;
import top.mcocet.rcgui.server.UdpServer;
import top.mcocet.rcgui.util.Logger;

import javax.swing.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 混合模式应用程序
 * 同时支持控制台命令行和GUI界面操作
 */
public class HybridApplication {
    private static MainWindow mainWindow;
    private static AtomicBoolean isRunning = new AtomicBoolean(true);
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        Logger.log("远程命令服务器启动");
        System.out.println("远程命令服务器启动");
        System.out.println("GUI界面已启动，您可以在GUI中操作，也可以在此控制台输入命令");
        showConsoleHelp();
        
        // 启动GUI界面
        startGui();
        
        // 启动控制台监听
        startConsoleListener();
        
        // 等待程序结束
        waitForExit();
    }
    
    /**
     * 启动GUI界面
     */
    private static void startGui() {
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    mainWindow = new MainWindow();
                    // 注册混合应用的日志监听器
                    addLogListener(new Logger.LogUpdateListener() {
                        @Override
                        public void onLogUpdate(String logMessage) {
                            // 在控制台显示GUI产生的日志
                            System.out.println(logMessage);
                        }
                    });
                    mainWindow.showWindow();
                    Logger.log("GUI界面已启动");
                    System.out.println("GUI界面已启动");
                } catch (Exception e) {
                    System.err.println("启动GUI失败: " + e.getMessage());
                    Logger.error("启动GUI失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("启动GUI时发生错误: " + e.getMessage());
            Logger.error("启动GUI时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 启动控制台监听器
     */
    private static void startConsoleListener() {
        Thread consoleThread = new Thread(() -> {
            System.out.println("\n=== 控制台命令输入 ===");
            System.out.print("> ");
            
            while (isRunning.get() && scanner.hasNextLine()) {
                try {
                    String command = scanner.nextLine().trim();
                    
                    if (command.isEmpty()) {
                        System.out.print("> ");
                        continue;
                    }
                    
                    // 处理退出命令
                    if ("quit".equalsIgnoreCase(command) || "exit".equalsIgnoreCase(command)) {
                        shutdownApplication();
                        break;
                    }
                    
                    // 处理帮助命令
                    if ("help".equalsIgnoreCase(command)) {
                        showConsoleHelp();
                        System.out.print("> ");
                        continue;
                    }
                    
                    // 处理GUI相关命令
                    if ("hide".equalsIgnoreCase(command)) {
                        hideGui();
                        System.out.print("> ");
                        continue;
                    }
                    
                    if ("show".equalsIgnoreCase(command)) {
                        showGui();
                        System.out.print("> ");
                        continue;
                    }
                    
                    // 执行普通命令
                    executeConsoleCommand(command);
                    System.out.print("> ");
                    
                } catch (Exception e) {
                    System.err.println("处理命令时出错: " + e.getMessage());
                    Logger.error("控制台命令处理错误: " + e.getMessage());
                    System.out.print("> ");
                }
            }
        });
        
        consoleThread.setDaemon(true);
        consoleThread.start();
    }
    
    /**
     * 执行控制台命令
     */
    private static void executeConsoleCommand(String command) {
        try {
            Logger.log("控制台执行命令: " + command);
            
            String[] parts = command.split("\\s+");
            switch (parts[0].toLowerCase()) {
                case "start":
                    handleStartCommand(parts);
                    break;
                case "stop":
                    handleStopCommand(parts);
                    break;
                case "broadcast":
                    handleBroadcastCommand(parts);
                    break;
                case "cmd":
                    CommandHandler.handleCmdCommand(parts);
                    break;
                case "link":
                    CommandHandler.handleLinkCommand(parts);
                    break;
                case "process":
                    CommandHandler.handleProcessCommand(parts);
                    break;
                case "restore":
                    CommandHandler.handleRestoreCommand(parts);
                    break;
                case "disable":
                    CommandHandler.handleDisableCommand(parts);
                    break;
                case "network":
                    System.out.println(NetworkHelper.getNetworkInterfacesInfo());
                    break;
                default:
                    System.out.println("未知命令: " + parts[0]);
                    Logger.log("未知命令: " + parts[0]);
                    break;
            }
            
            System.out.println("命令执行完成");
        } catch (Exception e) {
            System.err.println("执行命令时出错: " + e.getMessage());
            Logger.error("执行命令错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理start命令
     */
    private static void handleStartCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "http":
                    HttpServerManager.start();
                    System.out.println("HTTP服务已启动");
                    break;
                case "ftp":
                    FtpServer.start();
                    System.out.println("FTP服务已启动");
                    break;
                case "udprcv":
                    UdpServer.startUdpLogReceiver();
                    System.out.println("UDP日志接收服务已启动");
                    break;
                default:
                    System.out.println("未知服务: " + parts[1]);
                    break;
            }
        } else {
            System.out.println("请指定要启动的服务 (http/ftp/udprcv)");
        }
    }
    
    /**
     * 处理stop命令
     */
    private static void handleStopCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "http":
                    HttpServerManager.stop();
                    System.out.println("HTTP服务已停止");
                    break;
                case "ftp":
                    FtpServer.stop();
                    System.out.println("FTP服务已停止");
                    break;
                case "udprcv":
                    UdpServer.stopUdpLogReceiver();
                    System.out.println("UDP日志接收服务已停止");
                    break;
                default:
                    System.out.println("未知服务: " + parts[1]);
                    break;
            }
        } else {
            System.out.println("请指定要停止的服务 (http/ftp/udprcv)");
        }
    }
    
    /**
     * 处理广播命令
     */
    private static void handleBroadcastCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "runcmd":
                    UdpServer.sendRunCmdBroadcast();
                    System.out.println("已发送运行命令广播");
                    break;
                case "update":
                    UdpServer.sendUpdateBroadcast();
                    System.out.println("已发送更新命令广播");
                    break;
                default:
                    System.out.println("未知广播命令: " + parts[1]);
                    break;
            }
        } else {
            System.out.println("请指定要广播的命令 (runcmd/update)");
        }
    }
    
    /**
     * 隐藏GUI界面
     */
    private static void hideGui() {
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> mainWindow.setVisible(false));
            System.out.println("GUI界面已隐藏");
        }
    }
    
    /**
     * 显示GUI界面
     */
    private static void showGui() {
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.setVisible(true);
                mainWindow.toFront();
            });
            System.out.println("GUI界面已显示");
        }
    }
    
    /**
     * 显示控制台帮助信息
     */
    private static void showConsoleHelp() {
        System.out.println("=== 混合模式命令帮助 ===");
        System.out.println("服务控制:");
        System.out.println("  start http/ftp/udprcv  - 启动相应服务");
        System.out.println("  stop http/ftp/udprcv   - 停止相应服务");
        System.out.println();
        System.out.println("网络命令:");
        System.out.println("  broadcast runcmd/update - 发送广播命令");
        System.out.println("  cmd [命令]             - 发送命令");
        System.out.println("  link                   - 发送链接信息");
        System.out.println();
        System.out.println("进程管理:");
        System.out.println("  process list/app/taskkill - 进程管理命令");
        System.out.println();
        System.out.println("配置管理:");
        System.out.println("  restore config         - 还原配置");
        System.out.println("  disable nic_detection/all_features - 禁用功能");
        System.out.println();
        System.out.println("界面控制:");
        System.out.println("  show                   - 显示GUI界面");
        System.out.println("  hide                   - 隐藏GUI界面");
        System.out.println();
        System.out.println("系统命令:");
        System.out.println("  network                - 显示网络信息");
        System.out.println("  help                   - 显示此帮助");
        System.out.println("  quit/exit              - 退出程序");
        System.out.println("========================");
        System.out.println("输入命令来控制服务器:");
        System.out.println("  start http     - 启动HTTP服务");
        System.out.println("  stop http      - 停止HTTP服务");
        System.out.println("  start ftp      - 启动FTP服务");
        System.out.println("  stop ftp       - 停止FTP服务");
        System.out.println("  start udprcv   - 启动UDP日志接收服务");
        System.out.println("  stop udprcv    - 停止UDP日志接收服务");
        System.out.println("  broadcast runcmd - 向所有网卡发送runcmd命令广播包");
        System.out.println("  broadcast update - 向所有网卡发送update命令广播包");
        System.out.println("  cmd [IP地址] [命令] - 向指定IP地址发送命令，若无IP地址则广播命令");
        System.out.println("  link [IP地址]  - 向指定IP地址发送服务器地址信息");
        System.out.println("  process list [IP地址] - 向指定IP地址发送进程列表命令，若无IP地址则广播命令");
        System.out.println("  process taskkill [进程名称] [IP地址] - 向指定IP地址发送结束进程命令，若无IP地址则广播命令");
        System.out.println("  restore config [IP地址] - 向指定IP地址发送还原配置命令，若无IP地址则广播命令");
        System.out.println("  disable nic_detection [IP地址] - 向指定IP地址发送关闭网卡检测命令，若无IP地址则广播命令");
        System.out.println("  disable all_features [IP地址] - 向指定IP地址发送关闭所有功能命令，若无IP地址则广播命令");
        System.out.println("  gui            - 启动图形界面");
        System.out.println("  quit           - 退出程序");
        System.out.println("  help           - 显示帮助信息");
        System.out.println("  network        - 显示网络接口信息");
        System.out.println();
    }
    
    /**
     * 关闭应用程序
     */
    private static void shutdownApplication() {
        System.out.println("正在关闭应用程序...");
        Logger.log("正在关闭应用程序...");
        
        isRunning.set(false);
        
        // 停止所有服务
        try {
            if (HttpServerManager.isRunning()) {
                HttpServerManager.stop();
            }
            if (FtpServer.isRunning()) {
                FtpServer.stop();
            }
            if (UdpServer.isUdpLogRunning()) {
                UdpServer.stopUdpLogReceiver();
            }
            UdpServer.closeAllUdpSockets();
        } catch (Exception e) {
            Logger.error("关闭服务时出错: " + e.getMessage());
        }
        
        // 关闭GUI
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> mainWindow.dispose());
        }
        
        // 关闭Scanner
        try {
            scanner.close();
        } catch (Exception e) {
            // 忽略关闭错误
        }
        
        System.out.println("应用程序已关闭");
        Logger.log("应用程序已关闭");
    }
    
    /**
     * 等待程序退出
     */
    private static void waitForExit() {
        try {
            while (isRunning.get()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}