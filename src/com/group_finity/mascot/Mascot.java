/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.Manager;
import com.group_finity.mascot.MascotEventHandler;
import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Mascot {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(Mascot.class.getName());
    private static AtomicInteger lastId = new AtomicInteger();
    private final int id;
    private final TranslucentWindow window = NativeFactory.getInstance().newTransparentWindow();
    private Manager manager = null;
    private Point anchor = new Point(0, 0);
    private MascotImage image = null;
    private boolean lookRight = false;
    private Behavior behavior = null;
    private int time = 0;
    private boolean animating = true;
    private MascotEnvironment environment = new MascotEnvironment(this);

    public Mascot() {
        this.id = lastId.incrementAndGet();
        log.log(Level.INFO, "\u30de\u30b9\u30b3\u30c3\u30c8\u751f\u6210({0})", this);
        this.getWindow().asJWindow().setAlwaysOnTop(true);
        MascotEventHandler eventHandler = new MascotEventHandler(this);
        this.getWindow().asJWindow().addMouseListener(eventHandler);
        this.getWindow().asJWindow().addMouseMotionListener(eventHandler);
    }

    public String toString() {
        return "\u30de\u30b9\u30b3\u30c3\u30c8" + this.id;
    }

    void tick() {
        if (this.isAnimating() && this.getBehavior() != null) {
            try {
                this.getBehavior().next();
            }
            catch (CantBeAliveException e) {
                log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
                this.dispose();
            }
            this.setTime(this.getTime() + 1);
        }
    }

    public void apply() {
        if (this.isAnimating()) {
            if (this.getImage() != null) {
                this.getWindow().asJWindow().setBounds(this.getBounds());
                this.getWindow().setImage(this.getImage().getImage(
                        SettingsManager.getInstance().getCharacterScale()));
                if (!this.getWindow().asJWindow().isVisible()) {
                    this.getWindow().asJWindow().setVisible(true);
                    this.getWindow().setWindowOpacity(SettingsManager.getInstance().getCharacterOpacity());
                }
                this.getWindow().updateImage();
            } else if (this.getWindow().asJWindow().isVisible()) {
                this.getWindow().asJWindow().setVisible(false);
            }
        }
    }

    public void dispose() {
        log.log(Level.INFO, "\u30de\u30b9\u30b3\u30c3\u30c8\u7834\u68c4({0})", this);
        this.getWindow().asJWindow().dispose();
        if (this.getManager() != null) {
            this.getManager().remove(this);
        }
    }

    public Manager getManager() {
        return this.manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public Point getAnchor() {
        return this.anchor;
    }

    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    public MascotImage getImage() {
        return this.image;
    }

    public void setImage(MascotImage image) {
        this.image = image;
    }

    public boolean isLookRight() {
        return this.lookRight;
    }

    public void setLookRight(boolean lookRight) {
        this.lookRight = lookRight;
    }

    public Rectangle getBounds() {
        double scale = SettingsManager.getInstance().getCharacterScale();
        Point center = this.getImage().getCenter(scale);
        java.awt.Dimension size = this.getImage().getSize(scale);
        int top = this.getAnchor().y - center.y;
        int left = this.getAnchor().x - center.x;
        return new Rectangle(left, top, size.width, size.height);
    }

    public void applyVisualSettings() {
        this.getWindow().setWindowOpacity(SettingsManager.getInstance().getCharacterOpacity());
        if (this.getImage() != null) this.apply();
    }

    public int getTime() {
        return this.time;
    }

    private void setTime(int time) {
        this.time = time;
    }

    public Behavior getBehavior() {
        return this.behavior;
    }

    public void setBehavior(Behavior behavior) throws CantBeAliveException {
        this.behavior = behavior;
        this.behavior.init(this);
    }

    public int getTotalCount() {
        return this.getManager().getCount();
    }

    private boolean isAnimating() {
        return this.animating;
    }

    public void setAnimatingPublic(boolean animating) {
        this.animating = animating;
    }

    void setAnimating(boolean animating) {
        this.animating = animating;
    }

    public TranslucentWindow getWindow() {
        return this.window;
    }

    public MascotEnvironment getEnvironment() {
        return this.environment;
    }
}

