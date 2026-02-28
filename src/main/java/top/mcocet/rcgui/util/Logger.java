package top.mcocet.rcgui.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志记录工具类
 * 提供统一的日志记录功能
 */
public class Logger {
    // GUI日志更新监听器列表
    private static final List<LogUpdateListener> listeners = new ArrayList<>();
    private static final String LOG_FILE_PATH = "server.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 日志更新监听器接口
     */
    public interface LogUpdateListener {
        void onLogUpdate(String logMessage);
    }
    
    /**
     * 注册GUI日志监听器
     * @param listener 监听器实例
     */
    public static void addLogListener(LogUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除GUI日志监听器
     * @param listener 监听器实例
     */
    public static void removeLogListener(LogUpdateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知所有监听器日志更新
     * @param logMessage 日志消息
     */
    private static void notifyListeners(String logMessage) {
        // 创建副本避免并发修改异常
        List<LogUpdateListener> listenersCopy = new ArrayList<>(listeners);
        for (LogUpdateListener listener : listenersCopy) {
            try {
                listener.onLogUpdate(logMessage);
            } catch (Exception e) {
                // 忽略监听器异常，避免影响主流程
            }
        }
    }
    
    /**
     * 记录日志信息到文件和控制台
     * @param message 日志消息
     */
    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = "[" + timestamp + "] " + message;
        
        // 输出到控制台
        System.out.println(logEntry);
        
        // 写入日志文件
        writeToFile(logEntry);
        
        // 通知GUI监听器
        notifyListeners(logEntry);
    }
    
    /**
     * 记录错误信息
     * @param message 错误消息
     */
    public static synchronized void error(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = "[" + timestamp + "] [ERROR] " + message;
        
        // 输出到控制台（红色）
        System.err.println(logEntry);
        
        // 写入日志文件
        writeToFile(logEntry);
        
        // 通知GUI监听器
        notifyListeners(logEntry);
    }
    
    /**
     * 记录警告信息
     * @param message 警告消息
     */
    public static synchronized void warn(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = "[" + timestamp + "] [WARN] " + message;
        
        // 输出到控制台
        System.out.println(logEntry);
        
        // 写入日志文件
        writeToFile(logEntry);
        
        // 通知GUI监听器
        notifyListeners(logEntry);
    }
    
    /**
     * 将日志写入文件
     * @param logEntry 日志条目
     */
    private static void writeToFile(String logEntry) {
        try {
            // 确保日志目录存在
            File logFile = new File(LOG_FILE_PATH);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 追加写入日志文件
            try (FileWriter writer = new FileWriter(logFile, true);
                 PrintWriter printWriter = new PrintWriter(writer)) {
                printWriter.println(logEntry);
            }
        } catch (IOException e) {
            System.err.println("写入日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空日志文件
     */
    public static void clearLogFile() {
        try {
            Files.write(Paths.get(LOG_FILE_PATH), new byte[0]);
            log("日志文件已清空");
        } catch (IOException e) {
            error("清空日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取日志文件大小
     * @return 日志文件大小（字节）
     */
    public static long getLogFileSize() {
        File logFile = new File(LOG_FILE_PATH);
        return logFile.exists() ? logFile.length() : 0;
    }
    
    /**
     * 检查日志文件是否存在
     * @return 是否存在日志文件
     */
    public static boolean isLogFileExists() {
        return new File(LOG_FILE_PATH).exists();
    }
}