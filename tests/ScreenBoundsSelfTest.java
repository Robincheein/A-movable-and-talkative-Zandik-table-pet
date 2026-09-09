package com.group_finity.mascot;

import java.awt.Point;
import java.awt.Rectangle;

public final class ScreenBoundsSelfTest {
    public static void main(String[] args) {
        for (Rectangle work : new Rectangle[]{new Rectangle(0,0,1920,1040), new Rectangle(-1920,-300,1920,1080)}) {
            for (double scale : new double[]{.4,.7,1,1.5}) {
                Point pivot = new Point((int)Math.round(64*scale), (int)Math.round(128*scale));
                Rectangle visible = new Rectangle((int)Math.round(10*scale),(int)Math.round(25*scale),
                        (int)Math.round(108*scale),(int)Math.round(103*scale));
                for (int x : new int[]{work.x-5000,work.x,work.x+work.width/2,work.x+work.width+5000})
                    for (int y : new int[]{work.y-5000,work.y,work.y+work.height/2,work.y+work.height+5000}) {
                        Point a = ScreenBounds.clamp(new Point(x,y),pivot,visible,work);
                        Rectangle actual = new Rectangle(visible); actual.translate(a.x-pivot.x,a.y-pivot.y);
                        if (!work.contains(actual) || a.y > work.y+work.height) throw new AssertionError(actual);
                        if (!a.equals(ScreenBounds.clamp(a,pivot,visible,work))) throw new AssertionError("Clamp jitter");
                    }
            }
        }
        System.out.println("SCREEN_BOUNDS_OK: four edges, scales, negative coordinates, stable clamping");
    }
}
