/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImagePairLoader {
    public static ImagePair load(String name, Point center) throws IOException {
        String rightName = name.replaceAll("\\.[a-zA-Z]+$", "-r$0");
        BufferedImage leftImage = ImageIO.read(ImagePairLoader.class.getResource(name));
        BufferedImage rightImage = ImagePairLoader.class.getResource(rightName) == null
                ? ImagePairLoader.flip(leftImage) : ImageIO.read(ImagePairLoader.class.getResource(rightName));
        return new ImagePair(new MascotImage(leftImage, center),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - center.x, center.y)));
    }

    private static BufferedImage flip(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
            }
        }
        return copy;
    }
}