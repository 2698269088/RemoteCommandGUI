package top.mcocet.rcgui.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络辅助工具类
 * 提供网络接口和IP地址相关的工具方法
 */
public class NetworkHelper {
    
    /**
     * 获取所有IPv4地址
     * @return IPv4地址列表
     */
    public static List<InetAddress> getAllIPv4Addresses() {
        List<InetAddress> ipv4Addresses = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                
                // 检查网络接口是否启用
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress addr = inetAddresses.nextElement();
                        // 只获取IPv4地址
                        if (addr.getAddress().length == 4) {
                            ipv4Addresses.add(addr);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("获取网络接口信息时出错: " + e.getMessage());
        }
        
        return ipv4Addresses;
    }
    
    /**
     * 获取本机所有网络接口名称和IP地址
     * @return 网络接口信息字符串
     */
    public static String getNetworkInterfacesInfo() {
        StringBuilder info = new StringBuilder();
        info.append("网络接口信息:\n");
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                
                if (ni.isUp()) {
                    info.append("接口名称: ").append(ni.getName())
                        .append(" (").append(ni.getDisplayName()).append(")\n");
                    
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress addr = inetAddresses.nextElement();
                        info.append("  IP地址: ").append(addr.getHostAddress())
                            .append(" (").append(addr.isLoopbackAddress() ? "回环" : "外部").append(")\n");
                    }
                    info.append("\n");
                }
            }
        } catch (SocketException e) {
            info.append("获取网络接口信息失败: ").append(e.getMessage());
        }
        
        return info.toString();
    }
    
    /**
     * 验证IP地址格式是否有效
     * @param ipAddress IP地址字符串
     * @return 是否为有效的IPv4地址
     */
    public static boolean isValidIPv4Address(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}