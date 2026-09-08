/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.action;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ComplexAction;
import com.group_finity.mascot.script.VariableMap;
import java.util.logging.Logger;

public class Select
extends ComplexAction {
    private static final Logger log = Logger.getLogger(Select.class.getName());

    public Select(VariableMap params, Action ... actions) {
        super(params, actions);
    }
}

