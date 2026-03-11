package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.recipe.CompressorRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public class CompressorEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 104;
    private static final AnimatedVacuumChamberDisplay VACUUM_DISPLAY = new AnimatedVacuumChamberDisplay();

    private final CompressorRecipe recipe;
    private final boolean pressurizingMode;
    private final AnimatedBlazeBurnerDisplay heater = new AnimatedBlazeBurnerDisplay();

    public CompressorEmiRecipe(EmiRecipeCategory category, ResourceLocation id, CompressorRecipe recipe,
            boolean pressurizingMode) {
        super(category, id, WIDTH, HEIGHT);
        this.recipe = recipe;
        this.pressurizingMode = pressurizingMode;

        for (Ingredient ingredient : recipe.getIngredients()) {
            inputs.add(EmiIngredient.of(ingredient));
        }
        for (SizedFluidIngredient ingredient : recipe.getFluidIngredients()) {
            inputs.add(NeoForgeEmiIngredient.of(ingredient));
        }
        for (ProcessingOutput output : recipe.getRollableResults()) {
            outputs.add(EmiStack.of(output.getStack()).setChance(output.getChance()));
        }
        for (FluidStack output : recipe.getFluidResults()) {
            outputs.add(NeoForgeEmiStack.of(output));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (draw, mouseX, mouseY, delta) -> drawBackground(draw));

        int normalInputCount = recipe.getIngredients().size() + recipe.getFluidIngredients().size();
        if (recipe.getSecondaryFluidInput() >= 0) {
            normalInputCount--;
        }
        int inputOffset = normalInputCount < 3 ? (3 - normalInputCount) * 19 / 2 : 0;

        int inputIndex = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            int x = 17 + inputOffset + (inputIndex % 3) * 19;
            int y = 52 - (inputIndex / 3) * 19;
            widgets.addSlot(EmiIngredient.of(ingredient), x, y);
            inputIndex++;
        }

        int fluidInputIndex = 0;
        for (SizedFluidIngredient ingredient : recipe.getFluidIngredients()) {
            EmiIngredient emiIngredient = NeoForgeEmiIngredient.of(ingredient);
            if (fluidInputIndex == recipe.getSecondaryFluidInput()) {
                widgets.addTank(emiIngredient, 21, 14, 18, 18, Math.max((int) emiIngredient.getAmount(), 1000));
                widgets.addTooltipText(List.of(Component.literal("Secondary Fluid Input")), 21, 14, 18, 18);
            } else {
                int x = 17 + inputOffset + (inputIndex % 3) * 19;
                int y = 52 - (inputIndex / 3) * 19;
                widgets.addTank(emiIngredient, x, y, 18, 18, Math.max((int) emiIngredient.getAmount(), 1000));
                inputIndex++;
            }
            fluidInputIndex++;
        }

        int normalOutputCount = recipe.getRollableResults().size() + recipe.getFluidResults().size();
        if (recipe.getSecondaryFluidOutput() >= 0) {
            normalOutputCount--;
        }

        int outputIndex = 0;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            int x = 142 - (normalOutputCount % 2 != 0 && outputIndex == normalOutputCount - 1 ? 0
                    : outputIndex % 2 == 0 ? 10 : -9);
            int y = 52 - 19 * (outputIndex / 2);
            widgets.addSlot(EmiStack.of(output.getStack()).setChance(output.getChance()), x, y).recipeContext(this);
            outputIndex++;
        }

        int fluidOutputIndex = 0;
        for (FluidStack output : recipe.getFluidResults()) {
            if (fluidOutputIndex == recipe.getSecondaryFluidOutput()) {
                widgets.addTank(NeoForgeEmiStack.of(output), 140, 2, 18, 18, Math.max(output.getAmount(), 1000))
                        .recipeContext(this);
                widgets.addTooltipText(List.of(Component.literal("Secondary Fluid Output")), 140, 2, 18, 18);
            } else {
                int x = 142 - (normalOutputCount % 2 != 0 && outputIndex == normalOutputCount - 1 ? 0
                        : outputIndex % 2 == 0 ? 10 : -9);
                int y = 52 - 19 * (outputIndex / 2);
                widgets.addTank(NeoForgeEmiStack.of(output), x, y, 18, 18, Math.max(output.getAmount(), 1000))
                        .recipeContext(this);
                outputIndex++;
            }
            fluidOutputIndex++;
        }

        HeatCondition requiredHeat = recipe.getRequiredHeat();
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.NONE)) {
            widgets.addSlot(EmiStack.of(com.simibubi.create.AllBlocks.BLAZE_BURNER.get()), 134, 81);
        }
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            widgets.addSlot(EmiStack.of(com.simibubi.create.AllItems.BLAZE_CAKE.get()), 153, 81);
        }

        int duration = Math.max(recipe.getProcessingDuration(), 100);
        widgets.addTooltipText(List.of(Component.literal("Processing Time: " + duration + " t")), 90, 9, 30, 67);
    }

    private void drawBackground(net.minecraft.client.gui.GuiGraphics draw) {
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        boolean noHeat = requiredHeat == HeatCondition.NONE;

        int normalOutputCount = recipe.getRollableResults().size() + recipe.getFluidResults().size();
        if (recipe.getSecondaryFluidOutput() >= 0) {
            normalOutputCount--;
        }
        int outputRows = (1 + normalOutputCount) / 2;

        if (normalOutputCount > 0 && outputRows <= 2) {
            AllGuiTextures.JEI_DOWN_ARROW.render(draw, 136, -19 * (outputRows - 1) + 33);
        }

        AllGuiTextures shadow = noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
        shadow.render(draw, 81, 58 + (noHeat ? 10 : 30));

        AllGuiTextures heatBar = noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR;
        heatBar.render(draw, 4, 80);
        draw.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(requiredHeat.getTranslationKey()), 9, 86,
                requiredHeat.getColor(), false);

        if (recipe.getSecondaryFluidInput() >= 0) {
            AllGuiTextures.JEI_ARROW.render(draw, 45, 18);
        }

        if (requiredHeat != HeatCondition.NONE) {
            heater.withHeat(requiredHeat.visualizeAsBlazeBurner()).draw(draw, WIDTH / 2 + 3, 55);
        }
        VACUUM_DISPLAY.draw(draw, WIDTH / 2 + 3, 34, pressurizingMode);
    }
}
