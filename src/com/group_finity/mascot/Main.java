/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.Manager;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.MascotEventHandler;
import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.chat.ChatManager;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    static final String BEHAVIOR_GATHER = "\u30de\u30a6\u30b9\u306e\u5468\u308a\u306b\u96c6\u307e\u308b";
    private static Main instance;
    private final Manager manager = new Manager();
    private final Configuration configuration = new Configuration();

    public static Main getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        Main.getInstance().run();
    }

    public void run() {
        SettingsManager.getInstance().init(new File(System.getProperty("user.dir")));
        this.loadConfiguration();
        this.createTrayIcon();
        this.initChatSystem();
        this.createMascot();
        this.getManager().start();
    }

    /** 初始化 AI 对话系统 */
    private void initChatSystem() {
        try {
            // 获取程序运行目录
            File baseDir = new File(System.getProperty("user.dir"));
            ChatManager.getInstance().init(baseDir);
            log.info("AI 对话系统初始化完成");
        } catch (Exception e) {
            log.log(Level.WARNING, "AI 对话系统初始化失败（不影响桌宠基本功能）", e);
        }
    }

    private void loadConfiguration() {
        try {
            log.log(Level.INFO, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u3092\u8aad\u307f\u8fbc\u307f({0})", "/Behavior.xml");
            Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Main.class.getResourceAsStream("/Behavior.xml"));
            log.log(Level.INFO, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u3092\u8aad\u307f\u8fbc\u307f({0})", "/Actions.xml");
            this.getConfiguration().load(new Entry(actions.getDocumentElement()));
            Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Main.class.getResourceAsStream("/Actions.xml"));
            this.getConfiguration().load(new Entry(behaviors.getDocumentElement()));
            this.getConfiguration().validate();
        }
        catch (IOException e) {
            log.log(Level.SEVERE, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u306e\u8aad\u307f\u8fbc\u307f\u306b\u5931\u6557", e);
            this.exit();
        }
        catch (SAXException e) {
            log.log(Level.SEVERE, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u306e\u8aad\u307f\u8fbc\u307f\u306b\u5931\u6557", e);
            this.exit();
        }
        catch (ParserConfigurationException e) {
            log.log(Level.SEVERE, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u306e\u8aad\u307f\u8fbc\u307f\u306b\u5931\u6557", e);
            this.exit();
        }
        catch (ConfigurationException e) {
            log.log(Level.SEVERE, "\u8a2d\u5b9a\u30d5\u30a1\u30a4\u30eb\u306e\u8a18\u8ff0\u306b\u8aa4\u308a\u304c\u3042\u308a\u307e\u3059", e);
            this.exit();
        }
    }

    private void createTrayIcon() {
        log.log(Level.INFO, "\u30c8\u30ec\u30a4\u30a2\u30a4\u30b3\u30f3\u3092\u4f5c\u6210");
        if (SystemTray.getSystemTray() == null) {
            return;
        }
        MenuItem increaseMenu = new MenuItem("Add Dottore");
        increaseMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                Main.this.createMascot();
            }
        });
        MenuItem gatherMenu = new MenuItem("Gather All");
        gatherMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                Main.this.gatherAll();
            }
        });
        MenuItem oneMenu = new MenuItem("Keep One");
        oneMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                Main.this.remainOne();
            }
        });
        MenuItem restoreMenu = new MenuItem("Restore IE Windows");
        restoreMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                Main.this.restoreIE();
            }
        });
        MenuItem settingsMenu = new MenuItem("Settings");
        settingsMenu.addActionListener(e -> SettingsDialog.open(null));
        MenuItem restartMenu = new MenuItem("Restart");
        restartMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                Main.this.restart();
            }
        });
        MenuItem closeMenu = new MenuItem("Exit");
        closeMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                Main.this.exit();
            }
        });
        PopupMenu trayPopup = new PopupMenu();
        trayPopup.add(increaseMenu);
        trayPopup.add(gatherMenu);
        trayPopup.add(oneMenu);
        trayPopup.add(restoreMenu);
        trayPopup.add(settingsMenu);
        trayPopup.add(restartMenu);
        trayPopup.add(new MenuItem("-"));
        trayPopup.add(closeMenu);
        try {
            TrayIcon icon = new TrayIcon(ImageIO.read(Main.class.getResource("/icon.png")), "Dottore Desktop Pet", trayPopup);
            icon.setImageAutoSize(true);
            icon.addMouseListener(new MouseAdapter(){

                @Override
                public void mouseClicked(MouseEvent e) {
                    // 禁用托盘左键创建新小人
                }
            });
            SystemTray.getSystemTray().add(icon);
        }
        catch (IOException e) {
            log.log(Level.SEVERE, "\u30c8\u30ec\u30a4\u30a2\u30a4\u30b3\u30f3\u306e\u4f5c\u6210\u306b\u5931\u6557", e);
            this.exit();
        }
        catch (AWTException e) {
            log.log(Level.SEVERE, "\u30c8\u30ec\u30a4\u30a2\u30a4\u30b3\u30f3\u306e\u4f5c\u6210\u306b\u5931\u6557", e);
            MascotEventHandler.setShowSystemTrayMenu(true);
            this.getManager().setExitOnLastRemoved(true);
        }
    }

    public void createMascot() {
        log.log(Level.INFO, "\u30de\u30b9\u30b3\u30c3\u30c8\u3092\u4f5c\u6210");
        Mascot mascot = new Mascot();
        mascot.setAnchor(new Point(-1000, -1000));
        mascot.setLookRight(Math.random() < 0.5);
        try {
            mascot.setBehavior(this.getConfiguration().buildBehavior(null, mascot));
            this.getManager().add(mascot);
        }
        catch (BehaviorInstantiationException e) {
            log.log(Level.SEVERE, "\u6700\u521d\u306e\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
            mascot.dispose();
        }
        catch (CantBeAliveException e) {
            log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
            mascot.dispose();
        }
    }

    public void gatherAll() {
        this.getManager().setBehaviorAll(this.getConfiguration(), BEHAVIOR_GATHER);
    }

    public void remainOne() {
        this.getManager().remainOne();
    }

    public void restoreIE() {
        NativeFactory.getInstance().getEnvironment().restoreIE();
    }

    public void applyVisualSettings() {
        this.getManager().applyVisualSettings();
    }

    public void restart() {
        log.info("重启应用...");
        this.getManager().disposeAll();
        this.getManager().stop();
        // 重新创建
        this.createMascot();
        this.getManager().start();
        log.info("重启完成");
    }

    public void exit() {
        this.getManager().disposeAll();
        this.getManager().stop();
        System.exit(0);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    private Manager getManager() {
        return this.manager;
    }

    static {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        instance = new Main();
    }
}
