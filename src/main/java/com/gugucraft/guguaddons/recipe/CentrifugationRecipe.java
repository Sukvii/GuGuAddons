package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
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
import org.jetbrains.annotations.Nullable;

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

        ItemConsumptionPlan itemPlan = planItemConsumption(availableItems, ingredients);
        if (itemPlan == null) {
            return false;
        }

        List<FluidStack> fluidDrainPlan = RecipeFluidDrainPlan.plan(availableFluids, fluidIngredients);
        if (fluidDrainPlan == null) {
            return false;
        }

        recipeOutputItems.addAll(centrifugationRecipe.rollResults(centrifuge.getLevel().random));
        CraftingInput remainderInput = new DummyCraftingContainer(availableItems, itemPlan.extractedItemsPerSlot())
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

        if (!centrifuge.acceptOutputs(recipeOutputItems, recipeOutputFluids, true)) {
            return false;
        }

        if (test) {
            return true;
        }

        if (!executeItemConsumption(availableItems, itemPlan)) {
            return false;
        }
        if (!RecipeFluidDrainPlan.execute(availableFluids, fluidDrainPlan)) {
            return false;
        }

        return centrifuge.acceptOutputs(recipeOutputItems, recipeOutputFluids, false);
    }

    @Nullable
    private static ItemConsumptionPlan planItemConsumption(IItemHandler availableItems, List<Ingredient> ingredients) {
        int[] extractedItemsFromSlot = new int[availableItems.getSlots()];

        ingredientsLoop:
        for (Ingredient ingredient : ingredients) {
            for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                ItemStack stackInSlot = availableItems.getStackInSlot(slot);
                if (stackInSlot.getCount() <= extractedItemsFromSlot[slot]) {
                    continue;
                }
                ItemStack extracted = availableItems.extractItem(slot, 1, true);
                if (!ingredient.test(extracted)) {
                    continue;
                }
                extractedItemsFromSlot[slot]++;
                continue ingredientsLoop;
            }
            return null;
        }

        return new ItemConsumptionPlan(extractedItemsFromSlot);
    }

    private static boolean executeItemConsumption(IItemHandler availableItems, ItemConsumptionPlan plan) {
        int[] extractedItemsPerSlot = plan.extractedItemsPerSlot();
        for (int slot = 0; slot < extractedItemsPerSlot.length; slot++) {
            for (int count = 0; count < extractedItemsPerSlot[slot]; count++) {
                if (availableItems.extractItem(slot, 1, false).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private record ItemConsumptionPlan(int[] extractedItemsPerSlot) {
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
