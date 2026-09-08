/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.ImagePair;
import java.awt.Point;

public class Pose {
    private final ImagePair image;
    private final int dx;
    private final int dy;
    private final int duration;

    public Pose(ImagePair image) {
        this(image, 0, 0, 1);
    }

    public Pose(ImagePair image, int duration) {
        this(image, 0, 0, duration);
    }

    public Pose(ImagePair image, int dx, int dy, int duration) {
        this.image = image;
        this.dx = dx;
        this.dy = dy;
        this.duration = duration;
    }

    public String toString() {
        return "\u59ff\u52e2(" + this.getImage() + "," + this.getDx() + "," + this.getDy() + "," + this.getDuration() + ")";
    }

    public void next(Mascot mascot) {
        mascot.setAnchor(new Point(mascot.getAnchor().x + (mascot.isLookRight() ? -this.getDx() : this.getDx()), mascot.getAnchor().y + this.getDy()));
        mascot.setImage(this.getImage().getImage(mascot.isLookRight()));
    }

    public int getDuration() {
        return this.duration;
    }

    public ImagePair getImage() {
        return this.image;
    }

    public int getDx() {
        return this.dx;
    }

    public int getDy() {
        return this.dy;
    }
}

