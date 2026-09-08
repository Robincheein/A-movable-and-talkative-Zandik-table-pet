/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.Fall;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FallWithIE
extends Fall {
    private static final Logger log = Logger.getLogger(FallWithIE.class.getName());
    public static final String PARAMETER_IEOFFSETX = "IE\u306e\u7aefX";
    private static final int DEFAULT_IEOFFSETX = 0;
    public static final String PARAMETER_IEOFFSETY = "IE\u306e\u7aefY";
    private static final int DEFAULT_IEOFFSETY = 0;

    public FallWithIE(List<Animation> animations, VariableMap params) {
        super(animations, params);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        Area activeIE = this.getEnvironment().getActiveIE();
        if (!activeIE.isVisible()) {
            log.log(Level.INFO, "IE\u304c\u975e\u8868\u793a\u306b\u306a\u3063\u305f({0},{1})", new Object[]{this.getMascot(), this});
            throw new LostGroundException();
        }
        int offsetX = this.getIEOffsetX();
        int offsetY = this.getIEOffsetY();
        if (this.getMascot().isLookRight()) {
            if (this.getMascot().getAnchor().x - offsetX != activeIE.getLeft() || this.getMascot().getAnchor().y + offsetY != activeIE.getBottom()) {
                log.log(Level.INFO, "IE\u304b\u3089\u96e2\u308c\u305f({0},{1})", new Object[]{this.getMascot(), this});
                throw new LostGroundException();
            }
        } else if (this.getMascot().getAnchor().x + offsetX != activeIE.getRight() || this.getMascot().getAnchor().y + offsetY != activeIE.getBottom()) {
            log.log(Level.INFO, "IE\u304b\u3089\u96e2\u308c\u305f({0},{1})", new Object[]{this.getMascot(), this});
            throw new LostGroundException();
        }
        super.tick();
        if (activeIE.isVisible()) {
            if (this.getMascot().isLookRight()) {
                this.getEnvironment().moveActiveIE(new Point(this.getMascot().getAnchor().x - offsetX, this.getMascot().getAnchor().y + offsetY - activeIE.getHeight()));
            } else {
                this.getEnvironment().moveActiveIE(new Point(this.getMascot().getAnchor().x + offsetX - activeIE.getWidth(), this.getMascot().getAnchor().y + offsetY - activeIE.getHeight()));
            }
        }
    }

    private int getIEOffsetY() throws VariableException {
        return ((Number)this.eval(PARAMETER_IEOFFSETY, Number.class, 0)).intValue();
    }

    private int getIEOffsetX() throws VariableException {
        return ((Number)this.eval(PARAMETER_IEOFFSETX, Number.class, 0)).intValue();
    }
}

