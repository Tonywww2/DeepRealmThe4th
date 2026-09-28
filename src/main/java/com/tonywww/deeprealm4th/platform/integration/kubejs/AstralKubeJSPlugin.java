package com.tonywww.deeprealm4th.platform.integration.kubejs;

import com.tonywww.deeprealm4th.integration.kubejs.AstralRules;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.AstralContainers;
import com.tonywww.deeprealm4th.astral.FillerActivation;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreType;
import dev.latvian.mods.kubejs.script.ScriptType;

//? if forge {
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
//?} else {
/*import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
*///?}

/** Optional KubeJS entry point. KubeJS alone loads this class from kubejs.plugins.txt. */
//? if forge {
public final class AstralKubeJSPlugin extends KubeJSPlugin {
//?} else {
/*public final class AstralKubeJSPlugin implements KubeJSPlugin {
*///?}
    //? if forge {
    @Override
    public void registerBindings(BindingsEvent bindings) {
    //?} else {
    /*@Override
    public void registerBindings(BindingRegistry bindings) {
    *///?}
        //? if forge {
        if (bindings.getType() != ScriptType.STARTUP) return;
        //?} else {
        /*if (bindings.type() != ScriptType.STARTUP) return;
        *///?}
        bindings.add("AstralFillers", AstralFillers.class);
        bindings.add("AstralContainers", AstralContainers.class);
        bindings.add("FillerDefinition", FillerDefinition.class);
        bindings.add("FillerActivation", FillerActivation.class);
        bindings.add("ScoreType", ScoreType.class);
        bindings.add("AstralRules", AstralRules.class);
    }
}
