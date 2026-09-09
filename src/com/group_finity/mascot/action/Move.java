/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.BorderedAction;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Move
extends BorderedAction {
    private static final Logger log = Logger.getLogger(Move.class.getName());
    private static final String PARAMETER_TARGETX = "\u76ee\u7684\u5730X";
    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;
    private static final String PARAMETER_TARGETY = "\u76ee\u7684\u5730Y";
    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;
    private boolean edgeReached;

    @Override public void init(com.group_finity.mascot.Mascot mascot) throws VariableException {
        super.init(mascot);
        edgeReached = false;
    }

    public Move(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public boolean hasNext() throws VariableException {
        int targetX = this.getTargetX();
        int targetY = this.getTargetY();
        boolean noMoveX = false;
        boolean noMoveY = false;
        if (targetX != Integer.MIN_VALUE && this.getMascot().getAnchor().x == targetX) {
            noMoveX = true;
        }
        if (targetY != Integer.MIN_VALUE && this.getMascot().getAnchor().y == targetY) {
            noMoveY = true;
        }
        return !edgeReached && super.hasNext() && !noMoveX && !noMoveY;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();
        if (this.getBorder() != null && !this.getBorder().isOn(this.getMascot().getAnchor())) {
            log.log(Level.INFO, "\u67a0\u3092\u898b\u5931\u3063\u305f({0},{1})", new Object[]{this.getMascot(), this});
            throw new LostGroundException();
        }
        int targetX = this.getTargetX();
        int targetY = this.getTargetY();
        boolean down = false;
        if (targetX != Integer.MAX_VALUE && this.getMascot().getAnchor().x != targetX) {
            this.getMascot().setLookRight(this.getMascot().getAnchor().x < targetX);
        }
        if (targetY != Integer.MAX_VALUE) {
            down = this.getMascot().getAnchor().y < targetY;
        }
        this.getAnimation().next(this.getMascot(), this.getTime());
        if (targetX != Integer.MAX_VALUE && (this.getMascot().isLookRight() && this.getMascot().getAnchor().x >= targetX || !this.getMascot().isLookRight() && this.getMascot().getAnchor().x <= targetX)) {
            this.getMascot().setAnchor(new Point(targetX, this.getMascot().getAnchor().y));
        }
        if (targetY != Integer.MAX_VALUE && (down && this.getMascot().getAnchor().y >= targetY || !down && this.getMascot().getAnchor().y <= targetY)) {
            this.getMascot().setAnchor(new Point(this.getMascot().getAnchor().x, targetY));
        }
        int requestedX = this.getMascot().getAnchor().x;
        this.getMascot().constrainToWorkArea();
        if (requestedX != this.getMascot().getAnchor().x) {
            edgeReached = true;
            this.getMascot().setLookRight(requestedX < this.getMascot().getAnchor().x);
        }
    }

    private int getTargetY() throws VariableException {
        return ((Number)this.eval(PARAMETER_TARGETY, Number.class, Integer.MAX_VALUE)).intValue();
    }

    private int getTargetX() throws VariableException {
        return ((Number)this.eval(PARAMETER_TARGETX, Number.class, Integer.MAX_VALUE)).intValue();
    }
}
