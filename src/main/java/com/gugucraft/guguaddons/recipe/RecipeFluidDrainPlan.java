package com.gugucraft.guguaddons.recipe;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class RecipeFluidDrainPlan {
    private RecipeFluidDrainPlan() {
    }

    @Nullable
    static List<FluidStack> plan(IFluidHandler handler, List<SizedFluidIngredient> ingredients) {
        List<FluidStack> plannedDrains = new ArrayList<>();
        List<FluidStack> availableFluids = snapshotFluids(handler);
        return plan(handler, ingredients, 0, plannedDrains, availableFluids) ? plannedDrains : null;
    }

    static boolean execute(IFluidHandler handler, List<FluidStack> plannedDrains) {
        for (FluidStack plannedDrain : plannedDrains) {
            FluidStack drained = handler.drain(plannedDrain.copy(), FluidAction.EXECUTE);
            if (!matchesRequest(plannedDrain, drained)) {
                return false;
            }
        }
        return true;
    }

    private static boolean plan(IFluidHandler handler, List<SizedFluidIngredient> ingredients, int index,
            List<FluidStack> plannedDrains, List<FluidStack> availableFluids) {
        if (index >= ingredients.size()) {
            return true;
        }

        SizedFluidIngredient ingredient = ingredients.get(index);
        for (FluidStack candidate : ingredient.getFluids()) {
            List<FluidStack> candidateDrains = new ArrayList<>();
            List<FluidStack> remainingFluids = copyFluidStacks(availableFluids);
            if (!planCandidateDrain(candidate, remainingFluids, candidateDrains)
                    || !canSimulateDrains(handler, candidateDrains)) {
                continue;
            }

            int previousSize = plannedDrains.size();
            plannedDrains.addAll(candidateDrains);

            if (plan(handler, ingredients, index + 1, plannedDrains, remainingFluids)) {
                return true;
            }

            plannedDrains.subList(previousSize, plannedDrains.size()).clear();
        }

        return false;
    }

    private static List<FluidStack> snapshotFluids(IFluidHandler handler) {
        List<FluidStack> fluids = new ArrayList<>(handler.getTanks());
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            fluids.add(handler.getFluidInTank(tank).copy());
        }
        return fluids;
    }

    private static List<FluidStack> copyFluidStacks(List<FluidStack> stacks) {
        List<FluidStack> copies = new ArrayList<>(stacks.size());
        for (FluidStack stack : stacks) {
            copies.add(stack.copy());
        }
        return copies;
    }

    private static boolean planCandidateDrain(FluidStack candidate, List<FluidStack> availableFluids,
            List<FluidStack> plannedDrains) {
        int amountRequired = candidate.getAmount();
        for (FluidStack availableFluid : availableFluids) {
            if (availableFluid.isEmpty() || !FluidStack.isSameFluidSameComponents(availableFluid, candidate)) {
                continue;
            }

            int drainedAmount = Math.min(amountRequired, availableFluid.getAmount());
            plannedDrains.add(candidate.copyWithAmount(drainedAmount));
            availableFluid.shrink(drainedAmount);
            amountRequired -= drainedAmount;
            if (amountRequired == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSimulateDrains(IFluidHandler handler, List<FluidStack> plannedDrains) {
        for (FluidStack plannedDrain : plannedDrains) {
            FluidStack simulatedDrain = handler.drain(plannedDrain.copy(), FluidAction.SIMULATE);
            if (!matchesRequest(plannedDrain, simulatedDrain)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesRequest(FluidStack requested, FluidStack actual) {
        return FluidStack.isSameFluidSameComponents(requested, actual) && actual.getAmount() == requested.getAmount();
    }

}
