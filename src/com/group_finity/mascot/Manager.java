/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {
    private static final Logger log = Logger.getLogger(Manager.class.getName());
    public static final int TICK_INTERVAL = 40;
    private final List<Mascot> mascots = new ArrayList<Mascot>();
    private final Set<Mascot> added = new LinkedHashSet<Mascot>();
    private final Set<Mascot> removed = new LinkedHashSet<Mascot>();
    private boolean exitOnLastRemoved;
    private transient Thread thread;

    public Manager() {
        new Thread(){
            {
                this.setDaemon(true);
                this.start();
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        while (true) {
                            Thread.sleep(Integer.MAX_VALUE);
                        }
                    }
                    catch (InterruptedException interruptedException) {
                        continue;
                    }
                }
            }
        };
    }

    public void start() {
        if (this.thread != null && this.thread.isAlive()) {
            return;
        }
        this.thread = new Thread(){

            @Override
            public void run() {
                long prev = System.nanoTime() / 1000000L;
                try {
                    while (true) {
                        long cur;
                        if ((cur = System.nanoTime() / 1000000L) - prev >= 40L) {
                            prev = cur > prev + 80L ? cur : (prev += 40L);
                        } else {
                            Thread.sleep(1L, 0);
                            continue;
                        }
                        Manager.this.tick();
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };
        this.thread.setDaemon(false);
        this.thread.start();
    }

    public void stop() {
        if (this.thread == null || !this.thread.isAlive()) {
            return;
        }
        this.thread.interrupt();
        try {
            this.thread.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void tick() {
        NativeFactory.getInstance().getEnvironment().tick();
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            for (Mascot mascot : this.getAdded()) {
                this.getMascots().add(mascot);
            }
            this.getAdded().clear();
            for (Mascot mascot : this.getRemoved()) {
                this.getMascots().remove(mascot);
            }
            this.getRemoved().clear();
            for (Mascot mascot : this.getMascots()) {
                mascot.tick();
            }
            for (Mascot mascot : this.getMascots()) {
                mascot.apply();
            }
        }
        if (this.isExitOnLastRemoved() && this.getMascots().size() == 0) {
            Main.getInstance().exit();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void add(Mascot mascot) {
        Set<Mascot> set = this.getAdded();
        synchronized (set) {
            this.getAdded().add(mascot);
            this.getRemoved().remove(mascot);
        }
        mascot.setManager(this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void remove(Mascot mascot) {
        Set<Mascot> set = this.getAdded();
        synchronized (set) {
            this.getAdded().remove(mascot);
            this.getRemoved().add(mascot);
        }
        mascot.setManager(null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setBehaviorAll(Configuration configuration, String name) {
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            for (Mascot mascot : this.getMascots()) {
                try {
                    mascot.setBehavior(configuration.buildBehavior(name));
                }
                catch (BehaviorInstantiationException e) {
                    log.log(Level.SEVERE, "\u6b21\u306e\u884c\u52d5\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
                    mascot.dispose();
                }
                catch (CantBeAliveException e) {
                    log.log(Level.SEVERE, "\u751f\u304d\u7d9a\u3051\u308b\u3053\u3068\u304c\u51fa\u6765\u306a\u3044\u72b6\u6cc1", e);
                    mascot.dispose();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void remainOne() {
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            for (int i = this.getMascots().size() - 1; i > 0; --i) {
                this.getMascots().get(i).dispose();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void disposeAll() {
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            for (int i = this.getMascots().size() - 1; i >= 0; --i) {
                this.getMascots().get(i).dispose();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void applyVisualSettings() {
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            for (Mascot mascot : list) mascot.applyVisualSettings();
        }
    }

    public int getCount() {
        List<Mascot> list = this.getMascots();
        synchronized (list) {
            return this.getMascots().size();
        }
    }

    public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
        this.exitOnLastRemoved = exitOnLastRemoved;
    }

    public boolean isExitOnLastRemoved() {
        return this.exitOnLastRemoved;
    }

    private List<Mascot> getMascots() {
        return this.mascots;
    }

    private Set<Mascot> getAdded() {
        return this.added;
    }

    private Set<Mascot> getRemoved() {
        return this.removed;
    }
}

