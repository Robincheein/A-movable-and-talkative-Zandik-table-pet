/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.environment;

import com.group_finity.mascot.environment.Border;
import java.awt.Point;

public class NotOnBorder
implements Border {
    public static final NotOnBorder INSTANCE = new NotOnBorder();

    private NotOnBorder() {
    }

    @Override
    public boolean isOn(Point location) {
        return false;
    }

    @Override
    public Point move(Point location) {
        return location;
    }
}

