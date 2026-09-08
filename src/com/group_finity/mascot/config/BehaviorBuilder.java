/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.config;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BehaviorBuilder {
    private static final Logger log = Logger.getLogger(BehaviorBuilder.class.getName());
    private final Configuration configuration;
    private final String name;
    private final String actionName;
    private final int frequency;
    private final List<String> conditions;
    private final boolean nextAdditive;
    private final List<BehaviorBuilder> nextBehaviorBuilders = new ArrayList<BehaviorBuilder>();
    private final Map<String, String> params = new LinkedHashMap<String, String>();

    public BehaviorBuilder(Configuration configuration, Entry behaviorNode, List<String> conditions) {
        this.configuration = configuration;
        this.name = behaviorNode.getAttribute("\u540d\u524d");
        this.actionName = behaviorNode.getAttribute("\u52d5\u4f5c") == null ? this.getName() : behaviorNode.getAttribute("\u52d5\u4f5c");
        this.frequency = Integer.parseInt(behaviorNode.getAttribute("\u983b\u5ea6"));
        this.conditions = new ArrayList<String>(conditions);
        this.getConditions().add(behaviorNode.getAttribute("\u6761\u4ef6"));
        log.log(Level.INFO, "\u884c\u52d5\u8aad\u307f\u8fbc\u307f\u958b\u59cb({0})", this);
        this.getParams().putAll(behaviorNode.getAttributes());
        this.getParams().remove("\u540d\u524d");
        this.getParams().remove("\u52d5\u4f5c");
        this.getParams().remove("\u983b\u5ea6");
        this.getParams().remove("\u6761\u4ef6");
        boolean nextAdditive = true;
        for (Entry nextList : behaviorNode.selectChildren("\u6b21\u306e\u884c\u52d5\u30ea\u30b9\u30c8")) {
            log.log(Level.INFO, "\u6b21\u306e\u884c\u52d5\u30ea\u30b9\u30c8...");
            nextAdditive = Boolean.parseBoolean(nextList.getAttribute("\u8ffd\u52a0"));
            this.loadBehaviors(nextList, new ArrayList<String>());
        }
        this.nextAdditive = nextAdditive;
        log.log(Level.INFO, "\u884c\u52d5\u8aad\u307f\u8fbc\u307f\u5b8c\u4e86({0})", this);
    }

    public String toString() {
        return "\u884c\u52d5(" + this.getName() + "," + this.getFrequency() + "," + this.getActionName() + ")";
    }

    private void loadBehaviors(Entry list, List<String> conditions) {
        for (Entry node : list.getChildren()) {
            if (node.getName().equals("\u6761\u4ef6")) {
                ArrayList<String> newConditions = new ArrayList<String>(conditions);
                newConditions.add(node.getAttribute("\u6761\u4ef6"));
                this.loadBehaviors(node, newConditions);
                continue;
            }
            if (!node.getName().equals("\u884c\u52d5\u53c2\u7167")) continue;
            BehaviorBuilder behavior = new BehaviorBuilder(this.getConfiguration(), node, conditions);
            this.getNextBehaviorBuilders().add(behavior);
        }
    }

    public void validate() throws ConfigurationException {
        if (!this.getConfiguration().getActionBuilders().containsKey(this.getActionName())) {
            throw new ConfigurationException("\u5bfe\u5fdc\u3059\u308b\u52d5\u4f5c\u304c\u5b58\u5728\u3057\u307e\u305b\u3093(" + this + ")");
        }
    }

    public Behavior buildBehavior() throws BehaviorInstantiationException {
        try {
            return new UserBehavior(this.getName(), this.getConfiguration().buildAction(this.getActionName(), this.getParams()), this.getConfiguration());
        }
        catch (ActionInstantiationException e) {
            throw new BehaviorInstantiationException("\u5bfe\u5fdc\u3059\u308b\u52d5\u4f5c\u306e\u521d\u671f\u5316\u306b\u5931\u6557\u3057\u307e\u3057\u305f(" + this + ")", e);
        }
    }

    public boolean isEffective(VariableMap context) throws VariableException {
        for (String condition : this.getConditions()) {
            if (condition == null || ((Boolean)Variable.parse(condition).get(context)).booleanValue()) continue;
            return false;
        }
        return true;
    }

    String getName() {
        return this.name;
    }

    int getFrequency() {
        return this.frequency;
    }

    private String getActionName() {
        return this.actionName;
    }

    private Map<String, String> getParams() {
        return this.params;
    }

    private List<String> getConditions() {
        return this.conditions;
    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    boolean isNextAdditive() {
        return this.nextAdditive;
    }

    List<BehaviorBuilder> getNextBehaviorBuilders() {
        return this.nextBehaviorBuilders;
    }
}

