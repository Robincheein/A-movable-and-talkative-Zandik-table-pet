package com.group_finity.mascot;

import java.awt.Point;
import java.awt.Rectangle;

/** Top/left/right guard only. The physics engine exclusively owns the floor. */
public final class ScreenBounds {
    private ScreenBounds() {}

    public static Point clamp(Point anchor, Point pivot, Rectangle visible, Rectangle work) {
        if (work.width <= 0 || work.height <= 0) return new Point(anchor);
        int minX = work.x + pivot.x - visible.x;
        int maxX = work.x + work.width + pivot.x - visible.x - visible.width;
        int minY = work.y + pivot.y - visible.y;
        // Never lift the foot anchor to fit the bottom of a sprite. Frame padding,
        // tail tips and scale rounding can extend below it; lifting would make the
        // floor test fail and restart falling/landing on each animation change.
        return new Point(limit(anchor.x, minX, maxX), Math.max(minY, anchor.y));
    }

    private static int limit(int value, int low, int high) {
        // An oversized sprite cannot fit; center overflow rather than oscillating.
        return low > high ? low + (high - low) / 2 : Math.max(low, Math.min(high, value));
    }
}
