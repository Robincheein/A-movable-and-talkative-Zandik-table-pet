/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.image;

import com.group_finity.mascot.NativeFactory;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** 一帧桌宠图像，并支持运行时缩放。 */
public class MascotImage {
    private final NativeImage image;
    private final BufferedImage sourceImage;
    private final Point center;
    private final Dimension size;
    private int cachedScaleKey = -1;
    private NativeImage cachedScaledImage;
    private Rectangle visibleBounds;
    private final java.util.Map<Integer, MascotImage> motionFrames = new java.util.HashMap<Integer, MascotImage>();

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

    /** Cache a small set of runtime motion frames; the PNG files stay unchanged. */
    public synchronized MascotImage motionFrame(int tiltDegrees, int heightPercent) {
        tiltDegrees = Math.max(-6, Math.min(6, tiltDegrees));
        heightPercent = Math.max(92, Math.min(100, heightPercent));
        // Neutral is also padded: returning the original 128px frame here made
        // the window jump between 128px/160px whenever sway crossed zero.
        if (sourceImage == null) return this;
        int key = (tiltDegrees + 6) * 101 + heightPercent;
        MascotImage frame = motionFrames.get(key);
        if (frame == null) {
            int pad = 16;
            BufferedImage canvas = new BufferedImage(size.width + pad * 2, size.height + pad * 2,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(pad, pad);
            // Hanging motion pivots at the head; landing squash pivots at the feet.
            g.rotate(Math.toRadians(tiltDegrees), center.x, size.height * 0.30);
            g.translate(0, center.y * (1.0 - heightPercent / 100.0));
            g.scale(1, heightPercent / 100.0);
            g.drawImage(sourceImage, 0, 0, null);
            g.dispose();
            frame = new MascotImage(canvas, new Point(center.x + pad, center.y + pad));
            motionFrames.put(key, frame);
        }
        return frame;
    }

    public synchronized Rectangle getVisibleBounds(double scale) {
        if (visibleBounds == null) {
            int minX = size.width, minY = size.height, maxX = -1, maxY = -1;
            if (sourceImage != null) {
                for (int y = 0; y < size.height; y++) for (int x = 0; x < size.width; x++) {
                    if ((sourceImage.getRGB(x, y) >>> 24) < 32) continue;
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
            visibleBounds = maxX < minX ? new Rectangle(0, 0, size.width, size.height)
                    : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }
        return new Rectangle((int)Math.round(visibleBounds.x * scale), (int)Math.round(visibleBounds.y * scale),
                (int)Math.round(visibleBounds.width * scale), (int)Math.round(visibleBounds.height * scale));
    }
    public Point getCenter(double scale) {
        return new Point((int)Math.round(center.x * scale), (int)Math.round(center.y * scale));
    }
    public Dimension getSize() { return size; }
    public Dimension getSize(double scale) {
        return new Dimension(Math.max(1,(int)Math.round(size.width*scale)),
                Math.max(1,(int)Math.round(size.height*scale)));
    }
}
