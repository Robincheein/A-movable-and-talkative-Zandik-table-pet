/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Regist
extends ActionBase {
    private static final Logger log = Logger.getLogger(Regist.class.getName());

    public Regist(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    public boolean hasNext() throws VariableException {
        boolean notMoved = Math.abs(this.getEnvironment().getCursor().getX() - this.getMascot().getAnchor().x) < 5;
        return super.hasNext() && notMoved;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        Animation animation = this.getAnimation();
        animation.next(this.getMascot(), this.getTime());
        if (this.getTime() + 1 >= this.getAnimation().getDuration()) {
            this.getMascot().setLookRight(Math.random() < 0.5);
            log.log(Level.INFO, "\u30de\u30a6\u30b9\u304b\u3089\u9003\u3052\u305f({0},{1})", new Object[]{this.getMascot(), this});
            throw new LostGroundException();
        }
    }
}

