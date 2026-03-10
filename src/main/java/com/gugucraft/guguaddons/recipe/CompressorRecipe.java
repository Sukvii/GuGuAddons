package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

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

    private boolean applyInternal(BasinBlockEntity basin, VacuumChamberBlockEntity chamber, boolean test) {
        IItemHandler availableItems = basin.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(),
                null);
        IFluidHandler availableFluids = basin.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                basin.getBlockPos(), null);
        IFluidHandler availableSecondaryFluids = chamber.getFluidCapability();

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

        for (boolean simulate : Iterate.trueAndFalse) {
            if (!simulate && test) {
                return true;
            }

            int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
            int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];
            int[] extractedSecondaryFluidsFromTank = new int[availableSecondaryFluids.getTanks()];

            Ingredients:
            for (Ingredient ingredient : ingredients) {
                for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                    if (simulate && availableItems.getStackInSlot(slot).getCount() <= extractedItemsFromSlot[slot]) {
                        continue;
                    }
                    ItemStack extracted = availableItems.extractItem(slot, 1, true);
                    if (!ingredient.test(extracted)) {
                        continue;
                    }
                    if (!simulate) {
                        availableItems.extractItem(slot, 1, false);
                    }
                    extractedItemsFromSlot[slot]++;
                    continue Ingredients;
                }
                return false;
            }

            boolean fluidsAffected = false;
            FluidIngredients:
            for (int index = 0; index < fluidIngredients.size(); index++) {
                SizedFluidIngredient fluidIngredient = fluidIngredients.get(index);
                int amountRequired = fluidIngredient.amount();

                if (secondaryFluidInput == index) {
                    for (int tank = 0; tank < availableSecondaryFluids.getTanks(); tank++) {
                        FluidStack fluidStack = availableSecondaryFluids.getFluidInTank(tank);
                        if (simulate && fluidStack.getAmount() <= extractedSecondaryFluidsFromTank[tank]) {
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
                        extractedSecondaryFluidsFromTank[tank] += drainedAmount;
                        continue FluidIngredients;
                    }
                } else {
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
                        continue FluidIngredients;
                    }
                }

                return false;
            }

            if (fluidsAffected) {
                basin.getBehaviour(SmartFluidTankBehaviour.INPUT).forEach(TankSegment::onFluidStackChanged);
                basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT).forEach(TankSegment::onFluidStackChanged);
                chamber.getBehaviour(SmartFluidTankBehaviour.INPUT).forEach(TankSegment::onFluidStackChanged);
                chamber.getBehaviour(SmartFluidTankBehaviour.OUTPUT).forEach(TankSegment::onFluidStackChanged);
            }

            if (simulate) {
                recipeOutputItems.addAll(rollResults(basin.getLevel().random));

                CraftingInput remainderInput = new DummyCraftingContainer(availableItems, extractedItemsFromSlot)
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
            }

            if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)) {
                return false;
            }

            if (secondaryFluidOutput >= 0 && !chamber.acceptOutputs(recipeSecondaryOutputFluids, simulate)) {
                return false;
            }
        }

        return true;
    }
}
