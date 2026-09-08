/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;

public class Fall
extends ActionBase {
    private static final Logger log = Logger.getLogger(Fall.class.getName());
    public static final String PARAMETER_INITIALVX = "\u521d\u901fX";
    private static final int DEFAULT_INITIALVX = 0;
    private static final String PARAMETER_INITIALVY = "\u521d\u901fY";
    private static final int DEFAULT_INITIALVY = 0;
    public static final String PARAMETER_REGISTANCEX = "\u7a7a\u6c17\u62b5\u6297X";
    private static final double DEFAULT_REGISTANCEX = 0.05;
    public static final String PARAMETER_REGISTANCEY = "\u7a7a\u6c17\u62b5\u6297Y";
    private static final double DEFAULT_REGISTANCEY = 0.1;
    public static final String PARAMETER_GRAVITY = "\u91cd\u529b";
    private static final double DEFAULT_GRAVITY = 2.0;
    private double velocityX;
    private double velocityY;
    private double modX;
    private double modY;

    public Fall(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        this.setVelocityX(this.getInitialVx());
        this.setVelocityY(this.getInitialVy());
    }

    @Override
    public boolean hasNext() throws VariableException {
        Point pos = this.getMascot().getAnchor();
        boolean onBorder = false;
        if (this.getEnvironment().getFloor().isOn(pos)) {
            onBorder = true;
        }
        // 动物版暂不爬墙：碰到墙不能结束下落，否则会在边缘卡住。
        // 恢复爬墙时再恢复墙面落点检测及 Behavior.xml 的对应入口。
        // if (this.getEnvironment().getWall().isOn(pos)) { onBorder = true; }
        return super.hasNext() && !onBorder;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (this.getVelocityX() != 0.0) {
            this.getMascot().setLookRight(this.getVelocityX() > 0.0);
        }
        this.setVelocityX(this.getVelocityX() - this.getVelocityX() * this.getRegistanceX());
        this.setVelocityY(this.getVelocityY() - this.getVelocityY() * this.getRegistanceY() + this.getGravity());
        this.setModX(this.getModX() + this.getVelocityX() % 1.0);
        this.setModY(this.getModY() + this.getVelocityY() % 1.0);
        int dx = (int)this.getVelocityX() + (int)this.getModX();
        int dy = (int)this.getVelocityY() + (int)this.getModY();
        this.setModX(this.getModX() % 1.0);
        this.setModY(this.getModY() % 1.0);
        int dev = Math.max(Math.abs(dx), Math.abs(dy));
        if (dev < 1) {
            dev = 1;
        }
        Point start = this.getMascot().getAnchor();
        block0: for (int i = 0; i <= dev; ++i) {
            int x = start.x + dx * i / dev;
            x = Math.max(this.getEnvironment().getWorkArea().getLeft(),
                    Math.min(this.getEnvironment().getWorkArea().getRight() - 1, x));
            int y = start.y + dy * i / dev;
            this.getMascot().setAnchor(new Point(x, y));
            if (dy > 0) {
                for (int j = -80; j <= 0; ++j) {
                    this.getMascot().setAnchor(new Point(x, y + j));
                    if (this.getEnvironment().getFloor(true).isOn(this.getMascot().getAnchor())) break block0;
                }
            }
            // 动物版沿墙继续下落，不吸附到墙面。
            // if (this.getEnvironment().getWall(true).isOn(this.getMascot().getAnchor())) break;
        }
        this.getAnimation().next(this.getMascot(), this.getTime());
    }

    private int getInitialVy() throws VariableException {
        return ((Number)this.eval(PARAMETER_INITIALVY, Number.class, 0)).intValue();
    }

    private int getInitialVx() throws VariableException {
        return ((Number)this.eval(PARAMETER_INITIALVX, Number.class, 0)).intValue();
    }

    private double getGravity() throws VariableException {
        return ((Number)this.eval(PARAMETER_GRAVITY, Number.class, 2.0)).doubleValue();
    }

    private double getRegistanceY() throws VariableException {
        return ((Number)this.eval(PARAMETER_REGISTANCEY, Number.class, 0.1)).doubleValue();
    }

    private double getRegistanceX() throws VariableException {
        return ((Number)this.eval(PARAMETER_REGISTANCEX, Number.class, 0.05)).doubleValue();
    }

    private void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    private double getVelocityY() {
        return this.velocityY;
    }

    private void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    private double getVelocityX() {
        return this.velocityX;
    }

    private void setModX(double modX) {
        this.modX = modX;
    }

    private double getModX() {
        return this.modX;
    }

    private void setModY(double modY) {
        this.modY = modY;
    }

    private double getModY() {
        return this.modY;
    }
}
