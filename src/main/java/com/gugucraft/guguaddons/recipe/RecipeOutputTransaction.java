package com.gugucraft.guguaddons.recipe;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public final class RecipeOutputTransaction {
    private RecipeOutputTransaction() {
    }

    public static boolean canAcceptItems(IItemHandler target, List<ItemStack> outputs) {
        ItemStackHandler shadow = null;
        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }
            if (target == null) {
                return false;
            }
            if (shadow == null) {
                shadow = createItemShadow(target);
            }
            if (!ItemHandlerHelper.insertItemStacked(shadow, output.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean canAcceptFluids(IFluidHandler target, List<FluidStack> outputs, boolean enforceVariety) {
        CombinedTankWrapper shadow = null;
        for (FluidStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }
            if (target == null) {
                return false;
            }
            if (shadow == null) {
                shadow = createFluidShadow(target, enforceVariety);
            }
            if (shadow.fill(output.copy(), FluidAction.EXECUTE) != output.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStackHandler createItemShadow(IItemHandler target) {
        ItemStackHandler shadow = new ItemStackHandler(target.getSlots()) {
            @Override
            public int getSlotLimit(int slot) {
                return target.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return target.isItemValid(slot, stack);
            }
        };
        for (int slot = 0; slot < target.getSlots(); slot++) {
            shadow.setStackInSlot(slot, target.getStackInSlot(slot).copy());
        }
        return shadow;
    }

    private static CombinedTankWrapper createFluidShadow(IFluidHandler target, boolean enforceVariety) {
        FluidTank[] shadowTanks = new FluidTank[target.getTanks()];
        for (int tank = 0; tank < target.getTanks(); tank++) {
            int targetTank = tank;
            FluidTank shadow = new FluidTank(target.getTankCapacity(tank)) {
                @Override
                public boolean isFluidValid(FluidStack stack) {
                    return target.isFluidValid(targetTank, stack);
                }
            };
            shadow.setFluid(target.getFluidInTank(tank).copy());
            shadowTanks[tank] = shadow;
        }

        CombinedTankWrapper shadow = new CombinedTankWrapper(shadowTanks);
        return enforceVariety ? shadow.enforceVariety() : shadow;
    }
}
