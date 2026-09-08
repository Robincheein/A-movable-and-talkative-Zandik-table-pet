/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.platform.win32.GDI32
 *  com.sun.jna.platform.win32.WinDef$HBITMAP
 *  com.sun.jna.platform.win32.WinGDI$BITMAPINFO
 *  com.sun.jna.platform.win32.WinNT$HANDLE
 */
package com.group_finity.mascot.win;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.win.jna.BITMAP;
import com.group_finity.mascot.win.jna.GDI32Ex;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

class WindowsNativeImage
implements NativeImage {
    private final BufferedImage managedImage;
    private final WinDef.HBITMAP nativeHandle;
    private final int[] rgb;

    private static WinDef.HBITMAP createNative(int width, int height) {
        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biSize = 40;
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = (short)32;
        WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateDIBSection(null, bmi, 0, null, null, 0);
        return hBitmap;
    }

    private static void flushNative(WinDef.HBITMAP nativeHandle, int[] rgb) {
        BITMAP bmp = new BITMAP();
        GDI32Ex.INSTANCE.GetObjectW(nativeHandle, bmp.size(), bmp);
        int width = bmp.bmWidth;
        int height = bmp.bmHeight;
        int destPitch = (bmp.bmWidth * bmp.bmBitsPixel + 31) / 32 * 4;
        int destIndex = destPitch * (height - 1);
        int srcIndex = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                bmp.bmBits.setInt((long)(destIndex + x * 4), (rgb[srcIndex] & 0xFF000000) == 0 ? 0 : rgb[srcIndex]);
                ++srcIndex;
            }
            destIndex -= destPitch;
        }
    }

    private static void freeNative(WinDef.HBITMAP nativeHandle) {
        GDI32.INSTANCE.DeleteObject((WinNT.HANDLE)nativeHandle);
    }

    public WindowsNativeImage(BufferedImage image) {
        this.managedImage = image;
        this.nativeHandle = WindowsNativeImage.createNative(this.getManagedImage().getWidth(), this.getManagedImage().getHeight());
        this.rgb = new int[this.getManagedImage().getWidth() * this.getManagedImage().getHeight()];
        this.update();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        WindowsNativeImage.freeNative(this.getNativeHandle());
    }

    public void update() {
        this.getManagedImage().getRGB(0, 0, this.getManagedImage().getWidth(), this.getManagedImage().getHeight(), this.getRgb(), 0, this.getManagedImage().getWidth());
        WindowsNativeImage.flushNative(this.getNativeHandle(), this.getRgb());
    }

    public void flush() {
        this.getManagedImage().flush();
    }

    public WinDef.HBITMAP getHandle() {
        return this.getNativeHandle();
    }

    public Graphics getGraphics() {
        return this.getManagedImage().createGraphics();
    }

    public int getHeight() {
        return this.getManagedImage().getHeight();
    }

    public int getWidth() {
        return this.getManagedImage().getWidth();
    }

    public int getHeight(ImageObserver observer) {
        return this.getManagedImage().getHeight(observer);
    }

    public Object getProperty(String name, ImageObserver observer) {
        return this.getManagedImage().getProperty(name, observer);
    }

    public ImageProducer getSource() {
        return this.getManagedImage().getSource();
    }

    public int getWidth(ImageObserver observer) {
        return this.getManagedImage().getWidth(observer);
    }

    private BufferedImage getManagedImage() {
        return this.managedImage;
    }

    private WinDef.HBITMAP getNativeHandle() {
        return this.nativeHandle;
    }

    private int[] getRgb() {
        return this.rgb;
    }
}

