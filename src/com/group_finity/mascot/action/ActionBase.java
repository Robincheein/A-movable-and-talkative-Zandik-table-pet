/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ActionBase
implements Action {
    private static final Logger log = Logger.getLogger(ActionBase.class.getName());
    public static final String PARAMETER_DURATION = "\u9577\u3055";
    private static final boolean DEFAULT_CONDITION = true;
    public static final String PARAMETER_CONDITION = "\u6761\u4ef6";
    private static final int DEFAULT_DURATION = Integer.MAX_VALUE;
    private Mascot mascot;
    private int startTime;
    private List<Animation> animations;
    private VariableMap variables;

    public ActionBase(List<Animation> animations, VariableMap context) {
        this.animations = animations;
        this.variables = context;
    }

    public String toString() {
        try {
            return "\u52d5\u4f5c(" + this.getClass().getSimpleName() + "," + this.getName() + ")";
        }
        catch (VariableException e) {
            return "\u52d5\u4f5c(" + this.getClass().getSimpleName() + "," + null + ")";
        }
    }

    @Override
    public void init(Mascot mascot) throws VariableException {
        this.setMascot(mascot);
        this.setTime(0);
        log.log(Level.INFO, "\u52d5\u4f5c\u958b\u59cb({0},{1})", new Object[]{this.getMascot(), this});
        this.getVariables().put("mascot", (Object)mascot);
        this.getVariables().put("action", (Object)this);
        this.getVariables().init();
        for (Animation animation : this.animations) {
            animation.init();
        }
    }

    @Override
    public void next() throws LostGroundException, VariableException {
        this.initFrame();
        this.tick();
    }

    private void initFrame() {
        this.getVariables().initFrame();
        for (Animation animation : this.getAnimations()) {
            animation.initFrame();
        }
    }

    private List<Animation> getAnimations() {
        return this.animations;
    }

    protected abstract void tick() throws LostGroundException, VariableException;

    @Override
    public boolean hasNext() throws VariableException {
        boolean effective = this.isEffective();
        boolean intime = this.getTime() < this.getDuration();
        return effective && intime;
    }

    private Boolean isEffective() throws VariableException {
        return this.eval(PARAMETER_CONDITION, Boolean.class, true);
    }

    private int getDuration() throws VariableException {
        return ((Number)this.eval(PARAMETER_DURATION, Number.class, Integer.MAX_VALUE)).intValue();
    }

    private void setMascot(Mascot mascot) {
        this.mascot = mascot;
    }

    protected Mascot getMascot() {
        return this.mascot;
    }

    protected int getTime() {
        return this.getMascot().getTime() - this.startTime;
    }

    protected void setTime(int time) {
        this.startTime = this.getMascot().getTime() - time;
    }

    private String getName() throws VariableException {
        return this.eval("\u540d\u524d", String.class, null);
    }

    protected Animation getAnimation() throws VariableException {
        for (Animation animation : this.getAnimations()) {
            if (!animation.isEffective(this.getVariables())) continue;
            return animation;
        }
        log.log(Level.SEVERE, "\u6709\u52b9\u306a\u30a2\u30cb\u30e1\u30fc\u30b7\u30e7\u30f3\u304c\u898b\u3064\u3051\u3089\u308c\u307e\u305b\u3093\u3067\u3057\u305f({0},{1})", new Object[]{this.getMascot(), this});
        return null;
    }

    private VariableMap getVariables() {
        return this.variables;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void putVariable(String key, Object value) {
        VariableMap variableMap = this.getVariables();
        synchronized (variableMap) {
            this.getVariables().put(key, value);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected <T> T eval(String name, Class<T> type, T defaultValue) throws VariableException {
        VariableMap variableMap = this.getVariables();
        synchronized (variableMap) {
            Variable variable = this.getVariables().getRawMap().get(name);
            if (variable != null) {
                return type.cast(variable.get(this.getVariables()));
            }
        }
        return defaultValue;
    }

    protected MascotEnvironment getEnvironment() {
        return this.getMascot().getEnvironment();
    }
}

