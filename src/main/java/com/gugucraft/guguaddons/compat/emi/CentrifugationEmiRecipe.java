package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.recipe.CentrifugationRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
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

public class CentrifugationEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 112;
    private static final AnimatedCentrifugeDisplay CENTRIFUGE_DISPLAY = new AnimatedCentrifugeDisplay();

    private final CentrifugationRecipe recipe;

    public CentrifugationEmiRecipe(EmiRecipeCategory category, ResourceLocation id, CentrifugationRecipe recipe) {
        super(category, id, WIDTH, HEIGHT);
        this.recipe = recipe;

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
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (draw, mouseX, mouseY, delta) -> {
            AllGuiTextures.JEI_DOWN_ARROW.render(draw, 70, 6);
            AllGuiTextures.JEI_DOWN_ARROW.render(draw, 134, 36);
            AllGuiTextures.JEI_SHADOW.render(draw, 39, 79);
            CENTRIFUGE_DISPLAY.draw(draw, 56, 66);
            draw.drawCenteredString(Minecraft.getInstance().font,
                    Component.literal(recipe.getMinimalRPM() + " RPM"), 88, 103, 0xFFFF00);
        });

        int inputIndex = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            int x = 10 + inputIndex % 3 * 19;
            int y = 5 + inputIndex / 3 * 19;
            widgets.addSlot(EmiIngredient.of(ingredient), x, y);
            inputIndex++;
        }
        for (SizedFluidIngredient ingredient : recipe.getFluidIngredients()) {
            int x = 10 + inputIndex % 3 * 19;
            int y = 5 + inputIndex / 3 * 19;
            EmiIngredient emiIngredient = NeoForgeEmiIngredient.of(ingredient);
            widgets.addTank(emiIngredient, x, y, 18, 18, Math.max((int) emiIngredient.getAmount(), 1000));
            inputIndex++;
        }

        int outputIndex = 0;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            int x = 128 + outputIndex % 2 * 19;
            int y = 56 + outputIndex / 2 * 19;
            widgets.addSlot(EmiStack.of(output.getStack()).setChance(output.getChance()), x, y)
                    .recipeContext(this);
            outputIndex++;
        }
        for (FluidStack output : recipe.getFluidResults()) {
            int x = 128 + outputIndex % 2 * 19;
            int y = 56 + outputIndex / 2 * 19;
            widgets.addTank(NeoForgeEmiStack.of(output), x, y, 18, 18, Math.max(output.getAmount(), 1000))
                    .recipeContext(this);
            outputIndex++;
        }

        int duration = Math.max(recipe.getProcessingDuration(), 100);
        widgets.addTooltipText(
                java.util.List.of(Component.literal("Processing Time: " + duration + " t")),
                23, 47, 82, 50);
    }
}
