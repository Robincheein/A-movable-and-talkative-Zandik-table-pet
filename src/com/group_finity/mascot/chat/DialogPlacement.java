package com.group_finity.mascot.chat;

import java.awt.Point;
import java.awt.Rectangle;

/** Position relative to visible pixels, not transparent sprite padding. */
public final class DialogPlacement {
    private DialogPlacement() {}
    public static Point locate(Rectangle pet, Rectangle workArea, int width, int height) {
        final int gap = 6;
        int x = pet.x + pet.width / 2 - width / 2;
        int y = pet.y - height - gap;
        if (y < workArea.y) y = pet.y + pet.height + gap;
        x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - width));
        y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - height));
        return new Point(x, y);
    }
}
