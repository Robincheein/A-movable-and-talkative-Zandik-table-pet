/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.environment;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.ComplexArea;
import com.group_finity.mascot.environment.Location;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Environment {
    private static Rectangle screenRect = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize());
    private static Map<String, Rectangle> screenRects = new HashMap<String, Rectangle>();
    public ComplexArea complexScreen = new ComplexArea();
    public Area screen = new Area();
    public Location cursor = new Location();

    protected abstract Area getWorkArea();

    public abstract Area getActiveIE();

    public abstract void moveActiveIE(Point var1);

    public abstract void restoreIE();

    private static void updateScreenRect() {
        Rectangle virtualBounds = new Rectangle();
        HashMap<String, Rectangle> screenRects = new HashMap<String, Rectangle>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; ++j) {
            GraphicsDevice gd = gs[j];
            screenRects.put(gd.getIDstring(), gd.getDefaultConfiguration().getBounds());
            virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
        }
        Environment.screenRects = screenRects;
        screenRect = virtualBounds;
    }

    private static Rectangle getScreenRect() {
        return screenRect;
    }

    private static Point getCursorPos() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    protected Environment() {
        this.tick();
    }

    public void tick() {
        this.screen.set(Environment.getScreenRect());
        this.complexScreen.set(screenRects);
        this.cursor.set(Environment.getCursorPos());
    }

    public Area getScreen() {
        return this.screen;
    }

    public Collection<Area> getScreens() {
        return this.complexScreen.getAreas();
    }

    public ComplexArea getComplexScreen() {
        return this.complexScreen;
    }

    public Location getCursor() {
        return this.cursor;
    }

    public boolean isScreenTopBottom(Point location) {
        int count = 0;
        for (Area area : this.getScreens()) {
            if (area.getTopBorder().isOn(location)) {
                ++count;
            }
            if (!area.getBottomBorder().isOn(location)) continue;
            ++count;
        }
        if (count == 0) {
            if (this.getWorkArea().getTopBorder().isOn(location)) {
                return true;
            }
            if (this.getWorkArea().getBottomBorder().isOn(location)) {
                return true;
            }
        }
        return count == 1;
    }

    public boolean isScreenLeftRight(Point location) {
        int count = 0;
        for (Area area : this.getScreens()) {
            if (area.getLeftBorder().isOn(location)) {
                ++count;
            }
            if (!area.getRightBorder().isOn(location)) continue;
            ++count;
        }
        if (count == 0) {
            if (this.getWorkArea().getLeftBorder().isOn(location)) {
                return true;
            }
            if (this.getWorkArea().getRightBorder().isOn(location)) {
                return true;
            }
        }
        return count == 1;
    }

    static {
        Thread thread = new Thread(){

            @Override
            public void run() {
                try {
                    while (true) {
                        Environment.updateScreenRect();
                        Thread.sleep(5000L);
                    }
                }
                catch (InterruptedException interruptedException) {
                    return;
                }
            }
        };
        thread.setDaemon(true);
        thread.setPriority(1);
        thread.start();
    }
}

