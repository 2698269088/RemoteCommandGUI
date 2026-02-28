package top.mcocet.rcgui.gui;

import javax.swing.*;

/**
 * GUI应用程序启动器
 * 启动远程命令GUI界面
 */
public class GuiLauncher {
    
    /**
     * 主方法 - 启动GUI应用程序
     */
    public static void main(String[] args) {
        // 在事件调度线程中创建和显示GUI
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow mainWindow = new MainWindow();
                mainWindow.showWindow();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "启动GUI时发生错误: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}