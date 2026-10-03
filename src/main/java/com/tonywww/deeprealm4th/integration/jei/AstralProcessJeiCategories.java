package com.tonywww.deeprealm4th.integration.jei;

import com.tonywww.deeprealm4th.platform.recipe.CombinationForgingRecipe;
import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import com.tonywww.deeprealm4th.platform.PlatformIds;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Compact JEI views for all datapack recipes of the two new process types. */
public final class AstralProcessJeiCategories {
    private static final int TEXT_COLOR = 0xFF000000;
    private static final ResourceLocation ICONS = PlatformIds.id("textures/gui/jei_process_icons.png");
    private static final ResourceLocation HAMMER = PlatformIds.id("textures/gui/jei_hammer.png");
    public static final RecipeType<CombinationForgingRecipe> FORGING = RecipeType.create(
            "deeprealm_4th", "combination_forging", CombinationForgingRecipe.class);
    public static final RecipeType<ProjectionRecipe> PROJECTION = RecipeType.create(
            "deeprealm_4th", "projection_combining", ProjectionRecipe.class);

    private AstralProcessJeiCategories() {}

    private static void arrow(GuiGraphics graphics, int x, int y, boolean right) {
        graphics.blit(ICONS, x, y, right ? 0 : 25, 0, 25, 16, 64, 16);
    }

    /** Inventory projection steps use right click in either stacking direction. */
    private static void rightClickMouse(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICONS, x, y, 50, 0, 9, 11, 64, 16);
    }

    public static final class Forging implements IRecipeCategory<CombinationForgingRecipe> {
        private final IDrawable icon;
        public Forging(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(AstralItemCatalog.FORGING_PAD.get()));
        }
        @Override public RecipeType<CombinationForgingRecipe> getRecipeType() { return FORGING; }
        @Override public Component getTitle() { return Component.translatable("jei.deeprealm_4th.forging.title"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 176; }
        @Override public int getHeight() { return 104; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, CombinationForgingRecipe recipe, IFocusGroup focuses) {
            builder.addOutputSlot(146, 31).addItemStack(recipe.output());
            for (int i = 0; i < recipe.inputs().size(); i++) {
                ItemStack display = recipe.inputs().get(i).displayStack();
                if (!display.isEmpty()) {
                    builder.addInputSlot(5 + i % 4 * 18, 14 + i / 4 * 18).addItemStack(display);
                }
            }
        }
        @Override public void draw(CombinationForgingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.blit(HAMMER, 89, 31, 0, 0, 16, 16, 16, 16);
            arrow(graphics, 110, 32, true);
            graphics.drawString(font,
                    Component.translatable("jei.deeprealm_4th.forging.clicks", recipe.clicks()),
                    5, 72, TEXT_COLOR, false);
            graphics.drawString(font,
                    Component.translatable("jei.deeprealm_4th.forging.cooldown", recipe.cooldown()),
                    5, 83, TEXT_COLOR, false);
            graphics.drawString(font,
                    Component.translatable("jei.deeprealm_4th.forging.levels", recipe.levels()),
                    95, 83, TEXT_COLOR, false);
        }
    }

    public static final class Projection implements IRecipeCategory<ProjectionRecipe> {
        private final IDrawable icon;
        public Projection(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(AstralItemCatalog.PROJECTION_FRAME.get()));
        }
        @Override public RecipeType<ProjectionRecipe> getRecipeType() { return PROJECTION; }
        @Override public Component getTitle() { return Component.translatable("jei.deeprealm_4th.projection.title"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 176; }
        @Override public int getHeight() { return 203; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, ProjectionRecipe recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.steps().size(); i++) {
                boolean materialOnFrame = recipe.steps().get(i).direction()
                        == ProjectionRecipe.Direction.MATERIAL_ON_FRAME;
                builder.addInputSlot(materialOnFrame ? 27 : 108, 4 + i * 19)
                        .addItemStack(recipe.steps().get(i).displayStack());
            }
            builder.addOutputSlot(141, 181).addItemStack(recipe.output());
        }
        @Override public void draw(ProjectionRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            for (int i = 0; i < recipe.steps().size(); i++) {
                ProjectionRecipe.Step step = recipe.steps().get(i);
                int y = 4 + i * 19;
                boolean materialOnFrame = step.direction() == ProjectionRecipe.Direction.MATERIAL_ON_FRAME;
                graphics.drawString(font, Integer.toString(i + 1), 6, y + 4, TEXT_COLOR, false);
                graphics.renderItem(new ItemStack(AstralItemCatalog.PROJECTION_FRAME.get()),
                        materialOnFrame ? 108 : 27, y);
                arrow(graphics, 68, y + 2, materialOnFrame);
                rightClickMouse(graphics, 76, y + 3);
            }
            graphics.renderItem(new ItemStack(AstralItemCatalog.PROJECTION_FRAME.get()), 27, 181);
            arrow(graphics, 70, 183, true);
        }
    }
}
