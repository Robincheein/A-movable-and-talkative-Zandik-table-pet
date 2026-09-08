/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.chat.ChatManager;
import com.group_finity.mascot.exception.CantBeAliveException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class MascotEventHandler
implements MouseListener, MouseMotionListener {
    private static final Logger log = Logger.getLogger(MascotEventHandler.class.getName());
    private static boolean showSystemTrayMenu = false;
    private final Mascot mascot;

    /** 按下时的屏幕坐标。窗口会随拖动移动，不能使用组件内坐标判断距离。 */
    private Point pressScreenPoint = null;
    /** 保存的原始 press 事件，超过移动阈值后才交给拖动行为。 */
    private MouseEvent savedPressEvent = null;
    private boolean dragging = false;
    /** 超过该距离才开始拖动，轻微手抖仍按单击处理。 */
    private static final double DRAG_START_DISTANCE = 8.0;

    public static void setShowSystemTrayMenu(boolean showSystemTrayMenu) {
        MascotEventHandler.showSystemTrayMenu = showSystemTrayMenu;
    }

    public static boolean isShowSystemTrayMenu() {
        return showSystemTrayMenu;
    }

    public MascotEventHandler(Mascot mascot) {
        this.mascot = mascot;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }
        this.pressScreenPoint = event.getLocationOnScreen();
        this.savedPressEvent = event;
        this.dragging = false;
    }

    @Override
    public void mouseReleased(final MouseEvent event) {
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    MascotEventHandler.this.showPopup(event.getX(), event.getY());
                }
            });
            return;
        }

        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        if (!this.dragging) {
            SwingUtilities.invokeLater(() -> openChatDialog());
        } else if (this.getMascot().getBehavior() != null) {
            try {
                this.getMascot().getBehavior().mouseReleased(event);
            } catch (CantBeAliveException e) {
                log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
                this.getMascot().dispose();
            }
        }
        this.pressScreenPoint = null;
        this.savedPressEvent = null;
        this.dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (this.dragging || this.pressScreenPoint == null
                || (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
            return;
        }
        if (this.pressScreenPoint.distance(event.getLocationOnScreen()) < DRAG_START_DISTANCE) {
            return;
        }

        this.dragging = true;
        if (this.getMascot().getBehavior() != null && this.savedPressEvent != null) {
            try {
                this.getMascot().getBehavior().mousePressed(this.savedPressEvent);
            } catch (CantBeAliveException e) {
                log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
                this.getMascot().dispose();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    /** \u6253\u5f00 AI \u5bf9\u8bdd\u6846 */
    private void openChatDialog() {
        try {
            ChatManager.getInstance().openChatDialog(getMascot());
        } catch (Exception e) {
            log.log(Level.WARNING, "\u65e0\u6cd5\u6253\u5f00 AI \u5bf9\u8bdd", e);
            e.printStackTrace();
            // \u8c03\u8bd5\u5f39\u7a97
            javax.swing.JOptionPane.showMessageDialog(null,
                "AI \u5bf9\u8bdd\u6253\u5f00\u5931\u8d25: " + e.getMessage(), "\u8c03\u8bd5",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPopup(int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        popup.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                MascotEventHandler.this.getMascot().setAnimating(true);
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                MascotEventHandler.this.getMascot().setAnimating(false);
            }
        });
        JMenuItem chatMenu = new JMenuItem("\u5bf9\u8bdd");
        chatMenu.addActionListener(e -> openChatDialog());
        popup.add(chatMenu);
        JMenuItem settingsMenu = new JMenuItem("设置");
        settingsMenu.addActionListener(e -> SettingsDialog.open(getMascot()));
        popup.add(settingsMenu);
        popup.add(new JSeparator());

        JMenuItem disposeMenu = new JMenuItem("\u79fb\u9664\u8fd9\u4e2a\u684c\u5ba0");
        disposeMenu.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                MascotEventHandler.this.getMascot().dispose();
            }
        });
        popup.add(disposeMenu);
        if (MascotEventHandler.isShowSystemTrayMenu()) {
            popup.add(new JSeparator());
            JMenuItem increaseMenu = new JMenuItem("\u589e\u52a0\u4e00\u4e2a\u591a\u6258\u96f7");
            increaseMenu.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent event) {
                    Main.getInstance().createMascot();
                }
            });
            JMenuItem gatherMenu = new JMenuItem("\u5168\u90e8\u96c6\u5408\u5230\u8fd9\u91cc");
            gatherMenu.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent event) {
                    MascotEventHandler.this.getMascot().getManager().setBehaviorAll(Main.getInstance().getConfiguration(), "\u30de\u30a6\u30b9\u306e\u5468\u308a\u306b\u96c6\u307e\u308b");
                }
            });
            JMenuItem oneMenu = new JMenuItem("\u53ea\u4fdd\u7559\u4e00\u4e2a");
            oneMenu.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent event) {
                    MascotEventHandler.this.getMascot().getManager().remainOne();
                }
            });
            JMenuItem restoreMenu = new JMenuItem("\u8fd8\u539f IE \u7a97\u53e3");
            restoreMenu.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent event) {
                    NativeFactory.getInstance().getEnvironment().restoreIE();
                }
            });
            JMenuItem closeMenu = new JMenuItem("\u9000\u51fa\u7a0b\u5e8f");
            closeMenu.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    Main.getInstance().exit();
                }
            });
            popup.add(increaseMenu);
            popup.add(gatherMenu);
            popup.add(oneMenu);
            popup.add(restoreMenu);
            popup.add(new JSeparator());
            popup.add(closeMenu);
        }
        popup.show(this.getMascot().getWindow().asJWindow(), x, y);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private Mascot getMascot() {
        return this.mascot;
    }
}

