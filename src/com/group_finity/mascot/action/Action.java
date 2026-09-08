/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

public interface Action {
    public void init(Mascot var1) throws VariableException;

    public boolean hasNext() throws VariableException;

    public void next() throws LostGroundException, VariableException;
}

