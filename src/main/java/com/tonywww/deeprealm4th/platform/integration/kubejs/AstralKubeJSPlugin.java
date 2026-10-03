package com.tonywww.deeprealm4th.platform.integration.kubejs;

import com.tonywww.deeprealm4th.integration.kubejs.AstralRules;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.AstralContainers;
import com.tonywww.deeprealm4th.astral.FillerActivation;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.AstralRuntime;
import com.tonywww.deeprealm4th.astral.EffectScope;
import com.tonywww.deeprealm4th.astral.data.AstralItemData;
import com.tonywww.deeprealm4th.astral.data.AstralDataSchemas;
import com.tonywww.deeprealm4th.astral.data.GemPayload;
import com.tonywww.deeprealm4th.astral.data.GemPayloadMigrations;
import com.tonywww.deeprealm4th.astral.data.GemPayloadTransfer;
import com.tonywww.deeprealm4th.astral.node.CellView;
import com.tonywww.deeprealm4th.astral.node.FillerFilter;
import com.tonywww.deeprealm4th.astral.node.FillerNode;
import com.tonywww.deeprealm4th.astral.node.FillerResult;
import com.tonywww.deeprealm4th.astral.node.FillerResultSummary;
import com.tonywww.deeprealm4th.astral.node.PlayerStateSnapshot;
import com.tonywww.deeprealm4th.astral.node.ReadPlan;
import com.tonywww.deeprealm4th.astral.process.AstralDataPredicates;
import com.tonywww.deeprealm4th.astral.process.AstralTransforms;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import com.tonywww.deeprealm4th.astral.tooltip.FillerPresentations;
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
        ScriptType type = bindings.getType();
        //?} else {
        /*ScriptType type = bindings.type();
        *///?}
        if (type == ScriptType.STARTUP) {
            bindings.add("AstralFillers", AstralFillers.class);
            bindings.add("AstralContainers", AstralContainers.class);
            bindings.add("FillerDefinition", FillerDefinition.class);
            bindings.add("FillerActivation", FillerActivation.class);
            bindings.add("EffectScope", EffectScope.class);
            bindings.add("AstralRules", AstralRules.class);
            bindings.add("FillerNode", FillerNode.class);
            bindings.add("ReadPlan", ReadPlan.class);
            bindings.add("FillerFilter", FillerFilter.class);
            bindings.add("AstralTransforms", AstralTransforms.class);
            bindings.add("AstralDataPredicates", AstralDataPredicates.class);
            bindings.add("FillerPresentations", FillerPresentations.class);
            bindings.add("GemPayloadMigrations", GemPayloadMigrations.class);
            bindings.add("AstralDataSchemas", AstralDataSchemas.class);
        }
        bindings.add("ScoreType", ScoreType.class);
        bindings.add("AstralNumber", AstralNumber.class);
        bindings.add("BigScoreSheet", BigScoreSheet.class);
        bindings.add("AstralItemData", AstralItemData.class);
        bindings.add("GemPayload", GemPayload.class);
        bindings.add("GemPayloadTransfer", GemPayloadTransfer.class);
        bindings.add("CellView", CellView.class);
        bindings.add("FillerResult", FillerResult.class);
        bindings.add("FillerResultSummary", FillerResultSummary.class);
        bindings.add("PlayerStateSnapshot", PlayerStateSnapshot.class);
        if (type == ScriptType.SERVER) bindings.add("AstralRuntime", AstralRuntime.class);
    }
}
