/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.image;

import com.group_finity.mascot.NativeFactory;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** 一帧桌宠图像，并支持运行时缩放。 */
public class MascotImage {
    private final NativeImage image;
    private final BufferedImage sourceImage;
    private final Point center;
    private final Dimension size;
    private int cachedScaleKey = -1;
    private NativeImage cachedScaledImage;

    public MascotImage(NativeImage image, Point center, Dimension size) {
        this.image = image;
        this.sourceImage = null;
        this.center = center;
        this.size = size;
    }

    public MascotImage(BufferedImage image, Point center) {
        this.image = NativeFactory.getInstance().newNativeImage(image);
        this.sourceImage = image;
        this.center = center;
        this.size = new Dimension(image.getWidth(), image.getHeight());
    }

    public NativeImage getImage() { return image; }

    public synchronized NativeImage getImage(double scale) {
        int key = Math.max(1, (int)Math.round(scale * 1000.0));
        if (key == 1000 || sourceImage == null) return image;
        if (cachedScaledImage == null || cachedScaleKey != key) {
            int width = Math.max(1, (int)Math.round(sourceImage.getWidth() * scale));
            int height = Math.max(1, (int)Math.round(sourceImage.getHeight() * scale));
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(sourceImage, 0, 0, width, height, null);
            g.dispose();
            cachedScaledImage = NativeFactory.getInstance().newNativeImage(scaled);
            cachedScaleKey = key;
        }
        return cachedScaledImage;
    }

    public Point getCenter() { return center; }
    public Point getCenter(double scale) {
        return new Point((int)Math.round(center.x * scale), (int)Math.round(center.y * scale));
    }
    public Dimension getSize() { return size; }
    public Dimension getSize(double scale) {
        return new Dimension(Math.max(1,(int)Math.round(size.width*scale)),
                Math.max(1,(int)Math.round(size.height*scale)));
    }
}