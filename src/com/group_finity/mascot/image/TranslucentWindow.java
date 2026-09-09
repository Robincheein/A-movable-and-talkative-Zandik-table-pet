/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.image;

import com.group_finity.mascot.image.NativeImage;
import javax.swing.JWindow;

public interface TranslucentWindow {
    public JWindow asJWindow();

    public void setImage(NativeImage var1);

    public void updateImage();

    public void setWindowOpacity(float opacity);

    /** Publish one complete frame, rather than moving yesterday's bitmap first. */
    default void present(java.awt.Rectangle bounds, NativeImage image, float opacity) {
        java.awt.Rectangle target = new java.awt.Rectangle(bounds);
        javax.swing.SwingUtilities.invokeLater(() -> {
            setImage(image);
            asJWindow().setBounds(target);
            if (!asJWindow().isVisible()) asJWindow().setVisible(true);
            setWindowOpacity(opacity);
            updateImage();
        });
    }
}
