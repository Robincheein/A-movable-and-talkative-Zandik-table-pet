package com.group_finity.mascot.win;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Exercise the real frame queue without showing/moving a desktop window. */
public final class WindowFrameSelfTest {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static final class ProbeWindow extends WindowsTranslucentWindow {
        int frames;
        Rectangle lastBounds;
        WindowsNativeImage lastImage;
        @Override protected void displayFrame(Rectangle bounds, WindowsNativeImage image, float opacity) {
            check(SwingUtilities.isEventDispatchThread(), "Frame applied off EDT");
            frames++;
            lastBounds = bounds;
            lastImage = image;
        }
    }
    public static void main(String[] args) throws Exception {
        ProbeWindow[] holder = new ProbeWindow[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new ProbeWindow());
        ProbeWindow window = holder[0];
        WindowsNativeImage a = new WindowsNativeImage(new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB));
        WindowsNativeImage b = new WindowsNativeImage(new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB));
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            SwingUtilities.invokeLater(() -> {
                entered.countDown();
                try { check(release.await(5, TimeUnit.SECONDS), "EDT test gate timed out"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            });
            check(entered.await(5, TimeUnit.SECONDS), "EDT not ready");
            for (int i = 0; i < 100; i++) {
                Rectangle bounds = new Rectangle(i, 200, i % 2 == 0 ? 128 : 160, i % 2 == 0 ? 128 : 160);
                window.present(bounds, i % 2 == 0 ? a : b, 1);
                bounds.x = -999; // queued coordinates must be a snapshot, not an alias
            }
            release.countDown();
            SwingUtilities.invokeAndWait(() -> {});
            check(window.frames == 1, "Stale mouse positions were replayed");
            check(window.lastBounds.equals(new Rectangle(99, 200, 160, 160)) && window.lastImage == b,
                    "Bitmap/position snapshot torn or mutated");
            window.present(new Rectangle(101, 202, 128, 128), a, 1);
            SwingUtilities.invokeAndWait(() -> {});
            check(window.frames == 2 && window.lastImage == a, "Queue did not resume after draining");
            SwingUtilities.invokeAndWait(window::dispose);
            window.present(new Rectangle(500, 500, 128, 128), a, 1);
            SwingUtilities.invokeAndWait(() -> {});
            check(window.frames == 2, "Disposed pet was resurrected by a queued frame");
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(window::dispose);
        }
        System.out.println("WINDOW_FRAME_OK: EDT, coalescing, immutable bitmap/position pair, disposal");
        System.exit(0);
    }
}
