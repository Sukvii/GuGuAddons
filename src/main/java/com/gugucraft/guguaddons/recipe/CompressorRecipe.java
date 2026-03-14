package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class CompressorRecipe extends BasinRecipe {
    private final int secondaryFluidOutput;
    private final int secondaryFluidInput;

    protected CompressorRecipe(IRecipeTypeInfo typeInfo, CompressorRecipeParams params) {
        super(typeInfo, params);
        this.secondaryFluidOutput = params.secondaryFluidOutput;
        this.secondaryFluidInput = params.secondaryFluidInput;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    public boolean matches(BasinBlockEntity basin, VacuumChamberBlockEntity chamber) {
        FilteringBehaviour filter = basin.getFilter();
        if (filter == null) {
            return false;
        }

        boolean filterTest = filter.test(getResultItem(basin.getLevel().registryAccess()));
        if (getRollableResults().isEmpty() && !getFluidResults().isEmpty()) {
            filterTest = filter.test(getFluidResults().getFirst());
        }

        return filterTest && applyInternal(basin, chamber, true);
    }

    public boolean apply(BasinBlockEntity basin, VacuumChamberBlockEntity chamber) {
        return applyInternal(basin, chamber, false);
    }

    public int getSecondaryFluidOutput() {
        return secondaryFluidOutput;
    }

    public int getSecondaryFluidInput() {
        return secondaryFluidInput;
    }

    private boolean applyInternal(BasinBlockEntity basin, VacuumChamberBlockEntity chamber, boolean test) {
        IItemHandler availableItems = basin.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(),
                null);
        IFluidHandler availableFluids = basin.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                basin.getBlockPos(), null);
        IFluidHandler availableSecondaryFluids = chamber.getInputTankCapability();

        if (availableItems == null || availableFluids == null || availableSecondaryFluids == null) {
            return false;
        }

        BlazeBurnerBlock.HeatLevel heat = BasinBlockEntity
                .getHeatLevelOf(basin.getLevel().getBlockState(basin.getBlockPos().below()));
        if (!getRequiredHeat().testBlazeBurner(heat)) {
            return false;
        }

        List<ItemStack> recipeOutputItems = new ArrayList<>();
        List<FluidStack> recipeOutputFluids = new ArrayList<>();
        List<FluidStack> recipeSecondaryOutputFluids = new ArrayList<>();

        List<Ingredient> ingredients = new LinkedList<>(getIngredients());
        List<SizedFluidIngredient> fluidIngredients = new ArrayList<>(getFluidIngredients());

        ItemConsumptionPlan itemPlan = planItemConsumption(availableItems, ingredients);
        if (itemPlan == null) {
            return false;
        }

        List<SizedFluidIngredient> primaryFluidIngredients = new ArrayList<>();
        List<SizedFluidIngredient> secondaryFluidIngredients = new ArrayList<>();
        for (int index = 0; index < fluidIngredients.size(); index++) {
            SizedFluidIngredient fluidIngredient = fluidIngredients.get(index);
            if (secondaryFluidInput == index) {
                secondaryFluidIngredients.add(fluidIngredient);
            } else {
                primaryFluidIngredients.add(fluidIngredient);
            }
        }

        List<FluidStack> primaryDrainPlan = RecipeFluidDrainPlan.plan(availableFluids, primaryFluidIngredients);
        if (primaryDrainPlan == null) {
            return false;
        }

        List<FluidStack> secondaryDrainPlan = RecipeFluidDrainPlan.plan(availableSecondaryFluids, secondaryFluidIngredients);
        if (secondaryDrainPlan == null) {
            return false;
        }

        recipeOutputItems.addAll(rollResults(basin.getLevel().random));

        CraftingInput remainderInput = new DummyCraftingContainer(availableItems, itemPlan.extractedItemsPerSlot())
                .asCraftInput();
        for (ItemStack stack : getRemainingItems(remainderInput)) {
            if (!stack.isEmpty()) {
                recipeOutputItems.add(stack);
            }
        }

        NonNullList<FluidStack> fluidResults = getFluidResults();
        for (int index = 0; index < fluidResults.size(); index++) {
            FluidStack fluidStack = fluidResults.get(index);
            if (fluidStack.isEmpty()) {
                continue;
            }
            if (secondaryFluidOutput == index) {
                recipeSecondaryOutputFluids.add(fluidStack.copy());
            } else {
                recipeOutputFluids.add(fluidStack.copy());
            }
        }

        if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, true)) {
            return false;
        }

        if (secondaryFluidOutput >= 0 && !chamber.acceptOutputs(recipeSecondaryOutputFluids, true)) {
            return false;
        }

        if (test) {
            return true;
        }

        if (!executeItemConsumption(availableItems, itemPlan)) {
            return false;
        }
        if (!RecipeFluidDrainPlan.execute(availableFluids, primaryDrainPlan)) {
            return false;
        }
        if (!RecipeFluidDrainPlan.execute(availableSecondaryFluids, secondaryDrainPlan)) {
            return false;
        }

        if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, false)) {
            return false;
        }

        return secondaryFluidOutput < 0 || chamber.acceptOutputs(recipeSecondaryOutputFluids, false);
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
}
