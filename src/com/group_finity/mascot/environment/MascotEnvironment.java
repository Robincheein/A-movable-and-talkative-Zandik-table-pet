/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.environment;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Border;
import com.group_finity.mascot.environment.ComplexArea;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.environment.Location;
import com.group_finity.mascot.environment.NotOnBorder;
import java.awt.Point;

public class MascotEnvironment {
    private Environment impl = NativeFactory.getInstance().getEnvironment();
    private Mascot mascot;
    private Area currentWorkArea;

    public MascotEnvironment(Mascot mascot) {
        this.mascot = mascot;
    }

    public Area getWorkArea() {
        if (this.currentWorkArea != null) {
            if (this.currentWorkArea != this.impl.getWorkArea() && this.currentWorkArea.toRectangle().contains(this.impl.getWorkArea().toRectangle()) && this.impl.getWorkArea().contains(this.mascot.getAnchor().x, this.mascot.getAnchor().y)) {
                this.currentWorkArea = this.impl.getWorkArea();
                return this.currentWorkArea;
            }
            if (this.currentWorkArea.contains(this.mascot.getAnchor().x, this.mascot.getAnchor().y)) {
                return this.currentWorkArea;
            }
        }
        if (this.impl.getWorkArea().contains(this.mascot.getAnchor().x, this.mascot.getAnchor().y)) {
            this.currentWorkArea = this.impl.getWorkArea();
            return this.currentWorkArea;
        }
        for (Area area : this.impl.getScreens()) {
            if (!area.contains(this.mascot.getAnchor().x, this.mascot.getAnchor().y)) continue;
            this.currentWorkArea = area;
            return this.currentWorkArea;
        }
        this.currentWorkArea = this.impl.getWorkArea();
        return this.currentWorkArea;
    }

    public Area getActiveIE() {
        return this.impl.getActiveIE();
    }

    public Border getCeiling() {
        return this.getCeiling(false);
    }

    public Border getCeiling(boolean ignoreSeparator) {
        if (this.getActiveIE().getBottomBorder().isOn(this.mascot.getAnchor())) {
            return this.getActiveIE().getBottomBorder();
        }
        if (this.getWorkArea().getTopBorder().isOn(this.mascot.getAnchor()) && (!ignoreSeparator || this.isScreenTopBottom())) {
            return this.getWorkArea().getTopBorder();
        }
        return NotOnBorder.INSTANCE;
    }

    public ComplexArea getComplexScreen() {
        return this.impl.getComplexScreen();
    }

    public Location getCursor() {
        return this.impl.getCursor();
    }

    public Border getFloor() {
        return this.getFloor(false);
    }

    public Border getFloor(boolean ignoreSeparator) {
        if (this.getActiveIE().getTopBorder().isOn(this.mascot.getAnchor())) {
            return this.getActiveIE().getTopBorder();
        }
        if (this.getWorkArea().getBottomBorder().isOn(this.mascot.getAnchor()) && (!ignoreSeparator || this.isScreenTopBottom())) {
            return this.getWorkArea().getBottomBorder();
        }
        return NotOnBorder.INSTANCE;
    }

    public Area getScreen() {
        return this.impl.getScreen();
    }

    public Border getWall() {
        return this.getWall(false);
    }

    public Border getWall(boolean ignoreSeparator) {
        if (this.mascot.isLookRight()) {
            if (this.getActiveIE().getLeftBorder().isOn(this.mascot.getAnchor())) {
                return this.getActiveIE().getLeftBorder();
            }
            if (this.getWorkArea().getRightBorder().isOn(this.mascot.getAnchor()) && (!ignoreSeparator || this.isScreenLeftRight())) {
                return this.getWorkArea().getRightBorder();
            }
        } else {
            if (this.getActiveIE().getRightBorder().isOn(this.mascot.getAnchor())) {
                return this.getActiveIE().getRightBorder();
            }
            if (this.getWorkArea().getLeftBorder().isOn(this.mascot.getAnchor()) && (!ignoreSeparator || this.isScreenLeftRight())) {
                return this.getWorkArea().getLeftBorder();
            }
        }
        return NotOnBorder.INSTANCE;
    }

    public void moveActiveIE(Point point) {
        this.impl.moveActiveIE(point);
    }

    public void restoreIE() {
        this.impl.restoreIE();
    }

    private boolean isScreenTopBottom() {
        return this.impl.isScreenTopBottom(this.mascot.getAnchor());
    }

    private boolean isScreenLeftRight() {
        return this.impl.isScreenLeftRight(this.mascot.getAnchor());
    }
}

