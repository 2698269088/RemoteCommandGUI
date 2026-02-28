package top.mcocet.rcgui.handler;

import top.mcocet.rcgui.network.NetworkHelper;
import top.mcocet.rcgui.server.UdpServer;
import top.mcocet.rcgui.util.Logger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令处理器类
 * 处理各种远程命令请求
 */
public class CommandHandler {
    
    /**
     * 处理link命令
     * @param parts 命令参数数组
     */
    public static void handleLinkCommand(String[] parts) {
        // link命令格式: link (自动获取所有网卡IP并发送)
        if (parts.length > 1) {
            System.out.println("用法: link (无需参数)");
            Logger.log("link命令不需要参数");
            return;
        }

        UdpServer.sendLinkAuto();
    }
    
    /**
     * 处理process命令
     * @param parts 命令参数数组
     */
    public static void handleProcessCommand(String[] parts) {
        // process命令格式检查
        if (parts.length < 2 || 
            (!"list".equals(parts[1]) && !"taskkill".equals(parts[1]) && 
             !"app".equals(parts[1]) && !"path_black".equals(parts[1]) && 
             !"path_white".equals(parts[1]) && !"path_mode".equals(parts[1]))) {
            
            System.out.println("用法: process list [IP地址] 或 process list");
            System.out.println("      process app [IP地址] 或 process app");
            System.out.println("      process path_black add [路径列表] [IP地址] 或 process path_black add [路径列表]");
            System.out.println("      process path_black del [路径列表] [IP地址] 或 process path_black del [路径列表]");
            System.out.println("      process path_white add [路径列表] [IP地址] 或 process path_white add [路径列表]");
            System.out.println("      process path_white del [路径列表] [IP地址] 或 process path_white del [路径列表]");
            System.out.println("      process path_mode whitelist [IP地址] 或 process path_mode whitelist");
            System.out.println("      process path_mode blacklist [IP地址] 或 process path_mode blacklist");
            System.out.println("      process taskkill [进程名称] [IP地址] 或 process taskkill [进程名称]");
            Logger.log("process命令格式不正确");
            return;
        }

        switch (parts[1]) {
            case "list":
                handleProcessList(parts);
                break;
            case "app":
                handleProcessApp(parts);
                break;
            case "path_black":
                handleProcessPathBlack(parts);
                break;
            case "path_white":
                handleProcessPathWhite(parts);
                break;
            case "path_mode":
                handleProcessPathMode(parts);
                break;
            case "taskkill":
                handleProcessTaskKill(parts);
                break;
        }
    }
    
    /**
     * 处理process list命令
     */
    private static void handleProcessList(String[] parts) {
        if (parts.length == 2) {
            // 广播命令: process list
            UdpServer.sendProcessListBroadcast();
        } else if (parts.length == 3) {
            // 向指定IP发送命令: process list [IP地址]
            String ipAddress = parts[2];
            if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                try {
                    InetAddress targetIp = InetAddress.getByName(ipAddress);
                    UdpServer.sendProcessListToSpecificIP(targetIp);
                } catch (Exception e) {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            System.out.println("用法: process list [IP地址] 或 process list");
            Logger.log("process list命令参数过多");
        }
    }
    
    /**
     * 处理process app命令
     */
    private static void handleProcessApp(String[] parts) {
        if (parts.length == 2) {
            // 广播命令: process app
            UdpServer.sendProcessAppBroadcast();
        } else if (parts.length == 3) {
            // 向指定IP发送命令: process app [IP地址]
            String ipAddress = parts[2];
            if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                try {
                    InetAddress targetIp = InetAddress.getByName(ipAddress);
                    UdpServer.sendProcessAppToSpecificIP(targetIp);
                } catch (Exception e) {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            System.out.println("用法: process app [IP地址] 或 process app");
            Logger.log("process app命令参数过多");
        }
    }
    
    /**
     * 处理process path_black命令
     */
    private static void handleProcessPathBlack(String[] parts) {
        if (parts.length < 3 || (!"add".equals(parts[2]) && !"del".equals(parts[2]))) {
            System.out.println("用法: process path_black add [路径列表] [IP地址] 或 process path_black add [路径列表]");
            System.out.println("      process path_black del [路径列表] [IP地址] 或 process path_black del [路径列表]");
            Logger.log("process path_black命令格式不正确");
            return;
        }

        String action = parts[2]; // add 或 del

        if (parts.length < 4) {
            System.out.println("用法: process path_black " + action + " [路径列表] [IP地址] 或 process path_black " + action + " [路径列表]");
            Logger.log("process path_black " + action + "命令参数不足");
            return;
        }

        // 解析参数
        List<String> parsedArgs = parseCommandArguments(parts, 3);
        if (parsedArgs.isEmpty()) {
            System.out.println("请提供路径列表");
            Logger.log("缺少路径列表");
            return;
        }

        // 处理路径列表和可能的IP地址
        String ipAddress = null;
        List<String> paths = new ArrayList<>();

        // 检查最后一个参数是否为IP地址
        if (parsedArgs.size() >= 1 && NetworkHelper.isValidIPv4Address(parsedArgs.get(parsedArgs.size() - 1))) {
            ipAddress = parsedArgs.get(parsedArgs.size() - 1);
            for (int i = 0; i < parsedArgs.size() - 1; i++) {
                paths.add(parsedArgs.get(i));
            }
        } else {
            paths.addAll(parsedArgs);
        }

        // 构造路径列表，用逗号连接
        String pathList = String.join(",", paths);

        if (ipAddress != null) {
            // 向指定IP发送命令
            try {
                InetAddress targetIp = InetAddress.getByName(ipAddress);
                UdpServer.sendProcessPathBlackToSpecificIP(targetIp, action, pathList);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            // 广播命令
            UdpServer.sendProcessPathBlackBroadcast(action, pathList);
        }
    }
    
    /**
     * 处理process path_white命令
     */
    private static void handleProcessPathWhite(String[] parts) {
        if (parts.length < 3 || (!"add".equals(parts[2]) && !"del".equals(parts[2]))) {
            System.out.println("用法: process path_white add [路径列表] [IP地址] 或 process path_white add [路径列表]");
            System.out.println("      process path_white del [路径列表] [IP地址] 或 process path_white del [路径列表]");
            Logger.log("process path_white命令格式不正确");
            return;
        }

        String action = parts[2]; // add 或 del

        if (parts.length < 4) {
            System.out.println("用法: process path_white " + action + " [路径列表] [IP地址] 或 process path_white " + action + " [路径列表]");
            Logger.log("process path_white " + action + "命令参数不足");
            return;
        }

        // 解析参数
        List<String> parsedArgs = parseCommandArguments(parts, 3);
        if (parsedArgs.isEmpty()) {
            System.out.println("请提供路径列表");
            Logger.log("缺少路径列表");
            return;
        }

        // 处理路径列表和可能的IP地址
        String ipAddress = null;
        List<String> paths = new ArrayList<>();

        // 检查最后一个参数是否为IP地址
        if (parsedArgs.size() >= 1 && NetworkHelper.isValidIPv4Address(parsedArgs.get(parsedArgs.size() - 1))) {
            ipAddress = parsedArgs.get(parsedArgs.size() - 1);
            for (int i = 0; i < parsedArgs.size() - 1; i++) {
                paths.add(parsedArgs.get(i));
            }
        } else {
            paths.addAll(parsedArgs);
        }

        // 构造路径列表，用逗号连接
        String pathList = String.join(",", paths);

        if (ipAddress != null) {
            // 向指定IP发送命令
            try {
                InetAddress targetIp = InetAddress.getByName(ipAddress);
                UdpServer.sendProcessPathWhiteToSpecificIP(targetIp, action, pathList);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            // 广播命令
            UdpServer.sendProcessPathWhiteBroadcast(action, pathList);
        }
    }
    
    /**
     * 处理process path_mode命令
     */
    private static void handleProcessPathMode(String[] parts) {
        if (parts.length < 3 || (!"whitelist".equals(parts[2]) && !"blacklist".equals(parts[2]))) {
            System.out.println("用法: process path_mode whitelist [IP地址] 或 process path_mode whitelist");
            System.out.println("      process path_mode blacklist [IP地址] 或 process path_mode blacklist");
            Logger.log("process path_mode命令格式不正确");
            return;
        }

        String mode = parts[2]; // whitelist 或 blacklist

        if (parts.length == 4) {
            // 向指定IP发送命令: process path_mode [mode] [IP地址]
            String ipAddress = parts[3];
            if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                try {
                    InetAddress targetIp = InetAddress.getByName(ipAddress);
                    UdpServer.sendPathModeToSpecificIP(targetIp, mode);
                } catch (Exception e) {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else if (parts.length == 3) {
            // 广播命令: process path_mode [mode]
            UdpServer.sendPathModeBroadcast(mode);
        } else {
            System.out.println("用法: process path_mode whitelist [IP地址] 或 process path_mode whitelist");
            System.out.println("      process path_mode blacklist [IP地址] 或 process path_mode blacklist");
            Logger.log("process path_mode命令参数格式错误");
        }
    }
    
    /**
     * 处理process taskkill命令
     */
    private static void handleProcessTaskKill(String[] parts) {
        if (parts.length < 3) {
            System.out.println("用法: process taskkill [进程名称] [IP地址] 或 process taskkill [进程名称]");
            Logger.log("缺少进程名称");
            return;
        }

        // 解析参数
        List<String> parsedArgs = parseCommandArguments(parts, 2);
        if (parsedArgs.isEmpty()) {
            System.out.println("用法: process taskkill [进程名称] [IP地址] 或 process taskkill [进程名称]");
            Logger.log("缺少进程名称");
            return;
        }

        String processName;
        String ipAddress = null;

        // 检查最后一个参数是否为IP地址
        if (parsedArgs.size() >= 2 && NetworkHelper.isValidIPv4Address(parsedArgs.get(parsedArgs.size() - 1))) {
            ipAddress = parsedArgs.get(parsedArgs.size() - 1);
            processName = String.join(" ", parsedArgs.subList(0, parsedArgs.size() - 1));
        } else {
            processName = String.join(" ", parsedArgs);
        }

        // 处理引号包裹的进程名
        if (processName.startsWith("\"") && processName.endsWith("\"") && processName.length() > 1) {
            processName = processName.substring(1, processName.length() - 1);
        }

        if (ipAddress != null) {
            // 向指定IP发送命令
            try {
                InetAddress targetIp = InetAddress.getByName(ipAddress);
                UdpServer.sendProcessTaskKillToSpecificIP(targetIp, processName);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            // 广播命令
            UdpServer.sendProcessTaskKillBroadcast(processName);
        }
    }
    
    /**
     * 处理restore命令
     * @param parts 命令参数数组
     */
    public static void handleRestoreCommand(String[] parts) {
        // restore命令格式: restore config [IP地址] 或 restore config (广播)
        if (parts.length < 2 || !"config".equals(parts[1])) {
            System.out.println("用法: restore config [IP地址] 或 restore config");
            Logger.log("restore命令格式不正确");
            return;
        }

        if (parts.length == 2) {
            // 广播命令: restore config
            UdpServer.sendRestoreConfigBroadcast();
        } else if (parts.length == 3) {
            // 向指定IP发送命令: restore config [IP地址]
            String ipAddress = parts[2];
            if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                try {
                    InetAddress targetIp = InetAddress.getByName(ipAddress);
                    UdpServer.sendRestoreConfigToSpecificIP(targetIp);
                } catch (Exception e) {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            System.out.println("用法: restore config [IP地址] 或 restore config");
            Logger.log("restore config命令参数过多");
        }
    }
    
    /**
     * 处理disable命令
     * @param parts 命令参数数组
     */
    public static void handleDisableCommand(String[] parts) {
        // disable命令格式: disable nic_detection [IP地址] 或 disable all_features [IP地址]
        // 或: disable nic_detection (广播) 或 disable all_features (广播)
        if (parts.length < 2 || (!"nic_detection".equals(parts[1]) && !"all_features".equals(parts[1]))) {
            System.out.println("用法: disable nic_detection [IP地址] 或 disable nic_detection");
            System.out.println("      disable all_features [IP地址] 或 disable all_features");
            Logger.log("disable命令格式不正确");
            return;
        }

        if ("nic_detection".equals(parts[1])) {
            if (parts.length == 2) {
                // 广播命令: disable nic_detection
                UdpServer.sendDisableNicDetectionBroadcast();
            } else if (parts.length == 3) {
                // 向指定IP发送命令: disable nic_detection [IP地址]
                String ipAddress = parts[2];
                if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                    try {
                        InetAddress targetIp = InetAddress.getByName(ipAddress);
                        UdpServer.sendDisableNicDetectionToSpecificIP(targetIp);
                    } catch (Exception e) {
                        System.out.println("无效的IP地址: " + ipAddress);
                        Logger.log("无效的IP地址: " + ipAddress);
                    }
                } else {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("用法: disable nic_detection [IP地址] 或 disable nic_detection");
                Logger.log("disable nic_detection命令参数过多");
            }
        } else if ("all_features".equals(parts[1])) {
            if (parts.length == 2) {
                // 广播命令: disable all_features
                UdpServer.sendDisableAllFeaturesBroadcast();
            } else if (parts.length == 3) {
                // 向指定IP发送命令: disable all_features [IP地址]
                String ipAddress = parts[2];
                if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                    try {
                        InetAddress targetIp = InetAddress.getByName(ipAddress);
                        UdpServer.sendDisableAllFeaturesToSpecificIP(targetIp);
                    } catch (Exception e) {
                        System.out.println("无效的IP地址: " + ipAddress);
                        Logger.log("无效的IP地址: " + ipAddress);
                    }
                } else {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("用法: disable all_features [IP地址] 或 disable all_features");
                Logger.log("disable all_features命令参数过多");
            }
        }
    }
    
    /**
     * 处理黑白名单模式切换命令
     * @param parts 命令参数数组
     */
    public static void handleModeCommand(String[] parts) {
        // mode命令格式: mode whitelist [IP地址] 或 mode blacklist [IP地址]
        if (parts.length < 2 || (!"whitelist".equals(parts[1]) && !"blacklist".equals(parts[1]))) {
            System.out.println("用法: mode whitelist [IP地址] 或 mode whitelist");
            System.out.println("      mode blacklist [IP地址] 或 mode blacklist");
            Logger.log("mode命令格式不正确");
            return;
        }

        String mode = parts[1]; // whitelist 或 blacklist

        if (parts.length == 3) {
            // 向指定IP发送命令: mode [mode] [IP地址]
            String ipAddress = parts[2];
            if (NetworkHelper.isValidIPv4Address(ipAddress)) {
                try {
                    InetAddress targetIp = InetAddress.getByName(ipAddress);
                    UdpServer.sendModeToSpecificIP(targetIp, mode);
                } catch (Exception e) {
                    System.out.println("无效的IP地址: " + ipAddress);
                    Logger.log("无效的IP地址: " + ipAddress);
                }
            } else {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else if (parts.length == 2) {
            // 广播命令: mode [mode]
            UdpServer.sendModeBroadcast(mode);
        } else {
            System.out.println("用法: mode whitelist [IP地址] 或 mode whitelist");
            System.out.println("      mode blacklist [IP地址] 或 mode blacklist");
            Logger.log("mode命令参数格式错误");
        }
    }
    
    /**
     * 处理black命令（黑名单程序管理）
     * @param parts 命令参数数组
     */
    public static void handleBlackCommand(String[] parts) {
        // black命令格式: black add [程序列表] [IP地址] 或 black del [程序列表] [IP地址]
        if (parts.length < 2 || (!"add".equals(parts[1]) && !"del".equals(parts[1]))) {
            System.out.println("用法: black add [程序列表] [IP地址] 或 black add [程序列表]");
            System.out.println("      black del [程序列表] [IP地址] 或 black del [程序列表]");
            Logger.log("black命令格式不正确");
            return;
        }

        String action = parts[1]; // add 或 del

        if (parts.length < 3) {
            System.out.println("用法: black " + action + " [程序列表] [IP地址] 或 black " + action + " [程序列表]");
            Logger.log("black " + action + "命令参数不足");
            return;
        }

        // 解析参数
        List<String> parsedArgs = parseCommandArguments(parts, 2);
        if (parsedArgs.isEmpty()) {
            System.out.println("请提供程序列表");
            Logger.log("缺少程序列表");
            return;
        }

        // 处理程序列表和可能的IP地址
        String ipAddress = null;
        List<String> programs = new ArrayList<>();

        // 检查最后一个参数是否为IP地址
        if (parsedArgs.size() >= 1 && NetworkHelper.isValidIPv4Address(parsedArgs.get(parsedArgs.size() - 1))) {
            ipAddress = parsedArgs.get(parsedArgs.size() - 1);
            for (int i = 0; i < parsedArgs.size() - 1; i++) {
                programs.add(parsedArgs.get(i));
            }
        } else {
            programs.addAll(parsedArgs);
        }

        // 构造程序列表，用逗号连接
        String programList = String.join(",", programs);

        if (ipAddress != null) {
            // 向指定IP发送命令
            try {
                InetAddress targetIp = InetAddress.getByName(ipAddress);
                UdpServer.sendBlackToSpecificIP(targetIp, action, programList);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            // 广播命令
            UdpServer.sendBlackBroadcast(action, programList);
        }
    }
    
    /**
     * 处理width命令（白名单程序管理）
     * @param parts 命令参数数组
     */
    public static void handleWidthCommand(String[] parts) {
        // width命令格式: width add [程序列表] [IP地址] 或 width del [程序列表] [IP地址]
        if (parts.length < 2 || (!"add".equals(parts[1]) && !"del".equals(parts[1]))) {
            System.out.println("用法: width add [程序列表] [IP地址] 或 width add [程序列表]");
            System.out.println("      width del [程序列表] [IP地址] 或 width del [程序列表]");
            Logger.log("width命令格式不正确");
            return;
        }

        String action = parts[1]; // add 或 del

        if (parts.length < 3) {
            System.out.println("用法: width " + action + " [程序列表] [IP地址] 或 width " + action + " [程序列表]");
            Logger.log("width " + action + "命令参数不足");
            return;
        }

        // 解析参数
        List<String> parsedArgs = parseCommandArguments(parts, 2);
        if (parsedArgs.isEmpty()) {
            System.out.println("请提供程序列表");
            Logger.log("缺少程序列表");
            return;
        }

        // 处理程序列表和可能的IP地址
        String ipAddress = null;
        List<String> programs = new ArrayList<>();

        // 检查最后一个参数是否为IP地址
        if (parsedArgs.size() >= 1 && NetworkHelper.isValidIPv4Address(parsedArgs.get(parsedArgs.size() - 1))) {
            ipAddress = parsedArgs.get(parsedArgs.size() - 1);
            for (int i = 0; i < parsedArgs.size() - 1; i++) {
                programs.add(parsedArgs.get(i));
            }
        } else {
            programs.addAll(parsedArgs);
        }

        // 构造程序列表，用逗号连接
        String programList = String.join(",", programs);

        if (ipAddress != null) {
            // 向指定IP发送命令
            try {
                InetAddress targetIp = InetAddress.getByName(ipAddress);
                UdpServer.sendWidthToSpecificIP(targetIp, action, programList);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + ipAddress);
                Logger.log("无效的IP地址: " + ipAddress);
            }
        } else {
            // 广播命令
            UdpServer.sendWidthBroadcast(action, programList);
        }
    }
    
    /**
     * 处理cmd命令
     * @param parts 命令参数数组
     */
    public static void handleCmdCommand(String[] parts) {
        // cmd命令格式: cmd [IP地址] [命令] 或者 cmd [命令]
        if (parts.length < 2) {
            System.out.println("用法: cmd [IP地址] [命令] 或 cmd [命令]");
            Logger.log("cmd命令参数不足");
            return;
        }
        
        // 检查第二个参数是否为有效的IP地址
        if (parts.length >= 2 && NetworkHelper.isValidIPv4Address(parts[1])) {
            // 向指定IP发送命令: cmd [IP地址] [命令]
            String command = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
            try {
                InetAddress targetIp = InetAddress.getByName(parts[1]);
                UdpServer.sendCmdToSpecificIP(targetIp, command);
            } catch (Exception e) {
                System.out.println("无效的IP地址: " + parts[1]);
                Logger.log("无效的IP地址: " + parts[1]);
            }
        } else {
            // 广播命令: cmd [命令] (将所有参数合并为命令)
            String command = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            UdpServer.sendCmdBroadcast(command);
        }
    }
    
    /**
     * 解析命令参数，支持引号包裹的参数
     * @param parts 原始参数数组
     * @param startIndex 开始解析的索引
     * @return 解析后的参数列表
     */
    private static List<String> parseCommandArguments(String[] parts, int startIndex) {
        List<String> result = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        
        // 将从startIndex开始的所有参数重新组合
        String fullCommand = String.join(" ", java.util.Arrays.copyOfRange(parts, startIndex, parts.length));
        
        for (int i = 0; i < fullCommand.length(); i++) {
            char c = fullCommand.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                // 遇到空格且不在引号内，结束当前参数
                if (currentArg.length() > 0) {
                    result.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }
        
        // 添加最后一个参数
        if (currentArg.length() > 0) {
            result.add(currentArg.toString());
        }
        
        return result;
    }
}