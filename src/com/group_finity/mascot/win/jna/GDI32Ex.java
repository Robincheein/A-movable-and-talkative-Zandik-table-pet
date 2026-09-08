/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Native
 *  com.sun.jna.Structure
 *  com.sun.jna.platform.win32.WinDef$HBITMAP
 *  com.sun.jna.platform.win32.WinDef$HRGN
 *  com.sun.jna.platform.win32.WinDef$RECT
 *  com.sun.jna.win32.StdCallLibrary
 */
package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface GDI32Ex
extends StdCallLibrary {
    public static final GDI32Ex INSTANCE = (GDI32Ex)Native.loadLibrary((String)"Gdi32", GDI32Ex.class);

    public int GetObjectW(WinDef.HBITMAP var1, int var2, Structure var3);

    public int GetRgnBox(WinDef.HRGN var1, WinDef.RECT var2);
}

