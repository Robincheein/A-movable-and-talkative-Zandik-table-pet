package com.group_finity.mascot;

import com.group_finity.mascot.PetMood.Reaction;

/** Pure deterministic tests: no GUI, API, filesystem or real waiting. */
public final class PetMoodSelfTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        long[] clock = {1000000};
        double[] roll = {.5};
        PetMood mood = new PetMood(() -> clock[0], () -> roll[0]);
        PetMood other = new PetMood(() -> clock[0], () -> .5);
        check(mood.getPatience() == 60 && mood.getLabel().equals("平静"), "Initial mood");
        check(mood.touch() == Reaction.REFUSE && mood.getPatience() == 42, "Default refusal");
        check(mood.touch() == Reaction.REFUSE && mood.getPatience() == 14, "Repeated touch penalty");
        check(mood.touch() == Reaction.ANGRY && mood.getPatience() == 0, "Irritated refusal");
        roll[0] = .99;
        check(mood.touch() == Reaction.REFUSE, "Bad mood must never allow touch");
        check(other.getPatience() == 60, "Mood leaked between pets");
        clock[0] += 1000000;
        check(mood.getPatience() == 75 && mood.getLabel().equals("不错"), "Recovery cap");
        check(mood.touch() == Reaction.ALLOW && mood.getPatience() == 57, "Good mood acceptance/no repeat penalty");
        clock[0] -= 500000;
        check(mood.getPatience() == 57, "Clock reversal corrupts patience");
        // Count an evenly spaced random sample, with fresh state per trial.
        int[] neutral = new int[3], good = new int[3], bad = new int[3];
        for (int i = 0; i < 100; i++) {
            final double sample = (i + .5) / 100;
            long[] time = {0};
            PetMood trial = new PetMood(() -> time[0], () -> sample);
            neutral[trial.touch().ordinal()]++;
            trial.touch();
            bad[trial.touch().ordinal()]++;
            time[0] = 1000000;
            good[trial.touch().ordinal()]++;
        }
        check(neutral[0] == 80 && neutral[1] == 15 && neutral[2] == 5, "Neutral weights");
        check(good[0] == 50 && good[1] == 5 && good[2] == 45, "Good weights");
        check(bad[0] == 25 && bad[1] == 75 && bad[2] == 0, "Bad weights");
        System.out.println("PET_MOOD_OK: weights, repetition, recovery, bounds, clock reversal, per-pet isolation");
    }
}
