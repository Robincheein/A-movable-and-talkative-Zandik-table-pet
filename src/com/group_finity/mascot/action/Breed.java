/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Animate;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Breed
extends Animate {
    private static final Logger log = Logger.getLogger(Breed.class.getName());
    public static final String PARAMETER_BORNX = "\u751f\u307e\u308c\u308b\u5834\u6240X";
    private static final int DEFAULT_BORNX = 0;
    public static final String PARAMETER_BORNY = "\u751f\u307e\u308c\u308b\u5834\u6240Y";
    private static final int DEFAULT_BORNY = 0;
    public static final String PARAMETER_BORNBEHAVIOR = "\u751f\u307e\u308c\u305f\u6642\u306e\u884c\u52d5";
    private static final String DEFAULT_BORNBEHAVIOR = "";

    public Breed(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();
        if (this.getTime() == this.getAnimation().getDuration() - 1) {
            this.breed();
        }
    }

    private void breed() throws VariableException {
        Mascot mascot = new Mascot();
        log.log(Level.INFO, "\u5897\u6b96({0},{1},{2})", new Object[]{this.getMascot(), this, mascot});
        if (this.getMascot().isLookRight()) {
            mascot.setAnchor(new Point(this.getMascot().getAnchor().x - this.getBornX(), this.getMascot().getAnchor().y + this.getBornY().intValue()));
        } else {
            mascot.setAnchor(new Point(this.getMascot().getAnchor().x + this.getBornX(), this.getMascot().getAnchor().y + this.getBornY().intValue()));
        }
        mascot.setLookRight(this.getMascot().isLookRight());
        try {
            mascot.setBehavior(Main.getInstance().getConfiguration().buildBehavior(this.getBornBehavior()));
            this.getMascot().getManager().add(mascot);
        }
        catch (BehaviorInstantiationException e) {
            log.log(Level.SEVERE, "\u751f\u307e\u308c\u305f\u6642\u306e\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
            mascot.dispose();
        }
        catch (CantBeAliveException e) {
            log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
            mascot.dispose();
        }
    }

    private Number getBornY() throws VariableException {
        return this.eval(PARAMETER_BORNY, Number.class, 0);
    }

    private int getBornX() throws VariableException {
        return ((Number)this.eval(PARAMETER_BORNX, Number.class, 0)).intValue();
    }

    private String getBornBehavior() throws VariableException {
        return this.eval(PARAMETER_BORNBEHAVIOR, String.class, DEFAULT_BORNBEHAVIOR);
    }
}

