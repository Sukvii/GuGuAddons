package com.gugucraft.guguaddons.stage;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CreateRecipeStageHooks {
    private CreateRecipeStageHooks() {
    }

    public static <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> findUnlocked(
            BlockEntity machine, AllRecipeTypes recipeType, I input, Level level) {
        return recipeType.<I, R>find(input, level)
                .filter(holder -> MachineRecipeStageManager.canProcessIncludingSequenced(machine, holder));
    }

    public static ItemStack tryToApplyMechanicalCraftingRecipe(Level level, RecipeGridHandler.GroupedItems items,
                                                               BlockEntity machine) {
        items.calcStats();
        CraftingInput craftingInput = MechanicalCraftingInput.of(items);
        RegistryAccess registryAccess = level.registryAccess();

        if (AllConfigs.server().recipes.allowRegularCraftingInCrafter.get()) {
            Optional<RecipeHolder<CraftingRecipe>> craftingRecipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftingInput, level)
                    .filter(recipe -> RecipeGridHandler.isRecipeAllowed(recipe, craftingInput));

            if (craftingRecipe.isPresent()) {
                return craftingRecipe.get().value().assemble(craftingInput, registryAccess);
            }
        }

        Optional<RecipeHolder<Recipe<CraftingInput>>> mechanicalRecipe =
                AllRecipeTypes.MECHANICAL_CRAFTING.find(craftingInput, level);
        if (mechanicalRecipe.isPresent()
                && MachineRecipeStageManager.canProcess(machine, mechanicalRecipe.get())) {
            return mechanicalRecipe.get().value().assemble(craftingInput, registryAccess);
        }

        return null;
    }

    public static boolean canItemBeFilled(Level level, ItemStack stack, UUID ownerId) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingRecipe>> sequenced =
                SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.FILLING.getType(), FillingRecipe.class);
        if (sequenced.isPresent()) {
            return MachineRecipeStageManager.canProcessIncludingSequenced(ownerId, sequenced.get());
        }

        Optional<RecipeHolder<Recipe<SingleRecipeInput>>> filling =
                AllRecipeTypes.FILLING.find(input, level);
        if (filling.isPresent()) {
            return MachineRecipeStageManager.canProcess(ownerId, filling.get());
        }

        return GenericItemFilling.canItemBeFilled(level, stack);
    }

    public static boolean canItemBeEmptied(Level level, ItemStack stack, UUID ownerId) {
        if (PotionFluidHandler.isPotionItem(stack)) {
            return true;
        }

        Optional<RecipeHolder<Recipe<SingleRecipeInput>>> emptying =
                AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), level);
        if (emptying.isPresent()) {
            return MachineRecipeStageManager.canProcess(ownerId, emptying.get());
        }

        IFluidHandlerItem capability = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (capability == null) {
            return false;
        }
        for (int i = 0; i < capability.getTanks(); i++) {
            if (capability.getFluidInTank(i).getAmount() > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean canPolish(Level level, ItemStack stack, UUID ownerId) {
        return !getUnlockedPolishingRecipes(level, stack, ownerId).isEmpty();
    }

    public static ItemStack applyPolish(Level level, Vec3 position, ItemStack stack, ItemStack sandPaperStack,
                                        UUID ownerId) {
        List<RecipeHolder<Recipe<SingleRecipeInput>>> matchingRecipes =
                getUnlockedPolishingRecipes(level, stack, ownerId);
        if (matchingRecipes.isEmpty()) {
            return stack;
        }
        return matchingRecipes.getFirst().value()
                .assemble(new SingleRecipeInput(stack), level.registryAccess())
                .copy();
    }

    public static boolean canFanProcess(Level level, ItemStack stack, FanProcessingType type, UUID ownerId) {
        Optional<? extends RecipeHolder<?>> recipe = getFanRecipe(level, stack, type);
        return recipe.map(holder -> MachineRecipeStageManager.canProcess(ownerId, holder)).orElse(true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static List<RecipeHolder<Recipe<SingleRecipeInput>>> getUnlockedPolishingRecipes(Level level,
                                                                                             ItemStack stack,
                                                                                             UUID ownerId) {
        return SandPaperPolishingRecipe.getMatchingRecipes(level, stack).stream()
                .filter(holder -> MachineRecipeStageManager.canProcess(ownerId, holder))
                .toList();
    }

    private static Optional<? extends RecipeHolder<?>> getFanRecipe(Level level, ItemStack stack,
                                                                    FanProcessingType type) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (type == AllFanProcessingTypes.SPLASHING) {
            return AllRecipeTypes.SPLASHING.find(input, level);
        }
        if (type == AllFanProcessingTypes.HAUNTING) {
            return AllRecipeTypes.HAUNTING.find(input, level);
        }
        if (type == AllFanProcessingTypes.SMOKING) {
            return level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, input, level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        }
        if (type == AllFanProcessingTypes.BLASTING) {
            Optional<? extends RecipeHolder<?>> smelting = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            if (smelting.isPresent()) {
                return smelting;
            }
            return level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, input, level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
        }
        return Optional.empty();
    }
}
