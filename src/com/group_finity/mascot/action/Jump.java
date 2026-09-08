/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;

public class Jump
extends ActionBase {
    private static final Logger log = Logger.getLogger(Jump.class.getName());
    public static final String PARAMETER_TARGETX = "\u76ee\u7684\u5730X";
    private static final int DEFAULT_PARAMETERX = 0;
    public static final String PARAMETER_TARGETY = "\u76ee\u7684\u5730Y";
    private static final int DEFAULT_PARAMETERY = 0;
    public static final String PARAMETER_VELOCITY = "\u901f\u5ea6";
    private static final double DEFAULT_VELOCITY = 20.0;

    public Jump(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public boolean hasNext() throws VariableException {
        int targetX = this.getTargetX();
        int targetY = this.getTargetY();
        double distanceX = targetX - this.getMascot().getAnchor().x;
        double distanceY = (double)(targetY - this.getMascot().getAnchor().y) - Math.abs(distanceX) / 2.0;
        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
        return super.hasNext() && distance != 0.0;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        int targetX = this.getTargetX();
        int targetY = this.getTargetY();
        this.getMascot().setLookRight(this.getMascot().getAnchor().x < targetX);
        double distanceX = targetX - this.getMascot().getAnchor().x;
        double distanceY = (double)(targetY - this.getMascot().getAnchor().y) - Math.abs(distanceX) / 2.0;
        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
        double velocity = this.getVelocity();
        if (distance != 0.0) {
            int velocityX = (int)(velocity * distanceX / distance);
            int velocityY = (int)(velocity * distanceY / distance);
            this.getMascot().setAnchor(new Point(this.getMascot().getAnchor().x + velocityX, this.getMascot().getAnchor().y + velocityY));
            this.getAnimation().next(this.getMascot(), this.getTime());
        }
        if (distance <= velocity) {
            this.getMascot().setAnchor(new Point(targetX, targetY));
        }
    }

    private double getVelocity() throws VariableException {
        return ((Number)this.eval(PARAMETER_VELOCITY, Number.class, 20.0)).doubleValue();
    }

    private int getTargetY() throws VariableException {
        return ((Number)this.eval(PARAMETER_TARGETY, Number.class, 0)).intValue();
    }

    private int getTargetX() throws VariableException {
        return ((Number)this.eval(PARAMETER_TARGETX, Number.class, 0)).intValue();
    }
}

