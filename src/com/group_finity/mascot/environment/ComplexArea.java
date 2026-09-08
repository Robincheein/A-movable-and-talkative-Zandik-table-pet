/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.environment;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.FloorCeiling;
import com.group_finity.mascot.environment.Wall;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ComplexArea {
    private Map<String, Area> areas = new HashMap<String, Area>();

    public void set(Map<String, Rectangle> rectangles) {
        this.retain(rectangles.keySet());
        for (Map.Entry<String, Rectangle> e : rectangles.entrySet()) {
            this.set(e.getKey(), e.getValue());
        }
    }

    public void set(String name, Rectangle value) {
        for (Area area : this.areas.values()) {
            if (area.getLeft() != value.x || area.getTop() != value.y || area.getWidth() != value.width || area.getHeight() != value.height) continue;
            return;
        }
        Area area = this.areas.get(name);
        if (area == null) {
            area = new Area();
            this.areas.put(name, area);
        }
        area.set(value);
    }

    public void retain(Collection<String> deviceNames) {
        Iterator<String> i = this.areas.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            if (deviceNames.contains(key)) continue;
            i.remove();
        }
    }

    public FloorCeiling getBottomBorder(Point location) {
        FloorCeiling ret = null;
        for (Area area : this.areas.values()) {
            if (!area.getBottomBorder().isOn(location)) continue;
            ret = area.getBottomBorder();
        }
        for (Area area : this.areas.values()) {
            if (!area.getTopBorder().isOn(location)) continue;
            ret = null;
        }
        return ret;
    }

    public FloorCeiling getTopBorder(Point location) {
        FloorCeiling ret = null;
        for (Area area : this.areas.values()) {
            if (!area.getTopBorder().isOn(location)) continue;
            ret = area.getTopBorder();
        }
        for (Area area : this.areas.values()) {
            if (!area.getBottomBorder().isOn(location)) continue;
            ret = null;
        }
        return ret;
    }

    public Wall getLeftBorder(Point location) {
        Wall ret = null;
        for (Area area : this.areas.values()) {
            if (!area.getLeftBorder().isOn(location)) continue;
            ret = area.getRightBorder();
        }
        for (Area area : this.areas.values()) {
            if (!area.getRightBorder().isOn(location)) continue;
            ret = null;
        }
        return ret;
    }

    public Wall getRightBorder(Point location) {
        Wall ret = null;
        for (Area area : this.areas.values()) {
            if (!area.getRightBorder().isOn(location)) continue;
            ret = area.getRightBorder();
        }
        for (Area area : this.areas.values()) {
            if (!area.getLeftBorder().isOn(location)) continue;
            ret = null;
        }
        return ret;
    }

    public Collection<Area> getAreas() {
        return this.areas.values();
    }
}

