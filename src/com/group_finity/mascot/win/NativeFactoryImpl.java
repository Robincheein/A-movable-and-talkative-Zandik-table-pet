/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.win;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.win.WindowsEnvironment;
import com.group_finity.mascot.win.WindowsNativeImage;
import com.group_finity.mascot.win.WindowsTranslucentWindow;
import java.awt.image.BufferedImage;

public class NativeFactoryImpl
extends NativeFactory {
    private Environment environment = new WindowsEnvironment();

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public NativeImage newNativeImage(BufferedImage src) {
        return new WindowsNativeImage(src);
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new WindowsTranslucentWindow();
    }
}

