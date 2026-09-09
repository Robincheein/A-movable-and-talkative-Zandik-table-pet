/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.SettingsManager;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Location;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;

public class Dragged
extends ActionBase {
    private static final String VARIABLE_FOOTX = "footX";
    private double footX;
    private double footDx;
    private Point gripOffset;
    private double tilt;

    public Dragged(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        this.setFootX(this.getEnvironment().getCursor().getX());
        this.footDx = 0;
        Location cursor = getEnvironment().getCursor();
        gripOffset = new Point(mascot.getAnchor().x - cursor.getX(), mascot.getAnchor().y - cursor.getY());
        tilt = 0;
        mascot.setAnimatingPublic(true);
    }

    @Override
    public boolean hasNext() throws VariableException {
        // Only mouse release ends dragging; no random escape at the edge or after a timeout.
        return super.hasNext();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        Location cursor = this.getEnvironment().getCursor();
        int newX = cursor.getX();
        this.setFootDx((this.getFootDx() + ((double)newX - this.getFootX()) * 0.1) * 0.8);
        this.setFootX(this.getFootX() + this.getFootDx());
        this.putVariable(VARIABLE_FOOTX, this.getFootX());
        this.getAnimation().next(this.getMascot(), this.getTime());
        double pickup = Math.min(1.0, getTime() / 6.0);
        pickup = pickup * pickup * (3 - 2 * pickup);
        double targetTilt = Math.max(-4, Math.min(4, -cursor.getDx() * 0.15))
                + Math.sin(getTime() * Math.PI / 24.0) * 2;
        tilt += (targetTilt - tilt) * 0.25;
        getMascot().setImage(getMascot().getImage().motionFrame((int)Math.round(tilt * pickup), 100));
        double scale = SettingsManager.getInstance().getCharacterScale();
        getMascot().setAnchor(new Point(cursor.getX() + gripOffset.x,
                cursor.getY() + gripOffset.y - (int)Math.round(4 * scale * pickup)));
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
