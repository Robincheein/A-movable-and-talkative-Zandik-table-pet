/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.InstantAction;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.logging.Logger;

public class Offset
extends InstantAction {
    private static final Logger log = Logger.getLogger(Offset.class.getName());
    public static final String PARAMETER_OFFSETX = "X";
    private static final int DEFAULT_OFFSETX = 0;
    public static final String PARAMETER_OFFSETY = "Y";
    private static final int DEFAULT_OFFSETY = 0;

    public Offset(VariableMap params) {
        super(params);
    }

    @Override
    protected void apply() throws VariableException {
        this.getMascot().setAnchor(new Point(this.getMascot().getAnchor().x + this.getOffsetX(), this.getMascot().getAnchor().y + this.getOffsetY()));
    }

    private int getOffsetY() throws VariableException {
        return ((Number)this.eval(PARAMETER_OFFSETY, Number.class, 0)).intValue();
    }

    private int getOffsetX() throws VariableException {
        return ((Number)this.eval(PARAMETER_OFFSETX, Number.class, 0)).intValue();
    }
}

