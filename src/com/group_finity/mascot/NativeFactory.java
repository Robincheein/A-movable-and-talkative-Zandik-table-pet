/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Platform
 */
package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;
import java.awt.image.BufferedImage;

public abstract class NativeFactory {
    private static final NativeFactory instance;

    public static NativeFactory getInstance() {
        return instance;
    }

    public abstract Environment getEnvironment();

    public abstract NativeImage newNativeImage(BufferedImage var1);

    public abstract TranslucentWindow newTransparentWindow();

    static {
        String basePackage = NativeFactory.class.getName().substring(0, NativeFactory.class.getName().lastIndexOf(46));
        String subPackage = Platform.isWindows() ? "win" : "generic";
        try {
            Class<?> impl = Class.forName(basePackage + "." + subPackage + ".NativeFactoryImpl");
            instance = (NativeFactory)impl.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

