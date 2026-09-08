/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.InstantAction;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.logging.Logger;

public class Look
extends InstantAction {
    private static final Logger log = Logger.getLogger(Look.class.getName());
    public static final String PARAMETER_LOOKRIGHT = "\u53f3\u5411\u304d";

    public Look(VariableMap params) {
        super(params);
    }

    @Override
    protected void apply() throws VariableException {
        this.getMascot().setLookRight(this.isLookRight());
    }

    private Boolean isLookRight() throws VariableException {
        return this.eval(PARAMETER_LOOKRIGHT, Boolean.class, !this.getMascot().isLookRight());
    }
}

