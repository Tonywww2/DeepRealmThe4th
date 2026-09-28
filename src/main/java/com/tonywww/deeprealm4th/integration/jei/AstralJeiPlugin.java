package com.tonywww.deeprealm4th.integration.jei;

import com.tonywww.deeprealm4th.platform.PlatformIds;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.Minecraft;
import com.tonywww.deeprealm4th.platform.recipe.AstralProcessLookup;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** JEI loads this optional integration only when JEI is installed. */
@JeiPlugin
public final class AstralJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return PlatformIds.id("astral_jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(AstralItemCatalog.BASE_CONTAINER.get()),
                Component.translatable("jei.deeprealm_4th.base_container.layout"),
                Component.translatable("jei.deeprealm_4th.base_container.scores"),
                Component.translatable("jei.deeprealm_4th.base_container.equip"));
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(AstralProcessJeiCategories.FORGING,
                    AstralProcessLookup.forging(level).stream().map(AstralProcessLookup.ForgingEntry::recipe).toList());
            registration.addRecipes(AstralProcessJeiCategories.PROJECTION,
                    AstralProcessLookup.projection(level).stream().map(AstralProcessLookup.ProjectionEntry::recipe).toList());
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new AstralProcessJeiCategories.Forging(gui),
                new AstralProcessJeiCategories.Projection(gui));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AstralItemCatalog.FORGING_PAD.get()),
                AstralProcessJeiCategories.FORGING);
        registration.addRecipeCatalyst(new ItemStack(AstralItemCatalog.PROJECTION_FRAME.get()),
                AstralProcessJeiCategories.PROJECTION);
    }
}
