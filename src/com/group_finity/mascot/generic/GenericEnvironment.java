/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.generic;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import java.awt.Point;

class GenericEnvironment
extends Environment {
    private static Area activeIE = new Area();

    GenericEnvironment() {
    }

    @Override
    public void tick() {
        super.tick();
        activeIE.setVisible(false);
    }

    @Override
    public void moveActiveIE(Point point) {
    }

    @Override
    public void restoreIE() {
    }

    @Override
    public Area getWorkArea() {
        return this.getScreen();
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }
}

