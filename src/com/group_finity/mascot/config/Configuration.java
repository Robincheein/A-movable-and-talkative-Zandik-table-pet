/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.config;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.ActionBuilder;
import com.group_finity.mascot.config.BehaviorBuilder;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {
    private static final Logger log = Logger.getLogger(Configuration.class.getName());
    private final Map<String, String> constants = new LinkedHashMap<String, String>();
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<String, ActionBuilder>();
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<String, BehaviorBuilder>();

    public void load(Entry configurationNode) throws IOException, ConfigurationException {
        log.log(Level.INFO, "\u8a2d\u5b9a\u8aad\u307f\u8fbc\u307f\u958b\u59cb");
        for (Entry constant : configurationNode.selectChildren("\u5b9a\u6570")) {
            log.log(Level.INFO, "\u5b9a\u6570...");
            this.constants.put(constant.getAttribute("\u540d\u524d"), constant.getAttribute("\u5024"));
        }
        for (Entry list : configurationNode.selectChildren("\u52d5\u4f5c\u30ea\u30b9\u30c8")) {
            log.log(Level.INFO, "\u52d5\u4f5c\u30ea\u30b9\u30c8...");
            for (Entry node : list.selectChildren("\u52d5\u4f5c")) {
                ActionBuilder action = new ActionBuilder(this, node);
                if (this.getActionBuilders().containsKey(action.getName())) {
                    throw new ConfigurationException("\u52d5\u4f5c\u306e\u540d\u524d\u304c\u91cd\u8907\u3057\u3066\u3044\u307e\u3059:" + action.getName());
                }
                this.getActionBuilders().put(action.getName(), action);
            }
        }
        for (Entry list : configurationNode.selectChildren("\u884c\u52d5\u30ea\u30b9\u30c8")) {
            log.log(Level.INFO, "\u884c\u52d5\u30ea\u30b9\u30c8...");
            this.loadBehaviors(list, new ArrayList<String>());
        }
        log.log(Level.INFO, "\u8a2d\u5b9a\u8aad\u307f\u8fbc\u307f\u5b8c\u4e86");
    }

    private void loadBehaviors(Entry list, List<String> conditions) {
        for (Entry node : list.getChildren()) {
            if (node.getName().equals("\u6761\u4ef6")) {
                ArrayList<String> newConditions = new ArrayList<String>(conditions);
                newConditions.add(node.getAttribute("\u6761\u4ef6"));
                this.loadBehaviors(node, newConditions);
                continue;
            }
            if (!node.getName().equals("\u884c\u52d5")) continue;
            BehaviorBuilder behavior = new BehaviorBuilder(this, node, conditions);
            this.getBehaviorBuilders().put(behavior.getName(), behavior);
        }
    }

    public Action buildAction(String name, Map<String, String> params) throws ActionInstantiationException {
        ActionBuilder factory = this.actionBuilders.get(name);
        if (factory == null) {
            throw new ActionInstantiationException("\u5bfe\u5fdc\u3059\u308b\u52d5\u4f5c\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093: " + name);
        }
        return factory.buildAction(params);
    }

    public void validate() throws ConfigurationException {
        for (ActionBuilder actionBuilder : this.getActionBuilders().values()) {
            actionBuilder.validate();
        }
        for (BehaviorBuilder behaviorBuilder : this.getBehaviorBuilders().values()) {
            behaviorBuilder.validate();
        }
    }

    public Behavior buildBehavior(String previousName, Mascot mascot) throws BehaviorInstantiationException {
        VariableMap context = new VariableMap();
        context.put("mascot", (Object)mascot);
        for (Map.Entry<String, String> e : this.constants.entrySet()) {
            context.put(e.getKey(), (Object)e.getValue());
        }
        ArrayList<BehaviorBuilder> candidates = new ArrayList<BehaviorBuilder>();
        long totalFrequency = 0L;
        for (BehaviorBuilder behaviorFactory : this.getBehaviorBuilders().values()) {
            try {
                if (!behaviorFactory.isEffective(context)) continue;
                candidates.add(behaviorFactory);
                totalFrequency += (long)behaviorFactory.getFrequency();
            }
            catch (VariableException e) {
                log.log(Level.WARNING, "\u884c\u52d5\u983b\u5ea6\u306e\u8a55\u4fa1\u4e2d\u306b\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f", e);
            }
        }
        if (previousName != null) {
            BehaviorBuilder previousBehaviorFactory = this.getBehaviorBuilders().get(previousName);
            if (!previousBehaviorFactory.isNextAdditive()) {
                totalFrequency = 0L;
                candidates.clear();
            }
            for (BehaviorBuilder behaviorFactory : previousBehaviorFactory.getNextBehaviorBuilders()) {
                try {
                    if (!behaviorFactory.isEffective(context)) continue;
                    candidates.add(behaviorFactory);
                    totalFrequency += (long)behaviorFactory.getFrequency();
                }
                catch (VariableException e) {
                    log.log(Level.WARNING, "\u884c\u52d5\u983b\u5ea6\u306e\u8a55\u4fa1\u4e2d\u306b\u30a8\u30e9\u30fc\u304c\u767a\u751f\u3057\u307e\u3057\u305f", e);
                }
            }
        }
        if (totalFrequency == 0L) {
            // 禁用爬墙后，墙边/半空可能没有候选动作；原地落下而不是传送到屏幕顶部。
            return this.buildBehavior("\u843d\u4e0b\u3059\u308b");
        }
        double random = Math.random() * (double)totalFrequency;
        for (BehaviorBuilder behaviorFactory : candidates) {
            if (!((random -= (double)behaviorFactory.getFrequency()) < 0.0)) continue;
            return behaviorFactory.buildBehavior();
        }
        return null;
    }

    public Behavior buildBehavior(String name) throws BehaviorInstantiationException {
        return this.getBehaviorBuilders().get(name).buildBehavior();
    }

    public Map<String, String> getConstants() {
        return this.constants;
    }

    public Map<String, ActionBuilder> getActionBuilders() {
        return this.actionBuilders;
    }

    public Map<String, BehaviorBuilder> getBehaviorBuilders() {
        return this.behaviorBuilders;
    }
}
