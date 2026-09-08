/*
 * Decompiled with CFR 0.152.
 */
package com.group_finity.mascot.config;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.Animate;
import com.group_finity.mascot.action.Move;
import com.group_finity.mascot.action.Select;
import com.group_finity.mascot.action.Sequence;
import com.group_finity.mascot.action.Stay;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.config.ActionRef;
import com.group_finity.mascot.config.AnimationBuilder;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.config.IActionBuilder;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionBuilder
implements IActionBuilder {
    private static final Logger log = Logger.getLogger(ActionBuilder.class.getName());
    private final String type;
    private final String name;
    private final String className;
    private final Map<String, String> params = new LinkedHashMap<String, String>();
    private final List<AnimationBuilder> animationBuilders = new ArrayList<AnimationBuilder>();
    private final List<IActionBuilder> actionRefs = new ArrayList<IActionBuilder>();

    public ActionBuilder(Configuration configuration, Entry actionNode) throws IOException {
        this.name = actionNode.getAttribute("\u540d\u524d");
        this.type = actionNode.getAttribute("\u7a2e\u985e");
        this.className = actionNode.getAttribute("\u30af\u30e9\u30b9");
        log.log(Level.INFO, "\u52d5\u4f5c\u8aad\u307f\u8fbc\u307f\u958b\u59cb({0})", this);
        this.getParams().putAll(actionNode.getAttributes());
        for (Entry node : actionNode.selectChildren("\u30a2\u30cb\u30e1\u30fc\u30b7\u30e7\u30f3")) {
            this.getAnimationBuilders().add(new AnimationBuilder(node));
        }
        for (Entry node : actionNode.getChildren()) {
            if (node.getName().equals("\u52d5\u4f5c\u53c2\u7167")) {
                this.getActionRefs().add(new ActionRef(configuration, node));
                continue;
            }
            if (!node.getName().equals("\u52d5\u4f5c")) continue;
            this.getActionRefs().add(new ActionBuilder(configuration, node));
        }
        log.log(Level.INFO, "\u52d5\u4f5c\u8aad\u307f\u8fbc\u307f\u5b8c\u4e86");
    }

    public String toString() {
        return "\u52d5\u4f5c(" + this.getName() + "," + this.getType() + "," + this.getClassName() + ")";
    }

    @Override
    public Action buildAction(Map<String, String> params) throws ActionInstantiationException {
        try {
            VariableMap variables = this.createVariables(params);
            List<Animation> animations = this.createAnimations();
            List<Action> actions = this.createActions();
            if (this.type.equals("\u7d44\u307f\u8fbc\u307f")) {
                try {
                    Class<?> cls = Class.forName(this.getClassName());
                    try {
                        return (Action)cls.getConstructor(List.class, VariableMap.class).newInstance(animations, variables);
                    }
                    catch (Exception e) {
                        try {
                            return (Action)cls.getConstructor(VariableMap.class).newInstance(variables);
                        }
                        catch (Exception e2) {
                            return (Action)cls.newInstance();
                        }
                    }
                }
                catch (InstantiationException e) {
                    throw new ActionInstantiationException("\u52d5\u4f5c\u30af\u30e9\u30b9\u306e\u521d\u671f\u5316\u306b\u5931\u6557(" + this + ")", e);
                }
                catch (IllegalAccessException e) {
                    throw new ActionInstantiationException("\u52d5\u4f5c\u30af\u30e9\u30b9\u306b\u30a2\u30af\u30bb\u30b9\u3067\u304d\u307e\u305b\u3093(" + this + ")", e);
                }
                catch (ClassNotFoundException e) {
                    throw new ActionInstantiationException("\u52d5\u4f5c\u30af\u30e9\u30b9\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093(" + this + ")", e);
                }
            }
            if (this.type.equals("\u79fb\u52d5")) {
                return new Move(animations, variables);
            }
            if (this.type.equals("\u9759\u6b62")) {
                return new Stay(animations, variables);
            }
            if (this.type.equals("\u56fa\u5b9a")) {
                return new Animate(animations, variables);
            }
            if (this.type.equals("\u8907\u5408")) {
                return new Sequence(variables, actions.toArray(new Action[0]));
            }
            if (this.type.equals("\u9078\u629e")) {
                return new Select(variables, actions.toArray(new Action[0]));
            }
            throw new ActionInstantiationException("\u52d5\u4f5c\u306e\u7a2e\u985e\u304c\u4e0d\u660e(" + this + ")");
        }
        catch (AnimationInstantiationException e) {
            throw new ActionInstantiationException("\u30a2\u30cb\u30e1\u30fc\u30b7\u30e7\u30f3\u306e\u4f5c\u6210\u306b\u5931\u6557\u3057\u307e\u3057\u305f(" + this + ")", e);
        }
        catch (VariableException e) {
            throw new ActionInstantiationException("\u30d1\u30e9\u30e1\u30fc\u30bf\u306e\u8a55\u4fa1\u306b\u5931\u6557\u3057\u307e\u3057\u305f(" + this + ")", e);
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        for (IActionBuilder ref : this.getActionRefs()) {
            ref.validate();
        }
    }

    private List<Action> createActions() throws ActionInstantiationException {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (IActionBuilder ref : this.getActionRefs()) {
            actions.add(ref.buildAction(new HashMap<String, String>()));
        }
        return actions;
    }

    private List<Animation> createAnimations() throws AnimationInstantiationException {
        ArrayList<Animation> animations = new ArrayList<Animation>();
        for (AnimationBuilder animationFactory : this.getAnimationBuilders()) {
            animations.add(animationFactory.buildAnimation());
        }
        return animations;
    }

    private VariableMap createVariables(Map<String, String> params) throws VariableException {
        VariableMap variables = new VariableMap();
        for (Map.Entry<String, String> param : this.getParams().entrySet()) {
            variables.put(param.getKey(), (Object)Variable.parse(param.getValue()));
        }
        for (Map.Entry<String, String> param : params.entrySet()) {
            variables.put(param.getKey(), (Object)Variable.parse(param.getValue()));
        }
        return variables;
    }

    String getName() {
        return this.name;
    }

    private String getType() {
        return this.type;
    }

    private String getClassName() {
        return this.className;
    }

    private Map<String, String> getParams() {
        return this.params;
    }

    private List<AnimationBuilder> getAnimationBuilders() {
        return this.animationBuilders;
    }

    private List<IActionBuilder> getActionRefs() {
        return this.actionRefs;
    }
}

