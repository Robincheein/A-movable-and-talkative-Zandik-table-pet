/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.config;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.script.Variable;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnimationBuilder {
    private static final Logger log = Logger.getLogger(AnimationBuilder.class.getName());
    private final String condition;
    private final List<Pose> poses = new ArrayList<Pose>();

    public AnimationBuilder(Entry animationNode) throws IOException {
        this.condition = animationNode.getAttribute("\u6761\u4ef6") == null ? "true" : animationNode.getAttribute("\u6761\u4ef6");
        log.log(Level.INFO, "\u30a2\u30cb\u30e1\u30fc\u30b7\u30e7\u30f3\u8aad\u307f\u8fbc\u307f\u958b\u59cb");
        for (Entry frameNode : animationNode.getChildren()) {
            this.getPoses().add(this.loadPose(frameNode));
        }
        log.log(Level.INFO, "\u30a2\u30cb\u30e1\u30fc\u30b7\u30e7\u30f3\u8aad\u307f\u8fbc\u307f\u5b8c\u4e86");
    }

    private Pose loadPose(Entry frameNode) throws IOException {
        String imageText = frameNode.getAttribute("\u753b\u50cf");
        String anchorText = frameNode.getAttribute("\u57fa\u6e96\u5ea7\u6a19");
        String moveText = frameNode.getAttribute("\u79fb\u52d5\u901f\u5ea6");
        String durationText = frameNode.getAttribute("\u9577\u3055");
        String[] anchorCoordinates = anchorText.split(",");
        Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));
        ImagePair image = ImagePairLoader.load(imageText, anchor);
        String[] moveCoordinates = moveText.split(",");
        Point move = new Point(Integer.parseInt(moveCoordinates[0]), Integer.parseInt(moveCoordinates[1]));
        int duration = Integer.parseInt(durationText);
        Pose pose = new Pose(image, move.x, move.y, duration);
        log.log(Level.INFO, "\u59ff\u52e2\u8aad\u307f\u8fbc\u307f({0})", pose);
        return pose;
    }

    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(Variable.parse(this.getCondition()), this.getPoses().toArray(new Pose[0]));
        }
        catch (VariableException e) {
            throw new AnimationInstantiationException("\u6761\u4ef6\u306e\u8a55\u4fa1\u306b\u5931\u6557\u3057\u307e\u3057\u305f", e);
        }
    }

    private List<Pose> getPoses() {
        return this.poses;
    }

    private String getCondition() {
        return this.condition;
    }
}

