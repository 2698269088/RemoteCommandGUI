package top.mcocet.rcgui.server;

import top.mcocet.rcgui.network.NetworkHelper;
import top.mcocet.rcgui.util.Logger;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * UDP服务器类
 * 提供UDP广播和单播通信功能
 */
public class UdpServer {
    // UDP端口配置
    private static final int UDP_SEND_PORT = 6743;    // 发送端口
    private static final int UDP_RECEIVE_PORT = 6746; // 接收端口
    
    // UDP客户端列表
    private static List<DatagramSocket> udpSockets;
    private static DatagramSocket udpLogListener;
    private static volatile boolean udpLogRunning = false;
    
    // 线程池用于处理异步任务
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 初始化所有网络接口的UDP套接字
     */
    public static void initializeUdpSockets() {
        try {
            udpSockets = new ArrayList<>();
            
            // 获取所有IPv4地址
            List<InetAddress> ipv4Addresses = NetworkHelper.getAllIPv4Addresses();
            
            for (InetAddress ipAddress : ipv4Addresses) {
                try {
                    // 为每个IP地址创建一个UDP套接字
                    DatagramSocket socket = new DatagramSocket(0, ipAddress);
                    socket.setBroadcast(true);
                    udpSockets.add(socket);
                    
                    Logger.log("已初始化网络接口: " + ipAddress.getHostAddress());
                } catch (SocketException e) {
                    Logger.error("初始化网络接口 " + ipAddress.getHostAddress() + " 失败: " + e.getMessage());
                }
            }
            
            // 如果没有找到任何网络接口，则至少添加一个默认的
            if (udpSockets.isEmpty()) {
                DatagramSocket defaultSocket = new DatagramSocket();
                defaultSocket.setBroadcast(true);
                udpSockets.add(defaultSocket);
                Logger.log("使用默认UDP套接字");
            }
        } catch (Exception e) {
            Logger.error("初始化UDP套接字时出错: " + e.getMessage());
            udpSockets = new ArrayList<>();
            try {
                DatagramSocket defaultSocket = new DatagramSocket();
                defaultSocket.setBroadcast(true);
                udpSockets.add(defaultSocket);
            } catch (SocketException se) {
                Logger.error("创建默认UDP套接字失败: " + se.getMessage());
            }
        }
    }
    
    /**
     * 自动获取所有网卡IP地址并向每个地址发送link命令
     */
    public static void sendLinkAuto() {
        try {
            // 如果还没有初始化UDP套接字，则进行初始化
            if (udpSockets == null) {
                initializeUdpSockets();
            }

            // 获取所有网络接口的IPv4地址
            List<InetAddress> ipv4Addresses = NetworkHelper.getAllIPv4Addresses();

            // 为每个IPv4地址发送link消息
            for (InetAddress ipAddress : ipv4Addresses) {
                String message = "MOT-RC link " + ipAddress.getHostAddress();
                byte[] bytes = message.getBytes("UTF-8");
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

                // 找到与这个IP地址对应的UDP套接字发送消息
                boolean sent = false;
                for (DatagramSocket socket : udpSockets) {
                    try {
                        // 获取该UDP套接字绑定的本地IP地址
                        if (socket.getLocalSocketAddress() instanceof InetSocketAddress) {
                            InetSocketAddress localAddr = (InetSocketAddress) socket.getLocalSocketAddress();
                            if (localAddr.getAddress().equals(ipAddress)) {
                                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcastAddress, UDP_SEND_PORT);
                                socket.send(packet);
                                Logger.log("已通过网卡(" + ipAddress.getHostAddress() + ")发送link广播");
                                sent = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Logger.error("通过网卡(" + ipAddress.getHostAddress() + ")发送link广播时出错: " + e.getMessage());
                    }
                }

                if (!sent) {
                    Logger.warn("未找到网卡(" + ipAddress.getHostAddress() + ")对应的UDP套接字");
                }
            }
        } catch (Exception e) {
            Logger.error("发送link广播时出错: " + e.getMessage());
        }
    }
    
    /**
     * 启动UDP日志接收服务
     */
    public static void startUdpLogReceiver() {
        if (udpLogRunning) {
            Logger.log("UDP日志接收服务已在运行中");
            return;
        }

        try {
            udpLogListener = new DatagramSocket(UDP_RECEIVE_PORT);
            udpLogRunning = true;
            
            // 启动异步接收UDP数据的任务
            executorService.submit(UdpServer::receiveUdpLogs);
            
            Logger.log("UDP日志接收服务已启动，监听端口 " + UDP_RECEIVE_PORT);
        } catch (SocketException e) {
            Logger.error("启动UDP日志接收服务失败: " + e.getMessage());
            udpLogRunning = false;
        }
    }
    
    /**
     * 停止UDP日志接收服务
     */
    public static void stopUdpLogReceiver() {
        if (!udpLogRunning) {
            Logger.log("UDP日志接收服务未运行");
            return;
        }
        
        try {
            udpLogRunning = false;
            if (udpLogListener != null) {
                udpLogListener.close();
            }
            Logger.log("UDP日志接收服务已停止");
        } catch (Exception e) {
            Logger.error("停止UDP日志接收服务时出错: " + e.getMessage());
        }
    }
    
    /**
     * 异步接收UDP日志数据
     */
    private static void receiveUdpLogs() {
        Logger.log("开始监听UDP日志数据");
        byte[] buffer = new byte[1024];
        
        while (udpLogRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpLogListener.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                InetAddress clientAddress = packet.getAddress();
                
                // 检查消息格式是否符合要求
                if (message.startsWith("MOT-RC cmdlog ")) {
                    // 提取日志内容
                    String logContent = message.substring("MOT-RC cmdlog ".length());
                    Logger.log("[来自 " + clientAddress.getHostAddress() + " 的日志]: " + logContent);
                } else if (message.startsWith("MOT-RC process pl app:")) {
                    // 解析进程列表信息
                    parseAndDisplayProcessList(message, clientAddress);
                } else if (message.startsWith("MOT-RC process apl:")) {
                    // 解析新的应用进程列表信息
                    parseAndDisplayApplicationProcessList(message, clientAddress);
                } else {
                    // 不符合格式的消息
                    Logger.log("[来自 " + clientAddress.getHostAddress() + " 的未知消息]: " + message);
                }
            } catch (SocketException e) {
                // UDP套接字已关闭
                if (udpLogRunning) {
                    Logger.error("UDP套接字异常关闭: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (udpLogRunning) {
                    Logger.error("接收UDP日志时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 解析并显示进程列表
     */
    private static void parseAndDisplayProcessList(String message, InetAddress clientAddress) {
        try {
            // 格式: MOT-RC process pl app:"应用进程" bg:"后台进程"
            int appIndex = message.indexOf("app:");
            if (appIndex == -1) {
                Logger.log("[来自 " + clientAddress.getHostAddress() + " 的进程列表格式错误]: " + message);
                return;
            }

            int bgIndex = message.indexOf("bg:");
            if (bgIndex == -1) {
                Logger.log("[来自 " + clientAddress.getHostAddress() + " 的进程列表格式错误]: " + message);
                return;
            }

            // 提取应用进程列表
            String appProcesses = message.substring(appIndex + 5, bgIndex - 1); // 5是"app:"的长度+1个空格，减去1个引号
            
            // 提取后台进程列表
            String bgProcesses = message.substring(bgIndex + 4, message.length() - 1); // 4是"bg:"的长度+1个空格，去掉最后的引号
            
            // 输出进程列表信息
            Logger.log("[来自 " + clientAddress.getHostAddress() + " 的进程列表]");
            Logger.log("应用进程: " + appProcesses);
            Logger.log("后台进程: " + bgProcesses);
            
        } catch (Exception e) {
            Logger.error("[来自 " + clientAddress.getHostAddress() + " 的进程列表解析错误]: " + e.getMessage());
        }
    }
    
    /**
     * 解析并显示新的应用进程列表格式
     */
    private static void parseAndDisplayApplicationProcessList(String message, InetAddress clientAddress) {
        try {
            // 格式: MOT-RC process apl:"C:\Windows\System32\notepad.exe+notepad,C:\Program Files\Google\Chrome\Application\chrome.exe+chrome"
            final String prefix = "MOT-RC process apl:";
            if (!message.startsWith(prefix)) {
                Logger.log("[来自 " + clientAddress.getHostAddress() + " 的进程列表格式错误]: " + message);
                return;
            }

            // 提取引号内的内容
            String content = message.substring(prefix.length());
            if (!content.startsWith("\"") || !content.endsWith("\"")) {
                Logger.log("[来自 " + clientAddress.getHostAddress() + " 的进程列表格式错误]: " + message);
                return;
            }

            // 去掉引号
            content = content.substring(1, content.length() - 1);
            
            // 分割各个应用进程项
            String[] appEntries = content.split(",");
            
            // 输出进程列表信息
            Logger.log("[来自 " + clientAddress.getHostAddress() + " 的应用进程列表]");
            for (String entry : appEntries) {
                // 每个条目格式为: 路径+名称
                int plusIndex = entry.lastIndexOf('+');
                if (plusIndex > 0 && plusIndex < entry.length() - 1) {
                    String path = entry.substring(0, plusIndex);
                    String name = entry.substring(plusIndex + 1);
                    Logger.log("  路径: " + path);
                    Logger.log("  名称: " + name);
                } else {
                    // 如果没有找到+号或者格式不对，直接显示整个条目
                    Logger.log("  条目: " + entry);
                }
            }
            
        } catch (Exception e) {
            Logger.error("[来自 " + clientAddress.getHostAddress() + " 的应用进程列表解析错误]: " + e.getMessage());
        }
    }
    
    /**
     * 向指定IP地址发送命令
     */
    public static void sendCmdToSpecificIP(InetAddress targetIp, String command) {
        try {
            if (udpSockets == null) {
                initializeUdpSockets();
            }
            
            String message = "MOT-RC cmd " + command;
            byte[] bytes = message.getBytes("UTF-8");
            
            if (!udpSockets.isEmpty()) {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, targetIp, UDP_SEND_PORT);
                udpSockets.get(0).send(packet);
                Logger.log("已向 " + targetIp.getHostAddress() + " 发送命令: " + command);
            } else {
                Logger.error("未找到可用的UDP套接字");
            }
        } catch (Exception e) {
            Logger.error("向 " + targetIp.getHostAddress() + " 发送命令时出错: " + e.getMessage());
        }
    }
    
    /**
     * 广播命令到所有网络接口
     */
    public static void sendCmdBroadcast(String command) {
        String message = "MOT-RC cmd " + command;
        sendBroadcast(message);
    }
    
    /**
     * 发送广播包到所有网络接口
     */
    private static void sendBroadcast(String message) {
        try {
            if (udpSockets == null) {
                initializeUdpSockets();
            }
            
            byte[] bytes = message.getBytes("UTF-8");
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            
            // 向所有网络接口发送广播
            for (DatagramSocket socket : udpSockets) {
                try {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcastAddress, UDP_SEND_PORT);
                    socket.send(packet);
                } catch (Exception e) {
                    Logger.error("通过某个网络接口发送广播时出错: " + e.getMessage());
                }
            }
            
            Logger.log("已发送广播消息: " + message);
        } catch (Exception e) {
            Logger.error("发送广播时出错: " + e.getMessage());
        }
    }
    
    /**
     * 关闭所有UDP套接字
     */
    public static void closeAllUdpSockets() {
        if (udpSockets != null) {
            for (DatagramSocket socket : udpSockets) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {
                    // 忽略关闭时的异常
                }
            }
            udpSockets = null;
        }
        
        // 关闭日志监听器
        stopUdpLogReceiver();
        
        // 关闭线程池
        executorService.shutdown();
    }
    
    // 以下是从CommandHandler中调用的UDP发送方法
    
    /**
     * 发送runcmd命令广播包
     */
    public static void sendRunCmdBroadcast() {
        sendBroadcast("MOT-RC runcmd");
    }
    
    /**
     * 发送update命令广播包
     */
    public static void sendUpdateBroadcast() {
        sendBroadcast("MOT-RC update");
    }
    
    /**
     * 发送process list命令广播包
     */
    public static void sendProcessListBroadcast() {
        sendBroadcast("MOT-RC process list");
    }
    
    /**
     * 发送process app命令广播包
     */
    public static void sendProcessAppBroadcast() {
        sendBroadcast("MOT-RC process app");
    }
    
    /**
     * 发送process path_black命令广播包
     */
    public static void sendProcessPathBlackBroadcast(String action, String pathList) {
        sendBroadcast("MOT-RC path_black " + action + " " + pathList);
    }
    
    /**
     * 发送process path_white命令广播包
     */
    public static void sendProcessPathWhiteBroadcast(String action, String pathList) {
        sendBroadcast("MOT-RC path_white " + action + " " + pathList);
    }
    
    /**
     * 发送path_mode命令广播包
     */
    public static void sendPathModeBroadcast(String mode) {
        sendBroadcast("MOT-RC path_mode " + mode);
    }
    
    /**
     * 发送process taskkill命令广播包
     */
    public static void sendProcessTaskKillBroadcast(String processName) {
        sendBroadcast("MOT-RC process taskkill " + processName);
    }
    
    /**
     * 向指定IP地址发送process list命令
     */
    public static void sendProcessListToSpecificIP(InetAddress targetIp) {
        sendToSpecificIP(targetIp, "MOT-RC process list");
    }
    
    /**
     * 向指定IP地址发送process app命令
     */
    public static void sendProcessAppToSpecificIP(InetAddress targetIp) {
        sendToSpecificIP(targetIp, "MOT-RC process app");
    }
    
    /**
     * 向指定IP地址发送process path_black命令
     */
    public static void sendProcessPathBlackToSpecificIP(InetAddress targetIp, String action, String pathList) {
        sendToSpecificIP(targetIp, "MOT-RC path_black " + action + " " + pathList);
    }
    
    /**
     * 向指定IP地址发送process path_white命令
     */
    public static void sendProcessPathWhiteToSpecificIP(InetAddress targetIp, String action, String pathList) {
        sendToSpecificIP(targetIp, "MOT-RC path_white " + action + " " + pathList);
    }
    
    /**
     * 向指定IP地址发送path_mode命令
     */
    public static void sendPathModeToSpecificIP(InetAddress targetIp, String mode) {
        sendToSpecificIP(targetIp, "MOT-RC path_mode " + mode);
    }
    
    /**
     * 向指定IP地址发送process taskkill命令
     */
    public static void sendProcessTaskKillToSpecificIP(InetAddress targetIp, String processName) {
        sendToSpecificIP(targetIp, "MOT-RC process taskkill " + processName);
    }
    
    /**
     * 发送还原所有配置命令广播包
     */
    public static void sendRestoreConfigBroadcast() {
        sendBroadcast("MOT-RC restore_config");
    }
    
    /**
     * 向指定IP地址发送还原所有配置命令
     */
    public static void sendRestoreConfigToSpecificIP(InetAddress targetIp) {
        sendToSpecificIP(targetIp, "MOT-RC restore_config");
    }
    
    /**
     * 发送关闭网卡检测命令广播包
     */
    public static void sendDisableNicDetectionBroadcast() {
        sendBroadcast("MOT-RC disable_nic_detection");
    }
    
    /**
     * 向指定IP地址发送关闭网卡检测命令
     */
    public static void sendDisableNicDetectionToSpecificIP(InetAddress targetIp) {
        sendToSpecificIP(targetIp, "MOT-RC disable_nic_detection");
    }
    
    /**
     * 发送关闭所有功能命令广播包
     */
    public static void sendDisableAllFeaturesBroadcast() {
        sendBroadcast("MOT-RC disable_all_features");
    }
    
    /**
     * 向指定IP地址发送关闭所有功能命令
     */
    public static void sendDisableAllFeaturesToSpecificIP(InetAddress targetIp) {
        sendToSpecificIP(targetIp, "MOT-RC disable_all_features");
    }
    
    /**
     * 向指定IP地址发送命令
     */
    private static void sendToSpecificIP(InetAddress targetIp, String message) {
        try {
            if (udpSockets == null) {
                initializeUdpSockets();
            }
            
            byte[] bytes = message.getBytes("UTF-8");
            
            if (!udpSockets.isEmpty()) {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, targetIp, UDP_SEND_PORT);
                udpSockets.get(0).send(packet);
                Logger.log("已向 " + targetIp.getHostAddress() + " 发送命令: " + message);
            } else {
                Logger.error("未找到可用的UDP套接字");
            }
        } catch (Exception e) {
            Logger.error("向 " + targetIp.getHostAddress() + " 发送命令时出错: " + e.getMessage());
        }
    }
    
    /**
     * 发送mode命令广播包
     */
    public static void sendModeBroadcast(String mode) {
        sendBroadcast("MOT-RC mode " + mode);
    }
    
    /**
     * 向指定IP地址发送mode命令
     */
    public static void sendModeToSpecificIP(InetAddress targetIp, String mode) {
        sendToSpecificIP(targetIp, "MOT-RC mode " + mode);
    }
    
    /**
     * 发送black命令广播包
     */
    public static void sendBlackBroadcast(String action, String programList) {
        sendBroadcast("MOT-RC black " + action + " " + programList);
    }
    
    /**
     * 向指定IP地址发送black命令
     */
    public static void sendBlackToSpecificIP(InetAddress targetIp, String action, String programList) {
        sendToSpecificIP(targetIp, "MOT-RC black " + action + " " + programList);
    }
    
    /**
     * 发送width命令广播包
     */
    public static void sendWidthBroadcast(String action, String programList) {
        sendBroadcast("MOT-RC width " + action + " " + programList);
    }
    
    /**
     * 向指定IP地址发送width命令
     */
    public static void sendWidthToSpecificIP(InetAddress targetIp, String action, String programList) {
        sendToSpecificIP(targetIp, "MOT-RC width " + action + " " + programList);
    }
    
    /**
     * 检查UDP日志接收服务是否正在运行
     */
    public static boolean isUdpLogRunning() {
        return udpLogRunning;
    }
}