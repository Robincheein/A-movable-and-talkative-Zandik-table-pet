/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.script;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Script
extends Variable {
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static final ScriptEngine engine = manager.getEngineByMimeType("text/javascript");
    private final String source;
    private final boolean clearAtInitFrame;
    private final CompiledScript compiled;
    private Object value;

    public Script(String source, boolean clearAtInitFrame) throws VariableException {
        this.source = source;
        this.clearAtInitFrame = clearAtInitFrame;
        try {
            this.compiled = ((Compilable)((Object)engine)).compile(this.source);
        }
        catch (ScriptException e) {
            throw new VariableException("\u30b9\u30af\u30ea\u30d7\u30c8\u306e\u30b3\u30f3\u30d1\u30a4\u30eb\u3067\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f: " + this.source, e);
        }
    }

    public String toString() {
        return this.isClearAtInitFrame() ? "#{" + this.getSource() + "}" : "${" + this.getSource() + "}";
    }

    @Override
    public void init() {
        this.setValue(null);
    }

    @Override
    public void initFrame() {
        if (this.isClearAtInitFrame()) {
            this.setValue(null);
        }
    }

    @Override
    public synchronized Object get(VariableMap variables) throws VariableException {
        if (this.getValue() != null) {
            return this.getValue();
        }
        try {
            this.setValue(this.getCompiled().eval(variables));
        }
        catch (ScriptException e) {
            throw new VariableException("\u30b9\u30af\u30ea\u30d7\u30c8\u306e\u8a55\u4fa1\u3067\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f: " + this.source, e);
        }
        return this.getValue();
    }

    private void setValue(Object value) {
        this.value = value;
    }

    private Object getValue() {
        return this.value;
    }

    private boolean isClearAtInitFrame() {
        return this.clearAtInitFrame;
    }

    private CompiledScript getCompiled() {
        return this.compiled;
    }

    private String getSource() {
        return this.source;
    }
}

