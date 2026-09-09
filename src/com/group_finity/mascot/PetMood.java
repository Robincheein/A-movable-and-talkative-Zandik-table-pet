package com.group_finity.mascot;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/** Per-pet local touch tolerance, not an inferred emotion or chat memory. */
public final class PetMood {
    public enum Reaction { REFUSE, ANGRY, ALLOW }
    private final LongSupplier clock;
    private final DoubleSupplier random;
    private double patience = 60;
    private long updatedAt, lastTouch;
    private boolean touched;

    public PetMood() { this(System::currentTimeMillis, Math::random); }
    /** Injectable clock/random for deterministic offline tests. */
    public PetMood(LongSupplier clock, DoubleSupplier random) {
        this.clock = clock; this.random = random; updatedAt = clock.getAsLong();
    }
    private long recover() {
        long now = Math.max(updatedAt, clock.getAsLong());
        patience = Math.min(75, patience + (now - updatedAt) / 10000.0);
        updatedAt = now;
        return now;
    }
    public synchronized double getPatience() { recover(); return patience; }
    public synchronized String getLabel() {
        double value = getPatience();
        return value < 35 ? "烦躁" : value < 65 ? "平静" : "不错";
    }
    public synchronized Reaction touch() {
        long now = recover();
        double roll = random.getAsDouble();
        Reaction reaction;
        if (patience < 35) reaction = roll < .75 ? Reaction.ANGRY : Reaction.REFUSE;
        else if (patience < 65) reaction = roll < .15 ? Reaction.ANGRY : roll < .95 ? Reaction.REFUSE : Reaction.ALLOW;
        else reaction = roll < .05 ? Reaction.ANGRY : roll < .55 ? Reaction.REFUSE : Reaction.ALLOW;
        patience = Math.max(0, patience - 18 - (touched && now - lastTouch < 20000 ? 10 : 0));
        lastTouch = now; touched = true;
        return reaction;
    }
}
