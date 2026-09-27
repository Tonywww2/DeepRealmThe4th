package com.tony.deeprealmtheforth.compat.kubejs;

import com.tony.deeprealmtheforth.astral.AstralFillers;
import com.tony.deeprealmtheforth.astral.AstralContainers;
import com.tony.deeprealmtheforth.astral.FillerActivation;
import com.tony.deeprealmtheforth.astral.FillerDefinition;
import com.tony.deeprealmtheforth.astral.ScoreType;
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
