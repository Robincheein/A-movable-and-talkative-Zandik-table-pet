/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Location;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;

public class Dragged
extends ActionBase {
    private static final Logger log = Logger.getLogger(Dragged.class.getName());
    private static final String VARIABLE_FOOTX = "footX";
    private double footX;
    private double footDx;
    private int timeToRegist;

    public Dragged(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        this.setFootX(this.getEnvironment().getCursor().getX());
        this.setTimeToRegist(250);
    }

    @Override
    public boolean hasNext() throws VariableException {
        boolean intime = this.getTime() < this.getTimeToRegist();
        boolean lukewarm = Math.random() >= 0.1;
        return super.hasNext() && (intime || lukewarm);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        this.getMascot().setLookRight(false);
        Location cursor = this.getEnvironment().getCursor();
        if (Math.abs(cursor.getX() - this.getMascot().getAnchor().x) >= 5) {
            this.setTime(0);
        }
        int newX = cursor.getX();
        this.setFootDx((this.getFootDx() + ((double)newX - this.getFootX()) * 0.1) * 0.8);
        this.setFootX(this.getFootX() + this.getFootDx());
        this.putVariable(VARIABLE_FOOTX, this.getFootX());
        this.getAnimation().next(this.getMascot(), this.getTime());
        this.getMascot().setAnchor(new Point(cursor.getX(), cursor.getY() + 120));
    }

    public void setTimeToRegist(int timeToRegist) {
        this.timeToRegist = timeToRegist;
    }

    private int getTimeToRegist() {
        return this.timeToRegist;
    }

    private void setFootX(double footX) {
        this.footX = footX;
    }

    private double getFootX() {
        return this.footX;
    }

    private void setFootDx(double footDx) {
        this.footDx = footDx;
    }

    private double getFootDx() {
        return this.footDx;
    }
}

