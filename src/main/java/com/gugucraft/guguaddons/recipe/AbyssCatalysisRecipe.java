package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberBlock;
import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberBlockEntity;
import com.gugucraft.guguaddons.registry.ModItems;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbyssCatalysisRecipe implements Recipe<RecipeInput> {
    private final AbyssCatalysisRecipeParams params;
    private final List<Ingredient> topItemIngredients;
    private final List<SizedFluidIngredient> topFluidIngredients;
    private final List<Ingredient> bottomItemIngredients;
    private final List<SizedFluidIngredient> bottomFluidIngredients;
    private final List<Ingredient> catalystItemIngredients;
    private final List<SizedFluidIngredient> catalystFluidIngredients;
    private final List<ProcessingOutput> rollableResults;
    private final List<FluidStack> fluidResults;

    public AbyssCatalysisRecipe(AbyssCatalysisRecipeParams params) {
        this.params = params;
        this.topItemIngredients = itemIngredients(params.topIngredients());
        this.topFluidIngredients = fluidIngredients(params.topIngredients());
        this.bottomItemIngredients = itemIngredients(params.bottomIngredients());
        this.bottomFluidIngredients = fluidIngredients(params.bottomIngredients());
        this.catalystItemIngredients = itemIngredients(params.catalysts());
        this.catalystFluidIngredients = fluidIngredients(params.catalysts());
        this.rollableResults = itemResults(params.results());
        this.fluidResults = fluidResults(params.results());
    }

    public AbyssCatalysisRecipeParams params() {
        return params;
    }

    public List<AbyssCatalysisRecipeIngredient> getTopIngredients() {
        return params.topIngredients();
    }

    public List<AbyssCatalysisRecipeIngredient> getBottomIngredients() {
        return params.bottomIngredients();
    }

    public List<AbyssCatalysisRecipeIngredient> getCatalysts() {
        return params.catalysts();
    }

    public List<Ingredient> getTopItemIngredients() {
        return topItemIngredients;
    }

    public List<SizedFluidIngredient> getTopFluidIngredients() {
        return topFluidIngredients;
    }

    public List<Ingredient> getBottomItemIngredients() {
        return bottomItemIngredients;
    }

    public List<SizedFluidIngredient> getBottomFluidIngredients() {
        return bottomFluidIngredients;
    }

    public List<Ingredient> getCatalystItemIngredients() {
        return catalystItemIngredients;
    }

    public List<SizedFluidIngredient> getCatalystFluidIngredients() {
        return catalystFluidIngredients;
    }

    public float getCatalystConsumptionChance() {
        return params.chances();
    }

    public float getChances() {
        return params.chances();
    }

    public HeatCondition getRequiredHeat() {
        return params.heatRequirement();
    }

    public List<ProcessingOutput> getRollableResults() {
        return rollableResults;
    }

    public List<FluidStack> getFluidResults() {
        return fluidResults;
    }

    public List<ItemStack> rollItemResults(RandomSource randomSource) {
        List<ItemStack> results = new ArrayList<>();
        for (ProcessingOutput output : rollableResults) {
            ItemStack stack = output.rollOutput(randomSource);
            if (!stack.isEmpty()) {
                results.add(stack);
            }
        }
        return results;
    }

    public boolean processStage(ChamberLayers layers, Stage stage, boolean simulate) {
        if (stage == Stage.SETTLING && (!layers.bottom().canContinueProcessing() || !hasRequiredHeat(layers))) {
            return false;
        }
        AbyssCatalyticChamberBlockEntity input = switch (stage) {
            case SETTLING -> layers.bottom();
            case ACTIVATING -> layers.middle();
            case CONDENSING -> layers.top();
        };
        ItemConsumptionPlan itemPlan = planItemConsumption(input.getInputInventory(), switch (stage) {
            case SETTLING -> bottomItemIngredients;
            case ACTIVATING -> catalystItemIngredients;
            case CONDENSING -> topItemIngredients;
        });
        List<FluidStack> fluidPlan = planFluidConsumption(input.getInputTankCapability(), switch (stage) {
            case SETTLING -> bottomFluidIngredients;
            case ACTIVATING -> catalystFluidIngredients;
            case CONDENSING -> topFluidIngredients;
        });
        if (itemPlan == null || fluidPlan == null) {
            return false;
        }
        if (stage == Stage.ACTIVATING && getCatalystConsumptionChance() == 0) {
            return true;
        }
        List<ItemStack> outputItems = stage == Stage.CONDENSING ? getGuaranteedOutputCapacityItems() : new ArrayList<>();
        addRemainingItems(outputItems, itemPlan);
        List<FluidStack> outputFluids = stage == Stage.CONDENSING ? copyFluidResults(fluidResults) : List.of();
        if (!layers.bottom().acceptOutputs(outputItems, outputFluids, true)) {
            return false;
        }
        if (simulate) {
            return true;
        }
        if (stage == Stage.ACTIVATING && layers.level().random.nextFloat() >= getCatalystConsumptionChance()) {
            return true;
        }
        if (stage == Stage.CONDENSING) {
            outputItems = rollItemResults(layers.level().random);
            addRemainingItems(outputItems, itemPlan);
        }
        if (!executeItemConsumption(input.getInputInventory(), itemPlan)
                || !RecipeFluidDrainPlan.execute(input.getInputTankCapability(), fluidPlan)) {
            return false;
        }
        layers.bottom().acceptOutputs(outputItems, outputFluids, false);
        input.notifyContentsChanged();
        layers.bottom().notifyContentsChanged();
        return true;
    }

    private List<ItemStack> getGuaranteedOutputCapacityItems() {
        List<ItemStack> results = new ArrayList<>();
        for (ProcessingOutput output : rollableResults) {
            ItemStack stack = output.getStack();
            if (!stack.isEmpty()) {
                results.add(stack.copy());
            }
        }
        return results;
    }

    public boolean hasRequiredHeat(ChamberLayers layers) {
        return getRequiredHeat()
                .testBlazeBurner(BasinBlockEntity.getHeatLevelOf(layers.level()
                        .getBlockState(layers.bottom().getBlockPos().below())));
    }

    @Nullable
    public static ChamberLayers getLayers(AbyssCatalyticChamberBlockEntity chamber) {
        Level level = chamber.getLevel();
        if (level == null) {
            return null;
        }

        BlockPos bottomPos = chamber.getBottomPos();
        if (!AbyssCatalyticChamberBlock.isValidChamber(level, bottomPos)
                || !(level.getBlockEntity(bottomPos) instanceof AbyssCatalyticChamberBlockEntity bottom)
                || !(level.getBlockEntity(bottomPos.above()) instanceof AbyssCatalyticChamberBlockEntity middle)
                || !(level.getBlockEntity(bottomPos.above(2)) instanceof AbyssCatalyticChamberBlockEntity top)) {
            return null;
        }
        return new ChamberLayers(level, bottom, middle, top);
    }

    @Nullable
    private static ItemConsumptionPlan planItemConsumption(IItemHandler availableItems, List<Ingredient> ingredients) {
        int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
        List<ItemStack> consumedItems = new ArrayList<>();

        ingredientsLoop:
        for (Ingredient ingredient : ingredients) {
            for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                ItemStack stackInSlot = availableItems.getStackInSlot(slot);
                if (stackInSlot.getCount() <= extractedItemsFromSlot[slot]) {
                    continue;
                }

                ItemStack candidate = stackInSlot.copyWithCount(1);
                if (!ingredient.test(candidate)) {
                    continue;
                }

                extractedItemsFromSlot[slot]++;
                consumedItems.add(candidate);
                continue ingredientsLoop;
            }
            return null;
        }

        return new ItemConsumptionPlan(extractedItemsFromSlot, consumedItems);
    }

    @Nullable
    private static List<FluidStack> planFluidConsumption(@Nullable IFluidHandler handler,
            List<SizedFluidIngredient> ingredients) {
        if (handler == null) {
            return ingredients.isEmpty() ? List.of() : null;
        }
        return RecipeFluidDrainPlan.plan(handler, ingredients);
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

    private static void addRemainingItems(List<ItemStack> outputItems, ItemConsumptionPlan plan) {
        for (ItemStack consumedItem : plan.consumedItems()) {
            if (consumedItem.hasCraftingRemainingItem()) {
                ItemStack remainingItem = consumedItem.getCraftingRemainingItem();
                if (!remainingItem.isEmpty()) {
                    outputItems.add(remainingItem);
                }
            }
        }
    }

    private static List<FluidStack> copyFluidResults(List<FluidStack> fluidResults) {
        List<FluidStack> results = new ArrayList<>();
        for (FluidStack fluidStack : fluidResults) {
            if (!fluidStack.isEmpty()) {
                results.add(fluidStack.copy());
            }
        }
        return results;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return rollableResults.isEmpty() ? ItemStack.EMPTY : rollableResults.get(0).getStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.addAll(topItemIngredients);
        ingredients.addAll(bottomItemIngredients);
        ingredients.addAll(catalystItemIngredients);
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.MECHANICAL_SHRIEKER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ABYSS_CATALYSIS.getSerializer();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ABYSS_CATALYSIS.getType();
    }

    @Override
    public boolean isIncomplete() {
        return getIngredients().stream().anyMatch(Ingredient::hasNoItems);
    }

    private static List<Ingredient> itemIngredients(List<AbyssCatalysisRecipeIngredient> ingredients) {
        List<Ingredient> results = new ArrayList<>();
        for (AbyssCatalysisRecipeIngredient ingredient : ingredients) {
            if (!ingredient.isFluid()) {
                results.add(ingredient.itemIngredient());
            }
        }
        return List.copyOf(results);
    }

    private static List<SizedFluidIngredient> fluidIngredients(List<AbyssCatalysisRecipeIngredient> ingredients) {
        List<SizedFluidIngredient> results = new ArrayList<>();
        for (AbyssCatalysisRecipeIngredient ingredient : ingredients) {
            SizedFluidIngredient fluidIngredient = ingredient.fluidIngredient();
            if (fluidIngredient != null) {
                results.add(fluidIngredient);
            }
        }
        return List.copyOf(results);
    }

    private static List<ProcessingOutput> itemResults(List<AbyssCatalysisRecipeResult> results) {
        List<ProcessingOutput> itemResults = new ArrayList<>();
        for (AbyssCatalysisRecipeResult result : results) {
            if (!result.isFluid()) {
                itemResults.add(result.itemResult());
            }
        }
        return List.copyOf(itemResults);
    }

    private static List<FluidStack> fluidResults(List<AbyssCatalysisRecipeResult> results) {
        List<FluidStack> fluidResults = new ArrayList<>();
        for (AbyssCatalysisRecipeResult result : results) {
            FluidStack fluidStack = result.fluidResult();
            if (fluidStack != null && !fluidStack.isEmpty()) {
                fluidResults.add(fluidStack);
            }
        }
        return List.copyOf(fluidResults);
    }

    public enum Stage {
        SETTLING, ACTIVATING, CONDENSING
    }

    public record ChamberLayers(Level level, AbyssCatalyticChamberBlockEntity bottom,
            AbyssCatalyticChamberBlockEntity middle, AbyssCatalyticChamberBlockEntity top) {
    }

    private record ItemConsumptionPlan(int[] extractedItemsPerSlot, List<ItemStack> consumedItems) {
        private ItemConsumptionPlan {
            extractedItemsPerSlot = Arrays.copyOf(extractedItemsPerSlot, extractedItemsPerSlot.length);
            consumedItems = List.copyOf(consumedItems);
        }
    }
}
