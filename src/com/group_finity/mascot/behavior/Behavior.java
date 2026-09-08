/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;
import java.awt.event.MouseEvent;

public interface Behavior {
    public void init(Mascot var1) throws CantBeAliveException;

    public void next() throws CantBeAliveException;

    public void mousePressed(MouseEvent var1) throws CantBeAliveException;

    public void mouseReleased(MouseEvent var1) throws CantBeAliveException;
}

