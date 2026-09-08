/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.script;

import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

public class Constant
extends Variable {
    private final Object value;

    public Constant(Object value) {
        this.value = value;
    }

    public String toString() {
        return this.value == null ? "null" : this.value.toString();
    }

    private Object getValue() {
        return this.value;
    }

    @Override
    public void init() {
    }

    @Override
    public void initFrame() {
    }

    @Override
    public Object get(VariableMap variables) {
        return this.getValue();
    }
}

