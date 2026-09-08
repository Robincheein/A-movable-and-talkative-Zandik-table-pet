/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.image;

import com.group_finity.mascot.image.MascotImage;

public class ImagePair {
    private MascotImage leftImage;
    private MascotImage rightImage;

    public ImagePair(MascotImage leftImage, MascotImage rightImage) {
        this.leftImage = leftImage;
        this.rightImage = rightImage;
    }

    public MascotImage getImage(boolean lookRight) {
        return lookRight ? this.getRightImage() : this.getLeftImage();
    }

    private MascotImage getLeftImage() {
        return this.leftImage;
    }

    private MascotImage getRightImage() {
        return this.rightImage;
    }
}

