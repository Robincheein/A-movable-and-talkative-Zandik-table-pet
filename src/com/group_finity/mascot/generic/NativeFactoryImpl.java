/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.generic;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.generic.GenericEnvironment;
import com.group_finity.mascot.generic.GenericNativeImage;
import com.group_finity.mascot.generic.GenericTranslucentWindow;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import java.awt.image.BufferedImage;

public class NativeFactoryImpl
extends NativeFactory {
    private Environment environment = new GenericEnvironment();

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public NativeImage newNativeImage(BufferedImage src) {
        return new GenericNativeImage(src);
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new GenericTranslucentWindow();
    }
}

