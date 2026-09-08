/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public class UserBehavior
implements Behavior {
    private static final Logger log = Logger.getLogger(UserBehavior.class.getName());
    public static final String BEHAVIORNAME_FALL = "\u843d\u4e0b\u3059\u308b";
    public static final String BEHAVIORNAME_THROWN = "\u6295\u3052\u3089\u308c\u308b";
    public static final String BEHAVIORNAME_DRAGGED = "\u30c9\u30e9\u30c3\u30b0\u3055\u308c\u308b";
    private final String name;
    private final Configuration configuration;
    private final Action action;
    private Mascot mascot;

    public UserBehavior(String name, Action action, Configuration configuration) {
        this.name = name;
        this.configuration = configuration;
        this.action = action;
    }

    public String toString() {
        return "\u884c\u52d5(" + this.getName() + ")";
    }

    @Override
    public synchronized void init(Mascot mascot) throws CantBeAliveException {
        block4: {
            this.setMascot(mascot);
            log.log(Level.INFO, "\u884c\u52d5\u958b\u59cb({0},{1})", new Object[]{this.getMascot(), this});
            try {
                this.getAction().init(mascot);
                if (this.getAction().hasNext()) break block4;
                try {
                    mascot.setBehavior(this.getConfiguration().buildBehavior(this.getName(), mascot));
                }
                catch (BehaviorInstantiationException e) {
                    throw new CantBeAliveException("\u6b21\u306e\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
                }
            }
            catch (VariableException e) {
                throw new CantBeAliveException("\u5909\u6570\u306e\u8a55\u4fa1\u3067\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f", e);
            }
        }
    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    private Action getAction() {
        return this.action;
    }

    private String getName() {
        return this.name;
    }

    @Override
    public synchronized void mousePressed(MouseEvent event) throws CantBeAliveException {
        if (SwingUtilities.isLeftMouseButton(event)) {
            try {
                this.getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_DRAGGED));
            }
            catch (BehaviorInstantiationException e) {
                throw new CantBeAliveException("\u30c9\u30e9\u30c3\u30b0\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
            }
        }
    }

    @Override
    public synchronized void mouseReleased(MouseEvent event) throws CantBeAliveException {
        if (SwingUtilities.isLeftMouseButton(event)) {
            try {
                this.getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_THROWN));
            }
            catch (BehaviorInstantiationException e) {
                throw new CantBeAliveException("\u30c9\u30ed\u30c3\u30d7\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
            }
        }
    }

    @Override
    public synchronized void next() throws CantBeAliveException {
        block11: {
            try {
                if (this.getAction().hasNext()) {
                    this.getAction().next();
                }
                if (this.getAction().hasNext()) {
                    if (!(this.getMascot().getBounds().getX() + this.getMascot().getBounds().getWidth() <= (double)this.getEnvironment().getScreen().getLeft()) && !((double)this.getEnvironment().getScreen().getRight() <= this.getMascot().getBounds().getX()) && !((double)this.getEnvironment().getScreen().getBottom() <= this.getMascot().getBounds().getY())) break block11;
                    log.log(Level.INFO, "\u753b\u9762\u306e\u5916\u306b\u51fa\u305f({0},{1})", new Object[]{this.getMascot(), this});
                    this.getMascot().setAnchor(new Point((int)(Math.random() * (double)(this.getEnvironment().getScreen().getRight() - this.getEnvironment().getScreen().getLeft())) + this.getEnvironment().getScreen().getLeft(), this.getEnvironment().getScreen().getTop() - 256));
                    try {
                        this.getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_FALL));
                        break block11;
                    }
                    catch (BehaviorInstantiationException e) {
                        throw new CantBeAliveException("\u843d\u3061\u308b\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
                    }
                }
                log.log(Level.INFO, "\u884c\u52d5\u5b8c\u4e86({0},{1})", new Object[]{this.getMascot(), this});
                try {
                    this.getMascot().setBehavior(this.getConfiguration().buildBehavior(this.getName(), this.getMascot()));
                }
                catch (BehaviorInstantiationException e) {
                    throw new CantBeAliveException("\u6b21\u306e\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
                }
            }
            catch (LostGroundException e) {
                log.log(Level.INFO, "\u5730\u9762\u304b\u3089\u96e2\u308c\u305f({0},{1})", new Object[]{this.getMascot(), this});
                try {
                    this.getMascot().setBehavior(this.getConfiguration().buildBehavior(BEHAVIORNAME_FALL));
                }
                catch (BehaviorInstantiationException ex) {
                    throw new CantBeAliveException("\u843d\u3061\u308b\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", ex);
                }
            }
            catch (VariableException e) {
                throw new CantBeAliveException("\u5909\u6570\u306e\u8a55\u4fa1\u3067\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f", e);
            }
        }
    }

    private void setMascot(Mascot mascot) {
        this.mascot = mascot;
    }

    private Mascot getMascot() {
        return this.mascot;
    }

    protected MascotEnvironment getEnvironment() {
        return this.getMascot().getEnvironment();
    }
}

