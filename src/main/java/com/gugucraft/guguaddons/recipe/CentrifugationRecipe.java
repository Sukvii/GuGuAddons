package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CentrifugationRecipe extends ProcessingRecipe<RecipeInput, CentrifugationRecipeParams> {
    private final int minimalRPM;

    public CentrifugationRecipe(CentrifugationRecipeParams params) {
        super(ModRecipes.CENTRIFUGATION, params);
        this.minimalRPM = params.minimalRPM;
    }

    public static boolean match(CentrifugeBlockEntity centrifuge, Recipe<?> recipe) {
        return applyInternal(centrifuge, recipe, true);
    }

    public static boolean apply(CentrifugeBlockEntity centrifuge, Recipe<?> recipe) {
        return applyInternal(centrifuge, recipe, false);
    }

    private static boolean applyInternal(CentrifugeBlockEntity centrifuge, Recipe<?> recipe, boolean test) {
        if (!(recipe instanceof CentrifugationRecipe centrifugationRecipe)) {
            return false;
        }

        IItemHandler availableItems = centrifuge.getInputInventory();
        IFluidHandler availableFluids = centrifuge.getInputTankCapability();
        if (availableItems == null || availableFluids == null) {
            return false;
        }

        List<ItemStack> recipeOutputItems = new ArrayList<>();
        List<FluidStack> recipeOutputFluids = new ArrayList<>();
        List<Ingredient> ingredients = new LinkedList<>(centrifugationRecipe.getIngredients());
        List<SizedFluidIngredient> fluidIngredients = new ArrayList<>(centrifugationRecipe.getFluidIngredients());

        for (boolean simulate : Iterate.trueAndFalse) {
            if (!simulate && test) {
                return true;
            }

            int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
            int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];

            ingredientsLoop:
            for (Ingredient ingredient : ingredients) {
                for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                    ItemStack stackInSlot = availableItems.getStackInSlot(slot);
                    if (simulate && stackInSlot.getCount() <= extractedItemsFromSlot[slot]) {
                        continue;
                    }
                    if (!ingredient.test(stackInSlot)) {
                        continue;
                    }
                    if (!simulate) {
                        centrifuge.getInputInventory().extractItem(slot, 1, false);
                    }
                    extractedItemsFromSlot[slot]++;
                    continue ingredientsLoop;
                }
                return false;
            }

            boolean fluidsAffected = false;
            fluidLoop:
            for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
                int amountRequired = fluidIngredient.amount();

                for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                    FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                    if (simulate && fluidStack.getAmount() <= extractedFluidsFromTank[tank]) {
                        continue;
                    }
                    if (!fluidIngredient.test(fluidStack)) {
                        continue;
                    }
                    int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
                    if (!simulate) {
                        fluidStack.shrink(drainedAmount);
                        fluidsAffected = true;
                    }
                    amountRequired -= drainedAmount;
                    if (amountRequired != 0) {
                        continue;
                    }
                    extractedFluidsFromTank[tank] += drainedAmount;
                    continue fluidLoop;
                }
                return false;
            }

            if (fluidsAffected) {
                centrifuge.getBehaviour(SmartFluidTankBehaviour.INPUT).forEach(TankSegment::onFluidStackChanged);
                centrifuge.getBehaviour(SmartFluidTankBehaviour.OUTPUT).forEach(TankSegment::onFluidStackChanged);
            }

            if (simulate) {
                recipeOutputItems.addAll(centrifugationRecipe.rollResults(centrifuge.getLevel().random));
                CraftingInput remainderInput = new DummyCraftingContainer(availableItems, extractedItemsFromSlot)
                        .asCraftInput();
                for (ItemStack stack : centrifugationRecipe.getRemainingItems(remainderInput)) {
                    if (!stack.isEmpty()) {
                        recipeOutputItems.add(stack);
                    }
                }

                NonNullList<FluidStack> fluidResults = centrifugationRecipe.getFluidResults();
                for (FluidStack fluidStack : fluidResults) {
                    if (!fluidStack.isEmpty()) {
                        recipeOutputFluids.add(fluidStack.copy());
                    }
                }
            }

            if (!centrifuge.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 2;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    public int getMinimalRPM() {
        return minimalRPM;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }
}
