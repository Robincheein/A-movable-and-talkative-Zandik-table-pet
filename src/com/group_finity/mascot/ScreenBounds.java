package com.group_finity.mascot;

import java.awt.Point;
import java.awt.Rectangle;

/** Clamp the visible artwork, not its transparent canvas or just its foot anchor. */
public final class ScreenBounds {
    private ScreenBounds() {}

    public static Point clamp(Point anchor, Point pivot, Rectangle visible, Rectangle work) {
        if (work.width <= 0 || work.height <= 0) return new Point(anchor);
        int minX = work.x + pivot.x - visible.x;
        int maxX = work.x + work.width + pivot.x - visible.x - visible.width;
        int minY = work.y + pivot.y - visible.y;
        // The foot anchor cannot pass the floor even if a pose has bottom padding.
        int maxY = Math.min(work.y + work.height,
                work.y + work.height + pivot.y - visible.y - visible.height);
        return new Point(limit(anchor.x, minX, maxX), limit(anchor.y, minY, maxY));
    }

    private static int limit(int value, int low, int high) {
        // An oversized sprite cannot fit; center overflow rather than oscillating.
        return low > high ? low + (high - low) / 2 : Math.max(low, Math.min(high, value));
    }
}
