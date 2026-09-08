/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ComplexAction;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.logging.Logger;

public class Sequence
extends ComplexAction {
    private static final Logger log = Logger.getLogger(Sequence.class.getName());
    public static final String PARAMETER_LOOP = "\u7e70\u308a\u8fd4\u3057";
    private static final boolean DEFAULT_LOOP = false;

    public Sequence(VariableMap params, Action ... actions) {
        super(params, actions);
    }

    @Override
    public boolean hasNext() throws VariableException {
        this.seek();
        return super.hasNext();
    }

    @Override
    protected void setCurrentAction(int currentAction) throws VariableException {
        super.setCurrentAction(this.isLoop() != false ? currentAction % this.getActions().length : currentAction);
    }

    private Boolean isLoop() throws VariableException {
        return this.eval(PARAMETER_LOOP, Boolean.class, false);
    }
}

