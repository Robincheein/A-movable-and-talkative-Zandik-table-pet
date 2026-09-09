package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.PetMood.Reaction;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.exception.LostGroundException;
import java.awt.Point;
import java.io.IOException;

/** One-shot hand-drawn head pat. No API calls or chat initialization. */
public final class FoxInteraction implements Action {
    public enum Kind {
        PET("摸摸头");
        public final String label;
        Kind(String label) { this.label = label; }
    }
    private static final String[] SPRITES = {"fox_pet_rest", "fox_pet_approach",
        "fox_pet_good_mood_allow", "fox_pet_normal_refuse", "fox_pet_angry_block", "fox_pet_withdraw",
        "fox_pet_near", "fox_pet_half_raise", "fox_pet_mid_push", "fox_pet_lower_paw",
        "fox_pet_stroke_left", "fox_pet_stroke_right"};
    private static MascotImage[][] cached;
    private final MascotImage[][] frames;
    // Pose index and hold duration, at 25 ticks/second.
    private final int[][] sequence;
    private Mascot mascot;
    private int phase, ticksInPhase, facing;

    public FoxInteraction(Reaction reaction) throws IOException {
        frames = frames();
        switch (reaction) {
        case ANGRY:
            sequence = new int[][]{{0,5}, {5,3}, {1,3}, {6,2}, {7,3}, {8,2},
                {3,2}, {4,6}, {3,2}, {8,2}, {7,3}, {9,4}, {5,3}, {0,6}};
            break;
        case ALLOW:
            sequence = new int[][]{{0,5}, {5,3}, {1,3}, {6,3}, {2,4}, {10,3},
                {2,3}, {11,3}, {2,3}, {10,3}, {2,3}, {6,3}, {1,3}, {5,3}, {0,6}};
            break;
        default:
            sequence = new int[][]{{0,5}, {5,3}, {1,3}, {6,3}, {7,4}, {8,3},
                {3,6}, {8,3}, {7,3}, {9,4}, {5,3}, {0,6}};
        }
    }

    public static boolean canStart(Mascot mascot) {
        return mascot.getEnvironment().getFloor().isOn(mascot.getAnchor());
    }

    public static boolean start(Mascot mascot, Kind kind, Configuration config) throws Exception {
        if (!canStart(mascot)) return false;
        frames(); // Missing artwork must not consume patience.
        FoxInteraction action = new FoxInteraction(mascot.getPetMood().touch());
        // Null resumes normal behavior selection; UserBehavior preserves dragging.
        mascot.setBehavior(new UserBehavior(null, action, config));
        mascot.setAnimatingPublic(true);
        return true;
    }

    @Override public void init(Mascot mascot) {
        this.mascot = mascot; phase = 0; ticksInPhase = 0;
        facing = mascot.isLookRight() ? 1 : 0;
    }
    @Override public boolean hasNext() { return phase < sequence.length; }
    @Override public void next() throws LostGroundException {
        if (!canStart(mascot)) throw new LostGroundException();
        if (!hasNext()) return;
        mascot.setImage(frames[facing][sequence[phase][0]]);
        if (++ticksInPhase >= sequence[phase][1]) { phase++; ticksInPhase = 0; }
    }

    private static synchronized MascotImage[][] frames() throws IOException {
        if (cached != null) return cached;
        MascotImage[][] result = new MascotImage[2][SPRITES.length];
        for (int i = 0; i < SPRITES.length; i++) {
            com.group_finity.mascot.image.ImagePair pair = ImagePairLoader.load("/" + SPRITES[i] + ".png", new Point(64, 128));
            result[0][i] = pair.getImage(false);
            result[1][i] = pair.getImage(true);
        }
        cached = result;
        return result;
    }
}
