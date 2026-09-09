/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Native
 *  com.sun.jna.platform.win32.GDI32
 *  com.sun.jna.platform.win32.User32
 *  com.sun.jna.platform.win32.WinDef$HBITMAP
 *  com.sun.jna.platform.win32.WinDef$HDC
 *  com.sun.jna.platform.win32.WinDef$HWND
 *  com.sun.jna.platform.win32.WinDef$RECT
 *  com.sun.jna.platform.win32.WinNT$HANDLE
 *  com.sun.jna.platform.win32.WinUser$BLENDFUNCTION
 *  com.sun.jna.platform.win32.WinUser$POINT
 *  com.sun.jna.platform.win32.WinUser$SIZE
 */
package com.group_finity.mascot.win;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.win.WindowsNativeImage;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class WindowsTranslucentWindow
extends JWindow
implements TranslucentWindow {
    private static final long serialVersionUID = 1L;
    private WindowsNativeImage image;
    private int alpha = 255;
    private final AtomicReference<Frame> pendingFrame = new AtomicReference<Frame>();
    private final AtomicBoolean frameQueued = new AtomicBoolean();
    private volatile boolean disposed;

    private static final class Frame {
        final Rectangle bounds;
        final WindowsNativeImage image;
        final float opacity;
        Frame(Rectangle bounds, NativeImage image, float opacity) {
            this.bounds = new Rectangle(bounds);
            this.image = (WindowsNativeImage)image;
            this.opacity = opacity;
        }
    }

    WindowsTranslucentWindow() {
    }

    public static void main(String[] args) throws IOException {
        WindowsTranslucentWindow win = new WindowsTranslucentWindow();
        win.setVisible(true);
        win.setBounds(0, 0, 200, 200);
        BufferedImage img = ImageIO.read(new File("img/shime1.png"));
        WindowsNativeImage wi = new WindowsNativeImage(img);
        win.setImage(wi);
        win.repaint();
    }

    @Override
    public JWindow asJWindow() {
        return this;
    }

    private void paint(WinDef.HBITMAP imageHandle, int alpha) {
        paint(imageHandle, alpha, null);
    }

    private void paint(WinDef.HBITMAP imageHandle, int alpha, Rectangle target) {
        WinDef.HWND hWnd = new WinDef.HWND(Native.getComponentPointer((Component)this));
        if (User32.INSTANCE.IsWindowVisible(hWnd)) {
            int exStyle = User32.INSTANCE.GetWindowLong(hWnd, -20);
            if ((exStyle & 0x80000) == 0) {
                User32.INSTANCE.SetWindowLong(hWnd, -20, exStyle | 0x80000);
            }
            WinDef.HDC clientDC = User32.INSTANCE.GetDC(hWnd);
            WinDef.HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(clientDC);
            WinNT.HANDLE oldBmp = GDI32.INSTANCE.SelectObject(memDC, (WinNT.HANDLE)imageHandle);
            User32.INSTANCE.ReleaseDC(hWnd, clientDC);
            WinDef.RECT windowRect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(hWnd, windowRect);
            WinUser.BLENDFUNCTION bf = new WinUser.BLENDFUNCTION();
            bf.BlendOp = 0;
            bf.BlendFlags = 0;
            bf.SourceConstantAlpha = (byte)alpha;
            bf.AlphaFormat = 1;
            WinUser.POINT lt = new WinUser.POINT();
            lt.x = target == null ? windowRect.left : target.x;
            lt.y = target == null ? windowRect.top : target.y;
            WinUser.SIZE size = new WinUser.SIZE();
            size.cx = target == null ? windowRect.right - windowRect.left : target.width;
            size.cy = target == null ? windowRect.bottom - windowRect.top : target.height;
            WinUser.POINT zero = new WinUser.POINT();
            User32.INSTANCE.UpdateLayeredWindow(hWnd, null, lt, size, memDC, zero, 0, bf, 2);
            GDI32.INSTANCE.SelectObject(memDC, oldBmp);
            GDI32.INSTANCE.DeleteDC(memDC);
        }
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + this.hashCode() + ",bounds=" + this.getBounds() + "]";
    }

    @Override
    public void paint(Graphics g) {
        if (this.getImage() != null) {
            this.paint(this.getImage().getHandle(), this.getAlpha());
        }
    }

    private WindowsNativeImage getImage() {
        return this.image;
    }

    @Override
    public void setImage(NativeImage image) {
        this.image = (WindowsNativeImage)image;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void updateImage() {
        this.repaint();
    }

    @Override public void present(Rectangle bounds, NativeImage image, float opacity) {
        if (disposed) return;
        pendingFrame.set(new Frame(bounds, image, opacity));
        queueLatestFrame();
    }

    private void queueLatestFrame() {
        if (!frameQueued.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            try {
                Frame frame = pendingFrame.getAndSet(null);
                if (disposed || frame == null) return;
                displayFrame(frame.bounds, frame.image, frame.opacity);
            } finally {
                frameQueued.set(false);
                // Coalesce busy-EDT frames instead of replaying stale mouse positions.
                if (!disposed && pendingFrame.get() != null) queueLatestFrame();
            }
        });
    }

    protected void displayFrame(Rectangle bounds, WindowsNativeImage image, float opacity) {
        this.image = image;
        this.alpha = Math.max(0, Math.min(255, Math.round(opacity * 255)));
        if (!isVisible()) {
            setBounds(bounds);
            setVisible(true);
        }
        // UpdateLayeredWindow changes the bitmap AND its destination atomically.
        // Do not call setBounds first: Windows would move the old pixels there.
        paint(image.getHandle(), alpha, bounds);
        setBounds(bounds); // keep AWT's cached bounds in sync afterwards
    }

    @Override public void dispose() {
        disposed = true;
        pendingFrame.set(null);
        super.dispose();
    }

    @Override
    public void setWindowOpacity(float opacity) {
        this.setAlpha(Math.max(0, Math.min(255, Math.round(opacity * 255.0f))));
        this.updateImage();
    }
}
