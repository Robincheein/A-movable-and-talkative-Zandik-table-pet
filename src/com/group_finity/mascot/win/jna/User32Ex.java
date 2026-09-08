/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Native
 *  com.sun.jna.platform.win32.WinDef$HRGN
 *  com.sun.jna.platform.win32.WinDef$HWND
 *  com.sun.jna.platform.win32.WinDef$RECT
 *  com.sun.jna.win32.StdCallLibrary
 */
package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface User32Ex
extends StdCallLibrary {
    public static final User32Ex INSTANCE = (User32Ex)Native.loadLibrary((String)"User32", User32Ex.class);
    public static final int SPI_GETWORKAREA = 48;
    public static final int ERROR = 0;

    public int SystemParametersInfoW(int var1, int var2, WinDef.RECT var3, int var4);

    public int IsWindow(WinDef.HWND var1);

    public int IsIconic(WinDef.HWND var1);

    public int GetWindowRgn(WinDef.HWND var1, WinDef.HRGN var2);

    public int BringWindowToTop(WinDef.HWND var1);
}

