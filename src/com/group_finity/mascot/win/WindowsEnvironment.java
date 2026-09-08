/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.platform.win32.GDI32
 *  com.sun.jna.platform.win32.User32
 *  com.sun.jna.platform.win32.WinDef$DWORD
 *  com.sun.jna.platform.win32.WinDef$HRGN
 *  com.sun.jna.platform.win32.WinDef$HWND
 *  com.sun.jna.platform.win32.WinDef$RECT
 *  com.sun.jna.platform.win32.WinNT$HANDLE
 */
package com.group_finity.mascot.win;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.win.jna.GDI32Ex;
import com.group_finity.mascot.win.jna.User32Ex;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashMap;

class WindowsEnvironment
extends Environment {
    private static HashMap<WinDef.HWND, Boolean> ieCache = new LinkedHashMap<WinDef.HWND, Boolean>();
    static int f;
    public static Area workArea;
    public static Area activeIE;

    WindowsEnvironment() {
    }

    private static Rectangle getWorkAreaRect() {
        WinDef.RECT rect = new WinDef.RECT();
        User32Ex.INSTANCE.SystemParametersInfoW(48, 0, rect, 0);
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    private static boolean isIE(WinDef.HWND ie) {
        Boolean cache = ieCache.get(ie);
        if (cache != null) {
            return cache;
        }
        char[] title = new char[1024];
        int titleLength = User32.INSTANCE.GetWindowText(ie, title, 1024);
        if (new String(title, 0, titleLength).contains("Internet Explorer")) {
            ieCache.put(ie, true);
            return true;
        }
        if (new String(title, 0, titleLength).contains("Google Chrome")) {
            ieCache.put(ie, true);
            return true;
        }
        if (new String(title, 0, titleLength).contains("Mozilla Firefox")) {
            ieCache.put(ie, true);
            return true;
        }
        char[] className = new char[1024];
        int classNameLength = User32.INSTANCE.GetClassName(ie, className, 1024);
        if (new String(className, 0, classNameLength).contains("IMWindowClass")) {
            ieCache.put(ie, true);
            return true;
        }
        if (new String(className, 0, classNameLength).contains("SleipnirMainWindow")) {
            ieCache.put(ie, true);
            return true;
        }
        if (new String(className, 0, classNameLength).contains("OperaWindowClass")) {
            ieCache.put(ie, true);
            return true;
        }
        ieCache.put(ie, false);
        return false;
    }

    private static WinDef.HWND findActiveIE() {
        WinDef.HWND ie = User32.INSTANCE.GetWindow(User32.INSTANCE.GetForegroundWindow(), new WinDef.DWORD(0L));
        while (User32Ex.INSTANCE.IsWindow(ie) != 0) {
            if (User32.INSTANCE.IsWindowVisible(ie)) {
                if ((User32.INSTANCE.GetWindowLong(ie, -16) & 0x1000000) != 0) {
                    return null;
                }
                if (WindowsEnvironment.isIE(ie) && User32Ex.INSTANCE.IsIconic(ie) == 0) break;
            }
            ie = User32.INSTANCE.GetWindow(ie, new WinDef.DWORD(2L));
        }
        if (User32Ex.INSTANCE.IsWindow(ie) == 0) {
            return null;
        }
        return ie;
    }

    private static Rectangle getActiveIERect() {
        WinDef.HWND ie = WindowsEnvironment.findActiveIE();
        WinDef.RECT out = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(ie, out);
        WinDef.RECT in = new WinDef.RECT();
        if (WindowsEnvironment.getWindowRgnBox(ie, in) == 0) {
            in.left = 0;
            in.top = 0;
            in.right = out.right - out.left;
            in.bottom = out.bottom - out.top;
        }
        return new Rectangle(out.left + in.left, out.top + in.top, in.right - in.left, in.bottom - in.top);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int getWindowRgnBox(WinDef.HWND window, WinDef.RECT rect) {
        WinDef.HRGN hRgn = GDI32.INSTANCE.CreateRectRgn(0, 0, 0, 0);
        try {
            if (User32Ex.INSTANCE.GetWindowRgn(window, hRgn) == 0) {
                int n = 0;
                return n;
            }
            GDI32Ex.INSTANCE.GetRgnBox(hRgn, rect);
            int n = 1;
            return n;
        }
        finally {
            GDI32.INSTANCE.DeleteObject((WinNT.HANDLE)hRgn);
        }
    }

    private static boolean moveIE(WinDef.HWND ie, Rectangle rect) {
        if (ie == null) {
            return false;
        }
        WinDef.RECT out = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(ie, out);
        WinDef.RECT in = new WinDef.RECT();
        if (WindowsEnvironment.getWindowRgnBox(ie, in) == 0) {
            in.left = 0;
            in.top = 0;
            in.right = out.right - out.left;
            in.bottom = out.bottom - out.top;
        }
        User32.INSTANCE.MoveWindow(ie, rect.x - in.left, rect.y - in.top, rect.width + (out.right - out.left) - (in.right - in.left), rect.height + (out.bottom - out.top) - (in.bottom - in.top), true);
        return true;
    }

    private static void restoreAllIEs() {
        WinDef.RECT workArea = new WinDef.RECT();
        User32Ex.INSTANCE.SystemParametersInfoW(48, 0, workArea, 0);
        WinDef.HWND ie = User32.INSTANCE.GetWindow(User32.INSTANCE.GetForegroundWindow(), new WinDef.DWORD(0L));
        while (User32Ex.INSTANCE.IsWindow(ie) != 0) {
            if (WindowsEnvironment.isIE(ie)) {
                WinDef.RECT rect = new WinDef.RECT();
                User32.INSTANCE.GetWindowRect(ie, rect);
                if (rect.right > workArea.left + 100 && rect.bottom > workArea.top + 100 && rect.left < workArea.right - 100 && rect.top < workArea.bottom - 100) break;
                int offsetX = workArea.left + 100 - rect.left;
                int offsetY = workArea.top + 100 - rect.top;
                rect.left += offsetX;
                rect.right += offsetX;
                rect.top += offsetY;
                rect.bottom += offsetY;
                User32.INSTANCE.MoveWindow(ie, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, true);
                User32Ex.INSTANCE.BringWindowToTop(ie);
                break;
            }
            ie = User32.INSTANCE.GetWindow(ie, new WinDef.DWORD(2L));
        }
    }

    @Override
    public void tick() {
        super.tick();
        workArea.set(WindowsEnvironment.getWorkAreaRect());
        Rectangle ieRect = WindowsEnvironment.getActiveIERect();
        activeIE.setVisible(ieRect != null && ieRect.intersects(this.getScreen().toRectangle()));
        activeIE.set(ieRect == null ? new Rectangle(-1, -1, 0, 0) : ieRect);
    }

    @Override
    public void moveActiveIE(Point point) {
        WindowsEnvironment.moveIE(WindowsEnvironment.findActiveIE(), new Rectangle(point.x, point.y, activeIE.getWidth(), activeIE.getHeight()));
    }

    @Override
    public void restoreIE() {
        WindowsEnvironment.restoreAllIEs();
    }

    @Override
    public Area getWorkArea() {
        return workArea;
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }

    static {
        workArea = new Area();
        activeIE = new Area();
    }
}

