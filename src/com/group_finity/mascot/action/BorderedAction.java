/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Border;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.List;
import java.util.logging.Logger;

public abstract class BorderedAction
extends ActionBase {
    private static final Logger log = Logger.getLogger(BorderedAction.class.getName());
    private static final String PARAMETER_BORDERTYPE = "\u67a0";
    public static final String DEFAULT_BORDERTYPE = null;
    public static final String BORDERTYPE_CEILING = "\u5929\u4e95";
    public static final String BORDERTYPE_WALL = "\u58c1";
    public static final String BORDERTYPE_FLOOR = "\u5730\u9762";
    private Border border;

    public BorderedAction(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        String borderType = this.getBorderType();
        if (BORDERTYPE_CEILING.equals(borderType)) {
            this.setBorder(this.getEnvironment().getCeiling());
        } else if (BORDERTYPE_WALL.equals(borderType)) {
            this.setBorder(this.getEnvironment().getWall());
        } else if (BORDERTYPE_FLOOR.equals(borderType)) {
            this.setBorder(this.getEnvironment().getFloor());
        }
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (this.getBorder() != null) {
            this.getMascot().setAnchor(this.getBorder().move(this.getMascot().getAnchor()));
        }
    }

    private String getBorderType() throws VariableException {
        return this.eval(PARAMETER_BORDERTYPE, String.class, DEFAULT_BORDERTYPE);
    }

    private void setBorder(Border border) {
        this.border = border;
    }

    protected Border getBorder() {
        return this.border;
    }
}

