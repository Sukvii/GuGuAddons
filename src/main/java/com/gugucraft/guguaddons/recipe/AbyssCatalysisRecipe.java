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

    public static boolean match(AbyssCatalyticChamberBlockEntity chamber, Recipe<?> recipe) {
        return applyInternal(chamber, recipe, true);
    }

    public static boolean apply(AbyssCatalyticChamberBlockEntity chamber, Recipe<?> recipe) {
        return applyInternal(chamber, recipe, false);
    }

    private static boolean applyInternal(AbyssCatalyticChamberBlockEntity chamber, Recipe<?> recipe, boolean test) {
        if (!(recipe instanceof AbyssCatalysisRecipe abyssRecipe)) {
            return false;
        }

        ChamberLayers layers = getLayers(chamber);
        if (layers == null || !layers.bottom().canContinueProcessing() || !hasRequiredHeat(layers, abyssRecipe)) {
            return false;
        }

        IItemHandler bottomItems = layers.bottom().getItemCapability();
        IFluidHandler bottomFluids = layers.bottom().getFluidCapability();
        ItemConsumptionPlan topItemPlan = planItemConsumption(layers.top().getInputInventory(),
                abyssRecipe.getTopItemIngredients());
        ItemConsumptionPlan bottomItemPlan = planItemConsumption(bottomItems, abyssRecipe.getBottomItemIngredients());
        ItemConsumptionPlan catalystItemPlan = planItemConsumption(layers.middle().getInputInventory(),
                abyssRecipe.getCatalystItemIngredients());
        if (topItemPlan == null || bottomItemPlan == null || catalystItemPlan == null) {
            return false;
        }

        List<FluidStack> topFluidPlan = planFluidConsumption(layers.top().getInputTankCapability(),
                abyssRecipe.getTopFluidIngredients());
        List<FluidStack> bottomFluidPlan = planFluidConsumption(bottomFluids, abyssRecipe.getBottomFluidIngredients());
        List<FluidStack> catalystFluidPlan = planFluidConsumption(layers.middle().getInputTankCapability(),
                abyssRecipe.getCatalystFluidIngredients());
        if (topFluidPlan == null || bottomFluidPlan == null || catalystFluidPlan == null) {
            return false;
        }

        boolean consumeCatalysts = test
                ? abyssRecipe.getCatalystConsumptionChance() > 0F
                : layers.level().random.nextFloat() < abyssRecipe.getCatalystConsumptionChance();
        List<ItemStack> recipeOutputItems = test ? abyssRecipe.getGuaranteedOutputCapacityItems()
                : abyssRecipe.rollItemResults(layers.level().random);
        recipeOutputItems = new ArrayList<>(recipeOutputItems);
        addRemainingItems(recipeOutputItems, topItemPlan);
        addRemainingItems(recipeOutputItems, bottomItemPlan);
        if (consumeCatalysts) {
            addRemainingItems(recipeOutputItems, catalystItemPlan);
        }

        List<FluidStack> recipeOutputFluids = copyFluidResults(abyssRecipe.getFluidResults());
        if (!layers.bottom().acceptOutputs(recipeOutputItems, recipeOutputFluids, true)) {
            return false;
        }

        if (test) {
            return true;
        }

        if (!executeItemConsumption(layers.top().getInputInventory(), topItemPlan)
                || !executeItemConsumption(bottomItems, bottomItemPlan)
                || !RecipeFluidDrainPlan.execute(layers.top().getInputTankCapability(), topFluidPlan)
                || !RecipeFluidDrainPlan.execute(bottomFluids, bottomFluidPlan)) {
            return false;
        }

        if (consumeCatalysts && (!executeItemConsumption(layers.middle().getInputInventory(), catalystItemPlan)
                || !RecipeFluidDrainPlan.execute(layers.middle().getInputTankCapability(), catalystFluidPlan))) {
            return false;
        }

        if (!layers.bottom().acceptOutputs(recipeOutputItems, recipeOutputFluids, false)) {
            return false;
        }

        layers.top().notifyContentsChanged();
        layers.middle().notifyContentsChanged();
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

    private static boolean hasRequiredHeat(ChamberLayers layers, AbyssCatalysisRecipe recipe) {
        return recipe.getRequiredHeat()
                .testBlazeBurner(BasinBlockEntity.getHeatLevelOf(layers.level()
                        .getBlockState(layers.bottom().getBlockPos().below())));
    }

    @Nullable
    private static ChamberLayers getLayers(AbyssCatalyticChamberBlockEntity chamber) {
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

    private record ChamberLayers(Level level, AbyssCatalyticChamberBlockEntity bottom,
            AbyssCatalyticChamberBlockEntity middle, AbyssCatalyticChamberBlockEntity top) {
    }

    private record ItemConsumptionPlan(int[] extractedItemsPerSlot, List<ItemStack> consumedItems) {
        private ItemConsumptionPlan {
            extractedItemsPerSlot = Arrays.copyOf(extractedItemsPerSlot, extractedItemsPerSlot.length);
            consumedItems = List.copyOf(consumedItems);
        }
    }
}
