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
        List<FluidDemand> cumulativeDemands = new ArrayList<>();
        return plan(handler, ingredients, 0, plannedDrains, cumulativeDemands) ? plannedDrains : null;
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
            List<FluidStack> plannedDrains, List<FluidDemand> cumulativeDemands) {
        if (index >= ingredients.size()) {
            return true;
        }

        SizedFluidIngredient ingredient = ingredients.get(index);
        for (FluidStack candidate : ingredient.getFluids()) {
            FluidStack requestedTotal = candidate.copy();

            int demandIndex = findDemandIndex(cumulativeDemands, candidate);
            if (demandIndex >= 0) {
                requestedTotal = candidate.copyWithAmount(candidate.getAmount() + cumulativeDemands.get(demandIndex).amount);
            }

            FluidStack simulatedDrain = handler.drain(requestedTotal, FluidAction.SIMULATE);
            if (!matchesRequest(requestedTotal, simulatedDrain)) {
                continue;
            }

            plannedDrains.add(candidate.copy());
            if (demandIndex >= 0) {
                cumulativeDemands.get(demandIndex).amount += candidate.getAmount();
            } else {
                cumulativeDemands.add(new FluidDemand(candidate.copy(), candidate.getAmount()));
            }

            if (plan(handler, ingredients, index + 1, plannedDrains, cumulativeDemands)) {
                return true;
            }

            plannedDrains.remove(plannedDrains.size() - 1);
            if (demandIndex >= 0) {
                FluidDemand demand = cumulativeDemands.get(demandIndex);
                demand.amount -= candidate.getAmount();
            } else {
                cumulativeDemands.remove(cumulativeDemands.size() - 1);
            }
        }

        return false;
    }

    private static boolean matchesRequest(FluidStack requested, FluidStack actual) {
        return FluidStack.isSameFluidSameComponents(requested, actual) && actual.getAmount() == requested.getAmount();
    }

    private static int findDemandIndex(List<FluidDemand> cumulativeDemands, FluidStack candidate) {
        for (int index = 0; index < cumulativeDemands.size(); index++) {
            if (FluidStack.isSameFluidSameComponents(cumulativeDemands.get(index).prototype, candidate)) {
                return index;
            }
        }
        return -1;
    }

    private static final class FluidDemand {
        private final FluidStack prototype;
        private int amount;

        private FluidDemand(FluidStack prototype, int amount) {
            this.prototype = prototype;
            this.amount = amount;
        }
    }
}
