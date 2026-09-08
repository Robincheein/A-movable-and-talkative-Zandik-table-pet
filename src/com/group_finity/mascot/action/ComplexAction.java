/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class ComplexAction
extends ActionBase {
    private static final Logger log = Logger.getLogger(ComplexAction.class.getName());
    private final Action[] actions;
    private int currentAction;

    public ComplexAction(VariableMap params, Action ... actions) {
        super(new ArrayList<Animation>(), params);
        if (actions.length == 0) {
            throw new IllegalArgumentException("actions.length==0");
        }
        this.actions = actions;
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        super.init(mascot);
        if (super.hasNext()) {
            this.setCurrentAction(0);
            this.seek();
        }
    }

    protected void seek() throws VariableException {
        if (super.hasNext()) {
            while (this.getCurrentAction() < this.getActions().length && !this.getAction().hasNext()) {
                this.setCurrentAction(this.getCurrentAction() + 1);
            }
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        boolean inrange = this.getCurrentAction() < this.getActions().length;
        return super.hasNext() && inrange && this.getAction().hasNext();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (this.getAction().hasNext()) {
            this.getAction().next();
        }
    }

    protected void setCurrentAction(int currentAction) throws VariableException {
        this.currentAction = currentAction;
        if (super.hasNext() && this.getCurrentAction() < this.getActions().length) {
            this.getAction().init(this.getMascot());
        }
    }

    protected int getCurrentAction() {
        return this.currentAction;
    }

    protected Action[] getActions() {
        return this.actions;
    }

    protected Action getAction() {
        return this.getActions()[this.getCurrentAction()];
    }
}

