/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.Animate;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;

public class ThrowIE
extends Animate {
    private static final Logger log = Logger.getLogger(ThrowIE.class.getName());
    public static final String PARAMETER_INITIALVX = "\u521d\u901fX";
    private static final int DEFAULT_INITIALVX = 32;
    public static final String PARAMETER_INITIALVY = "\u521d\u901fY";
    private static final int DEFAULT_INITIALVY = -10;
    public static final String PARAMETER_GRAVITY = "\u91cd\u529b";
    private static final double DEFAULT_GRAVITY = 0.5;

    public ThrowIE(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public boolean hasNext() throws VariableException {
        boolean ieVisible = this.getEnvironment().getActiveIE().isVisible();
        return super.hasNext() && ieVisible;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();
        Area activeIE = this.getEnvironment().getActiveIE();
        if (activeIE.isVisible()) {
            if (this.getMascot().isLookRight()) {
                this.getEnvironment().moveActiveIE(new Point(activeIE.getLeft() + this.getInitialVx(), activeIE.getTop() + this.getInitialVy() + (int)((double)this.getTime() * this.getGravity())));
            } else {
                this.getEnvironment().moveActiveIE(new Point(activeIE.getLeft() - this.getInitialVx(), activeIE.getTop() + this.getInitialVy() + (int)((double)this.getTime() * this.getGravity())));
            }
        }
    }

    private double getGravity() throws VariableException {
        return ((Number)this.eval(PARAMETER_GRAVITY, Number.class, 0.5)).doubleValue();
    }

    private int getInitialVy() throws VariableException {
        return ((Number)this.eval(PARAMETER_INITIALVY, Number.class, -10)).intValue();
    }

    private int getInitialVx() throws VariableException {
        return ((Number)this.eval(PARAMETER_INITIALVX, Number.class, 32)).intValue();
    }
}

