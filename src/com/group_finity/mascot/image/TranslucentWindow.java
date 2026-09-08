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
}

