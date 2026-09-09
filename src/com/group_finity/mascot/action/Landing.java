package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.List;

/** A brief feet-anchored settle, never the sleeping/collapsed sprite. */
public class Landing extends Stay {
    public Landing(List<Animation> animations, VariableMap params) { super(animations, params); }
    @Override public boolean hasNext() throws VariableException {
        return super.hasNext() && getTime() < 8;
    }
    @Override protected void tick() throws LostGroundException, VariableException {
        super.tick();
        int[] heights = {100, 96, 92, 94, 96, 98, 100, 100};
        getMascot().setImage(getMascot().getImage().motionFrame(0, heights[Math.min(7, getTime())]));
    }
}
