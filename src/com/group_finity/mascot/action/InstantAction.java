/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class InstantAction
extends ActionBase {
    private static final Logger log = Logger.getLogger(InstantAction.class.getName());

    public InstantAction(VariableMap params) {
        super(new ArrayList<Animation>(), params);
    }

    @Override
    public final void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        if (super.hasNext()) {
            this.apply();
        }
    }

    protected abstract void apply() throws VariableException;

    @Override
    public final boolean hasNext() throws VariableException {
        if (super.hasNext()) {
            // empty if block
        }
        return false;
    }

    @Override
    protected final void tick() {
    }
}

