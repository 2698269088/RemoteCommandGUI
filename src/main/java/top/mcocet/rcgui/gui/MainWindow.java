package top.mcocet.rcgui.gui;

import top.mcocet.rcgui.handler.CommandHandler;
import top.mcocet.rcgui.server.*;
import top.mcocet.rcgui.util.Logger;

import static top.mcocet.rcgui.util.Logger.addLogListener;
import static top.mcocet.rcgui.util.Logger.removeLogListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 远程命令GUI主窗口
 * 提供图形化界面来发送各种远程命令
 */
public class MainWindow extends JFrame implements Logger.LogUpdateListener {
    private JTextField ipField;
    private JTextArea commandArea;
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    private JButton sendButton;
    
    // 黑白名单相关组件
    private JTextField pathInputField;
    private JTextField programInputField;
    
    // 服务状态标签
    private JLabel httpStatusLabel;
    private JLabel ftpStatusLabel;
    private JLabel udpStatusLabel;
    
    public MainWindow() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setTitle("远程命令GUI控制器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // IP地址输入框
        ipField = new JTextField(15);
        ipField.setToolTipText("输入目标IP地址，留空则广播到所有设备");
        
        // 命令显示区域
        commandArea = new JTextArea(3, 30);
        commandArea.setEditable(false);
        commandArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandArea.setBorder(BorderFactory.createTitledBorder("当前命令"));
        
        // 日志显示区域
        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("运行日志"));
        
        // 发送按钮
        sendButton = new JButton("发送命令");
        sendButton.setFont(new Font(null, Font.BOLD, 12));
        
        // 黑白名单输入框
        pathInputField = new JTextField(20);
        pathInputField.setToolTipText("输入路径，多个路径用分号分隔");
        
        programInputField = new JTextField(20);
        programInputField.setToolTipText("输入程序名，多个程序用分号分隔");
        
        // 服务状态标签
        httpStatusLabel = new JLabel("HTTP: 停止");
        ftpStatusLabel = new JLabel("FTP: 停止");
        udpStatusLabel = new JLabel("UDP接收: 停止");
        
        updateLog("远程命令GUI控制器启动");
        updateLog("请输入IP地址并选择要发送的命令");
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部面板 - IP输入和状态
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ipPanel.add(new JLabel("目标IP地址:"));
        ipPanel.add(ipField);
        ipPanel.setBorder(BorderFactory.createTitledBorder("目标设置"));
        
        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        statusPanel.add(httpStatusLabel);
        statusPanel.add(ftpStatusLabel);
        statusPanel.add(udpStatusLabel);
        statusPanel.setBorder(BorderFactory.createTitledBorder("服务状态"));
        
        topPanel.add(ipPanel, BorderLayout.CENTER);
        topPanel.add(statusPanel, BorderLayout.EAST);
        
        // 中部面板 - 命令按钮区域
        JPanel centerPanel = createCommandButtonsPanel();
        
        // 底部面板 - 输入框和命令显示及日志
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // 输入框面板
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("路径输入:"));
        inputPanel.add(pathInputField);
        inputPanel.add(Box.createHorizontalStrut(20));
        inputPanel.add(new JLabel("程序输入:"));
        inputPanel.add(programInputField);
        inputPanel.setBorder(BorderFactory.createTitledBorder("黑白名单输入"));
        
        // 主要内容面板
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(commandArea, BorderLayout.NORTH);
        mainContentPanel.add(logScrollPane, BorderLayout.CENTER);
        
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(mainContentPanel, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.SOUTH);
        
        // 添加到主窗口
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建命令按钮面板
     */
    private JPanel createCommandButtonsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 第一行 - 服务控制命令
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createCommandButton("启动HTTP", "start http"), gbc);
        
        gbc.gridx = 1;
        panel.add(createCommandButton("停止HTTP", "stop http"), gbc);
        
        gbc.gridx = 2;
        panel.add(createCommandButton("启动FTP", "start ftp"), gbc);
        
        gbc.gridx = 3;
        panel.add(createCommandButton("停止FTP", "stop ftp"), gbc);
        
        gbc.gridx = 4;
        panel.add(createCommandButton("启动UDP接收", "start udprcv"), gbc);
        
        gbc.gridx = 5;
        panel.add(createCommandButton("停止UDP接收", "stop udprcv"), gbc);
        
        // 第二行 - 广播命令
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createCommandButton("广播运行命令", "broadcast runcmd"), gbc);
        
        gbc.gridx = 1;
        panel.add(createCommandButton("广播更新命令", "broadcast update"), gbc);
        
        gbc.gridx = 2;
        panel.add(createCommandButton("发送链接信息", "link"), gbc);
        
        gbc.gridx = 3;
        panel.add(createCommandButton("还原所有配置", "restore config"), gbc);
        
        gbc.gridx = 4;
        panel.add(createCommandButton("关闭网卡检测", "disable nic_detection"), gbc);
        
        gbc.gridx = 5;
        panel.add(createCommandButton("关闭所有功能", "disable all_features"), gbc);
        
        // 第三行 - 进程管理命令
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createCommandButton("查看进程列表", "process list"), gbc);
        
        gbc.gridx = 1;
        panel.add(createCommandButton("查看应用进程", "process app"), gbc);
        
        gbc.gridx = 2;
        panel.add(createProcessTaskKillButton(), gbc);
        
        gbc.gridx = 3;
        panel.add(createCustomCommandPanel(), gbc);
        
        // 第四行 - 黑白名单管理命令
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createCommandButton("切换白名单模式", "mode whitelist"), gbc);
        
        gbc.gridx = 1;
        panel.add(createCommandButton("切换黑名单模式", "mode blacklist"), gbc);
        
        gbc.gridx = 2;
        panel.add(createAddPathBlackButton(), gbc);
        
        gbc.gridx = 3;
        panel.add(createAddPathWhiteButton(), gbc);
        
        gbc.gridx = 4;
        panel.add(createAddProgramBlackButton(), gbc);
        
        gbc.gridx = 5;
        panel.add(createAddProgramWhiteButton(), gbc);
        
        // 第五行 - 重启和其他命令
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(createRestartButton(), gbc);
        
        gbc.gridx = 1;
        panel.add(createClearLogButton(), gbc);
        
        panel.setBorder(BorderFactory.createTitledBorder("命令控制面板"));
        return panel;
    }
    
    /**
     * 创建标准命令按钮
     */
    private JButton createCommandButton(String buttonText, String command) {
        JButton button = new JButton(buttonText);
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> executeCommand(command));
        return button;
    }
    
    /**
     * 创建进程结束按钮（带输入对话框）
     */
    private JButton createProcessTaskKillButton() {
        JButton button = new JButton("结束进程");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            String processName = JOptionPane.showInputDialog(this, 
                "请输入要结束的进程名称:", "结束进程", JOptionPane.QUESTION_MESSAGE);
            if (processName != null && !processName.trim().isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "process taskkill \"" + processName + "\"" : 
                    "process taskkill \"" + processName + "\" " + ip;
                executeCommand(command);
            }
        });
        return button;
    }
    
    /**
     * 创建自定义命令面板
     */
    private JPanel createCustomCommandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField customCmdField = new JTextField(15);
        JButton customSendBtn = new JButton("发送");
        
        customCmdField.setToolTipText("输入自定义命令");
        customSendBtn.addActionListener(e -> {
            String customCmd = customCmdField.getText().trim();
            if (!customCmd.isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "cmd " + customCmd : 
                    "cmd " + ip + " " + customCmd;
                executeCommand(command);
                customCmdField.setText("");
            }
        });
        
        panel.add(customCmdField, BorderLayout.CENTER);
        panel.add(customSendBtn, BorderLayout.EAST);
        panel.setBorder(BorderFactory.createTitledBorder("自定义命令"));
        panel.setPreferredSize(new Dimension(200, 60));
        
        return panel;
    }
    
    /**
     * 创建添加黑名单路径按钮
     */
    private JButton createAddPathBlackButton() {
        JButton button = new JButton("添加黑路径");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            String paths = pathInputField.getText().trim();
            if (!paths.isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "process path_black add \"" + paths + "\"" : 
                    "process path_black add \"" + paths + "\" " + ip;
                executeCommand(command);
                pathInputField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "请输入路径！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        return button;
    }
    
    /**
     * 创建添加白名单路径按钮
     */
    private JButton createAddPathWhiteButton() {
        JButton button = new JButton("添加白路径");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            String paths = pathInputField.getText().trim();
            if (!paths.isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "process path_white add \"" + paths + "\"" : 
                    "process path_white add \"" + paths + "\" " + ip;
                executeCommand(command);
                pathInputField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "请输入路径！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        return button;
    }
    
    /**
     * 创建添加黑名单程序按钮
     */
    private JButton createAddProgramBlackButton() {
        JButton button = new JButton("添加黑程序");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            String programs = programInputField.getText().trim();
            if (!programs.isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "black add \"" + programs + "\"" : 
                    "black add \"" + programs + "\" " + ip;
                executeCommand(command);
                programInputField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "请输入程序名！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        return button;
    }
    
    /**
     * 创建添加白名单程序按钮
     */
    private JButton createAddProgramWhiteButton() {
        JButton button = new JButton("添加白程序");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            String programs = programInputField.getText().trim();
            if (!programs.isEmpty()) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "width add \"" + programs + "\"" : 
                    "width add \"" + programs + "\" " + ip;
                executeCommand(command);
                programInputField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "请输入程序名！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        return button;
    }
    
    /**
     * 创建重启按钮
     */
    private JButton createRestartButton() {
        JButton button = new JButton("一键重启");
        button.setPreferredSize(new Dimension(120, 30));
        button.setBackground(new Color(255, 99, 71)); // 红色背景
        button.setForeground(Color.WHITE);
        button.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, 
                "确定要执行重启命令吗？\n这将运行: shutdown /r /t 5", 
                "确认重启", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                String ip = ipField.getText().trim();
                String command = ip.isEmpty() ? 
                    "cmd shutdown /r /t 5" : 
                    "cmd " + ip + " shutdown /r /t 5";
                executeCommand(command);
            }
        });
        return button;
    }
    
    /**
     * 创建清空日志按钮
     */
    private JButton createClearLogButton() {
        JButton button = new JButton("清空日志");
        button.setPreferredSize(new Dimension(120, 30));
        button.addActionListener(e -> {
            logArea.setText("");
            updateLog("日志已清空");
        });
        return button;
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 发送按钮事件
        sendButton.addActionListener(e -> {
            String displayedCommand = commandArea.getText().trim();
            if (!displayedCommand.isEmpty() && !displayedCommand.equals("当前命令")) {
                // 从显示的命令中提取实际命令
                executeDisplayedCommand(displayedCommand);
            }
        });
    }
    
    /**
     * 执行命令
     */
    private void executeCommand(String command) {
        try {
            String ip = ipField.getText().trim();
            String fullCommand = ip.isEmpty() ? command : command + " " + ip;
            
            updateCommandDisplay(fullCommand);
            updateLog("准备执行命令: " + fullCommand);
            
            // 解析并执行命令
            String[] parts = fullCommand.split("\\s+");
            switch (parts[0].toLowerCase()) {
                case "start":
                case "stop":
                    handleServiceCommand(parts);
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
                case "mode":
                    CommandHandler.handleModeCommand(parts);
                    break;
                case "black":
                    CommandHandler.handleBlackCommand(parts);
                    break;
                case "width":
                    CommandHandler.handleWidthCommand(parts);
                    break;
                default:
                    updateLog("未知命令类型: " + parts[0]);
                    break;
            }
            
            updateLog("命令发送完成");
        } catch (Exception ex) {
            updateLog("执行命令时出错: " + ex.getMessage());
            Logger.error("GUI执行命令错误: " + ex.getMessage());
        }
    }
    
    /**
     * 执行显示区域中的命令
     */
    private void executeDisplayedCommand(String displayedCommand) {
        // 移除"当前命令:"前缀
        if (displayedCommand.startsWith("当前命令:")) {
            displayedCommand = displayedCommand.substring("当前命令:".length()).trim();
        }
        executeCommand(displayedCommand);
    }
    
    /**
     * 处理服务控制命令
     */
    private void handleServiceCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "http":
                    if ("start".equals(parts[0])) {
                        HttpServerManager.start();
                        httpStatusLabel.setText("HTTP: 运行中");
                    } else {
                        HttpServerManager.stop();
                        httpStatusLabel.setText("HTTP: 停止");
                    }
                    break;
                case "ftp":
                    if ("start".equals(parts[0])) {
                        FtpServer.start();
                        ftpStatusLabel.setText("FTP: 运行中");
                    } else {
                        FtpServer.stop();
                        ftpStatusLabel.setText("FTP: 停止");
                    }
                    break;
                case "udprcv":
                    if ("start".equals(parts[0])) {
                        UdpServer.startUdpLogReceiver();
                        udpStatusLabel.setText("UDP接收: 运行中");
                    } else {
                        UdpServer.stopUdpLogReceiver();
                        udpStatusLabel.setText("UDP接收: 停止");
                    }
                    break;
            }
        }
    }
    
    /**
     * 处理广播命令
     */
    private void handleBroadcastCommand(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1]) {
                case "runcmd":
                    UdpServer.sendRunCmdBroadcast();
                    break;
                case "update":
                    UdpServer.sendUpdateBroadcast();
                    break;
            }
        }
    }
    
    /**
     * 更新命令显示区域
     */
    private void updateCommandDisplay(String command) {
        commandArea.setText("当前命令: " + command);
    }
    
    /**
     * 更新日志显示区域
     */
    private void updateLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    /**
     * 实现日志更新监听器接口
     * @param logMessage 日志消息
     */
    @Override
    public void onLogUpdate(String logMessage) {
        updateLogFromLogger(logMessage);
    }
    
    /**
     * 从Logger更新日志显示
     * @param logMessage 日志消息
     */
    private void updateLogFromLogger(String logMessage) {
        SwingUtilities.invokeLater(() -> {
            // 移除时间戳前缀，因为logMessage已经包含完整的时间戳
            logArea.append(logMessage + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    /**
     * 显示窗口
     */
    public void showWindow() {
        // 注册日志监听器
        addLogListener(this);
        setVisible(true);
    }
    
    /**
     * 重写dispose方法，清理资源
     */
    @Override
    public void dispose() {
        // 移除日志监听器
        removeLogListener(this);
        super.dispose();
    }
}