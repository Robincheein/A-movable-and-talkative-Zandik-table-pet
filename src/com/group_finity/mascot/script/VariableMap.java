/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.script;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Constant;
import com.group_finity.mascot.script.Variable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;

public class VariableMap
extends AbstractMap<String, Object>
implements Bindings {
    private final Map<String, Variable> rawMap = new LinkedHashMap<String, Variable>();
    private final Set<Map.Entry<String, Object>> entrySet = new AbstractSet<Map.Entry<String, Object>>(){

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new Iterator<Map.Entry<String, Object>>(){
                private Iterator<Map.Entry<String, Variable>> rawIterator;
                {
                    this.rawIterator = VariableMap.this.getRawMap().entrySet().iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.rawIterator.hasNext();
                }

                @Override
                public Map.Entry<String, Object> next() {
                    final Map.Entry<String, Variable> rawKeyValue = this.rawIterator.next();
                    final Variable value = rawKeyValue.getValue();
                    return new Map.Entry<String, Object>(){

                        @Override
                        public String getKey() {
                            return (String)rawKeyValue.getKey();
                        }

                        @Override
                        public Object getValue() {
                            try {
                                return ((Variable)value).get(VariableMap.this);
                            }
                            catch (VariableException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public Object setValue(Object value2) {
                            throw new UnsupportedOperationException("setValue is not supported");
                        }
                    };
                }

                @Override
                public void remove() {
                    this.rawIterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return VariableMap.this.getRawMap().size();
        }
    };

    public Map<String, Variable> getRawMap() {
        return this.rawMap;
    }

    public void init() {
        for (Variable o : this.getRawMap().values()) {
            o.init();
        }
    }

    public void initFrame() {
        for (Variable o : this.getRawMap().values()) {
            o.initFrame();
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return this.entrySet;
    }

    @Override
    public Object put(String key, Object value) {
        Variable result = value instanceof Variable ? this.getRawMap().put(key, (Variable)value) : this.getRawMap().put(key, new Constant(value));
        return result;
    }
}

