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
        List<FluidStack> attemptedCandidates = new ArrayList<>();
        for (FluidStack availableFluid : availableFluids) {
            FluidStack candidate = matchingCandidate(ingredient, availableFluid);
            if (candidate == null || wasCandidateAttempted(attemptedCandidates, candidate)) {
                continue;
            }
            attemptedCandidates.add(candidate);

            int previousSize = plannedDrains.size();
            List<FluidAmountChange> changes = new ArrayList<>();

            if (planCandidateDrain(candidate, availableFluids, plannedDrains, changes)
                    && canSimulateDrains(handler, plannedDrains, previousSize)
                    && plan(handler, ingredients, index + 1, plannedDrains, availableFluids)) {
                return true;
            }

            rollbackChanges(availableFluids, changes);
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

    @Nullable
    private static FluidStack matchingCandidate(SizedFluidIngredient ingredient, FluidStack availableFluid) {
        if (availableFluid.isEmpty() || !ingredient.ingredient().test(availableFluid)) {
            return null;
        }

        for (FluidStack candidate : ingredient.getFluids()) {
            if (FluidStack.isSameFluidSameComponents(availableFluid, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean wasCandidateAttempted(List<FluidStack> attemptedCandidates, FluidStack candidate) {
        for (FluidStack attemptedCandidate : attemptedCandidates) {
            if (FluidStack.isSameFluidSameComponents(attemptedCandidate, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean planCandidateDrain(FluidStack candidate, List<FluidStack> availableFluids,
            List<FluidStack> plannedDrains, List<FluidAmountChange> changes) {
        int amountRequired = candidate.getAmount();
        for (int tank = 0; tank < availableFluids.size(); tank++) {
            FluidStack availableFluid = availableFluids.get(tank);
            if (availableFluid.isEmpty() || !FluidStack.isSameFluidSameComponents(availableFluid, candidate)) {
                continue;
            }

            int drainedAmount = Math.min(amountRequired, availableFluid.getAmount());
            plannedDrains.add(candidate.copyWithAmount(drainedAmount));
            changes.add(new FluidAmountChange(tank, drainedAmount));
            availableFluid.shrink(drainedAmount);
            amountRequired -= drainedAmount;
            if (amountRequired == 0) {
                return true;
            }
        }
        return false;
    }

    private static void rollbackChanges(List<FluidStack> availableFluids, List<FluidAmountChange> changes) {
        for (FluidAmountChange change : changes) {
            availableFluids.get(change.tank()).grow(change.amount());
        }
    }

    private static boolean canSimulateDrains(IFluidHandler handler, List<FluidStack> plannedDrains, int startIndex) {
        for (int index = startIndex; index < plannedDrains.size(); index++) {
            FluidStack plannedDrain = plannedDrains.get(index);
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

    private record FluidAmountChange(int tank, int amount) {
    }

}
