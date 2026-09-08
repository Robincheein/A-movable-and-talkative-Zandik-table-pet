/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.script;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Constant;
import com.group_finity.mascot.script.Script;
import com.group_finity.mascot.script.VariableMap;

public abstract class Variable {
    public static Variable parse(String source) throws VariableException {
        Variable result = null;
        if (source != null) {
            result = source.startsWith("${") && source.endsWith("}") ? new Script(source.substring(2, source.length() - 1), false) : (source.startsWith("#{") && source.endsWith("}") ? new Script(source.substring(2, source.length() - 1), true) : new Constant(Variable.parseConstant(source)));
        }
        return result;
    }

    private static Object parseConstant(String source) {
        Object result = null;
        if (source != null) {
            if (source.equals("null")) {
                result = null;
            }
            if (source.equals("true")) {
                result = Boolean.TRUE;
            } else if (source.equals("false")) {
                result = Boolean.FALSE;
            } else {
                try {
                    result = Double.parseDouble(source);
                }
                catch (NumberFormatException e) {
                    result = source;
                }
            }
        }
        return result;
    }

    public abstract void init();

    public abstract void initFrame();

    public abstract Object get(VariableMap var1) throws VariableException;
}

