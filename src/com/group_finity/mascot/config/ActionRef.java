/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.config;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.config.IActionBuilder;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionRef
implements IActionBuilder {
    private static final Logger log = Logger.getLogger(ActionRef.class.getName());
    private final Configuration configuration;
    private final String name;
    private final Map<String, String> params = new LinkedHashMap<String, String>();

    public ActionRef(Configuration configuration, Entry refNode) {
        this.configuration = configuration;
        this.name = refNode.getAttribute("\u540d\u524d");
        this.getParams().putAll(refNode.getAttributes());
        log.log(Level.INFO, "\u52d5\u4f5c\u53c2\u7167\u8aad\u307f\u8fbc\u307f({0})", this);
    }

    public String toString() {
        return "\u52d5\u4f5c\u53c2\u7167(" + this.getName() + ")";
    }

    private String getName() {
        return this.name;
    }

    private Map<String, String> getParams() {
        return this.params;
    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void validate() throws ConfigurationException {
        if (!this.getConfiguration().getActionBuilders().containsKey(this.getName())) {
            throw new ConfigurationException("\u5bfe\u5fdc\u3059\u308b\u52d5\u4f5c\u304c\u5b58\u5728\u3057\u307e\u305b\u3093(" + this + ")");
        }
    }

    @Override
    public Action buildAction(Map<String, String> params) throws ActionInstantiationException {
        LinkedHashMap<String, String> newParams = new LinkedHashMap<String, String>(params);
        newParams.putAll(this.getParams());
        return this.getConfiguration().buildAction(this.getName(), newParams);
    }
}

