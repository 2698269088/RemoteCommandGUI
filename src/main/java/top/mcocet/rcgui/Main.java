package top.mcocet.rcgui;

import top.mcocet.rcgui.handler.CommandHandler;
import top.mcocet.rcgui.network.NetworkHelper;
import top.mcocet.rcgui.server.FtpServer;
import top.mcocet.rcgui.server.HttpServerManager;
import top.mcocet.rcgui.server.UdpServer;
import top.mcocet.rcgui.util.Logger;

import javax.swing.*;

import java.net.InetAddress;
import java.util.Scanner;

/**
 * 远程命令GUI主程序
 * 提供命令行界面来控制远程命令服务器
 */
public class Main {
    public static void main(String[] args) {
        Logger.log("远程命令服务器启动");
        System.out.println("远程命令服务器");
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

        Scanner scanner = new Scanner(System.in);
        String command;
        
        do {
            System.out.print("> ");
            command = scanner.nextLine().trim().toLowerCase();
            
            String[] parts = command.isEmpty() ? new String[0] : command.split("\\s+");
            
            if (parts.length > 0) {
                switch (parts[0]) {
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
                    case "help":
                        showHelp();
                        break;
                    case "network":
                        showNetworkInfo();
                        break;
                    case "gui":
                        startGui();
                        break;
                    case "quit":
                        // 退出前停止所有服务
                        stopAllServices();
                        break;
                    case "":
                        // 空命令，不做处理
                        break;
                    default:
                        System.out.println("未知命令: " + parts[0]);
                        Logger.log("未知命令: " + parts[0]);
                        break;
                }
            }
        } while (!"quit".equals(command));
        
        System.out.println("程序已退出");
        Logger.log("程序正常退出");
    }
    
    /**
     * 处理start命令
     */
    private static void handleStartCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "http":
                    HttpServerManager.start();
                    break;
                case "ftp":
                    FtpServer.start();
                    break;
                case "udprcv":
                    UdpServer.startUdpLogReceiver();
                    break;
                default:
                    System.out.println("未知服务: " + parts[1]);
                    Logger.log("未知服务: " + parts[1]);
                    break;
            }
        } else {
            System.out.println("请指定要启动的服务 (http/ftp/udprcv)");
            Logger.log("请指定要启动的服务 (http/ftp/udprcv)");
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
                    break;
                case "ftp":
                    FtpServer.stop();
                    break;
                case "udprcv":
                    UdpServer.stopUdpLogReceiver();
                    break;
                default:
                    System.out.println("未知服务: " + parts[1]);
                    Logger.log("未知服务: " + parts[1]);
                    break;
            }
        } else {
            System.out.println("请指定要停止的服务 (http/ftp/udprcv)");
            Logger.log("请指定要停止的服务 (http/ftp/udprcv)");
        }
    }
    
    /**
     * 处理broadcast命令
     */
    private static void handleBroadcastCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "runcmd":
                    UdpServer.sendRunCmdBroadcast();
                    break;
                case "update":
                    UdpServer.sendUpdateBroadcast();
                    break;
                default:
                    System.out.println("未知命令: " + parts[1] + "，支持的命令有: runcmd, update");
                    Logger.log("未知命令: " + parts[1] + "，支持的命令有: runcmd, update");
                    break;
            }
        } else {
            System.out.println("请指定要广播的命令 (runcmd/update)");
            Logger.log("请指定要广播的命令 (runcmd/update)");
        }
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("=== 远程命令服务器帮助 ===");
        System.out.println("服务控制命令:");
        System.out.println("  start http     - 启动HTTP服务");
        System.out.println("  stop http      - 停止HTTP服务");
        System.out.println("  start ftp      - 启动FTP服务");
        System.out.println("  stop ftp       - 停止FTP服务");
        System.out.println("  start udprcv   - 启动UDP日志接收服务");
        System.out.println("  stop udprcv    - 停止UDP日志接收服务");
        System.out.println();
        
        System.out.println("网络命令:");
        System.out.println("  broadcast runcmd - 广播运行命令");
        System.out.println("  broadcast update - 广播更新命令");
        System.out.println("  cmd [命令]       - 广播命令到所有设备");
        System.out.println("  cmd [IP] [命令]  - 向指定IP发送命令");
        System.out.println();
        
        System.out.println("进程管理命令:");
        System.out.println("  process list [IP]           - 查看进程列表");
        System.out.println("  process app [IP]            - 查看应用进程");
        System.out.println("  process taskkill [进程名] [IP] - 结束进程");
        System.out.println();
        
        System.out.println("配置管理命令:");
        System.out.println("  restore config [IP]     - 还原所有配置");
        System.out.println("  disable nic_detection [IP] - 关闭网卡检测");
        System.out.println("  disable all_features [IP]  - 关闭所有功能");
        System.out.println();
        
        System.out.println("其他命令:");
        System.out.println("  link           - 发送服务器地址信息");
        System.out.println("  network        - 显示网络接口信息");
        System.out.println("  gui            - 启动图形用户界面");
        System.out.println("  help           - 显示此帮助信息");
        System.out.println("  quit           - 退出程序");
        System.out.println("=========================");
    }
    
    /**
     * 显示网络接口信息
     */
    private static void showNetworkInfo() {
        System.out.println(NetworkHelper.getNetworkInterfacesInfo());
    }
    
    /**
     * 启动GUI界面
     */
    private static void startGui() {
        Logger.log("正在启动GUI界面...");
        System.out.println("正在启动图形用户界面...");
        
        try {
            // 在新线程中启动GUI以避免阻塞控制台
            SwingUtilities.invokeLater(() -> {
                try {
                    top.mcocet.rcgui.gui.MainWindow mainWindow = new top.mcocet.rcgui.gui.MainWindow();
                    mainWindow.showWindow();
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
     * 停止所有服务
     */
    private static void stopAllServices() {
        Logger.log("正在停止所有服务...");
        
        // 停止HTTP服务
        if (HttpServerManager.isRunning()) {
            HttpServerManager.stop();
        }
        
        // 停止FTP服务
        if (FtpServer.isRunning()) {
            FtpServer.stop();
        }
        
        // 停止UDP日志接收服务
        if (UdpServer.isUdpLogRunning()) {
            UdpServer.stopUdpLogReceiver();
        }
        
        // 关闭UDP套接字
        UdpServer.closeAllUdpSockets();
        
        Logger.log("所有服务已停止");
    }
}