package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.recipe.CompressorRecipe;
import com.gugucraft.guguaddons.recipe.PressurizingRecipe;
import com.gugucraft.guguaddons.recipe.VacuumizingRecipe;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.foundation.recipe.trie.AbstractVariant;
import com.simibubi.create.foundation.recipe.trie.RecipeTrie;
import com.simibubi.create.foundation.recipe.trie.RecipeTrieFinder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VacuumChamberBlockEntity extends BasinOperatingBlockEntity {
    private static final Object RECIPE_CACHE_KEY = new Object();

    public int runningTicks;
    public int processingTicks = -1;
    public boolean running;

    private SmartFluidTankBehaviour outputTank;
    private SmartFluidTankBehaviour inputTank;
    private IFluidHandler fluidCapability;
    private boolean pressurizingMode;

    public VacuumChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUM_CHAMBER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 1000, true)
                .whenFluidUpdates(() -> basinChecker.scheduleUpdate());
        outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, 1000, true)
                .whenFluidUpdates(() -> basinChecker.scheduleUpdate())
                .forbidInsertion();
        behaviours.add(inputTank);
        behaviours.add(outputTank);

        fluidCapability = new CombinedTankWrapper(outputTank.getCapability(), inputTank.getCapability());
    }

    @Override
    public void tick() {
        super.tick();

        if (runningTicks >= 40) {
            running = false;
            runningTicks = 0;
            processingTicks = -1;
            basinChecker.scheduleUpdate();
            return;
        }

        float speed = Math.abs(getSpeed());
        if (!running || level == null) {
            return;
        }

        if (level.isClientSide && runningTicks == 20) {
            renderParticles();
        }

        if ((!level.isClientSide || isVirtual()) && runningTicks == 20) {
            if (processingTicks < 0) {
                float recipeSpeed = 1;
                if (currentRecipe instanceof StandardProcessingRecipe<?> processingRecipe) {
                    int duration = processingRecipe.getProcessingDuration();
                    if (duration != 0) {
                        recipeSpeed = duration / 100f;
                    }
                }

                processingTicks = Mth.clamp((Mth.log2((int) (512 / speed))) * Mth.ceil(recipeSpeed * 15) + 1, 1, 512);

                getBasin().ifPresent(basin -> {
                    Couple<SmartFluidTankBehaviour> tanks = basin.getTanks();
                    if (!tanks.getFirst().isEmpty() || !tanks.getSecond().isEmpty()) {
                        level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                                SoundSource.BLOCKS, .75f, speed < 65 ? .75f : 1.5f);
                    }
                });
            } else {
                processingTicks--;
                if (processingTicks == 0) {
                    runningTicks++;
                    processingTicks = -1;
                    applyBasinRecipe();
                    sendData();
                }
            }
        }

        if (runningTicks != 20) {
            runningTicks++;
        }
    }

    @Override
    protected void applyBasinRecipe() {
        if (currentRecipe == null) {
            return;
        }
        if (!MachineRecipeStageManager.canProcess(this, currentRecipe)) {
            return;
        }

        Optional<BasinBlockEntity> optionalBasin = getBasin();
        if (optionalBasin.isEmpty()) {
            return;
        }

        BasinBlockEntity basin = optionalBasin.get();
        boolean wasEmpty = basin.canContinueProcessing();
        boolean applied = false;

        if (!pressurizingMode && currentRecipe instanceof VacuumizingRecipe vacuumizingRecipe) {
            applied = vacuumizingRecipe.apply(basin, this);
        } else if (pressurizingMode && currentRecipe instanceof PressurizingRecipe pressurizingRecipe) {
            applied = pressurizingRecipe.apply(basin, this);
        } else if (!(currentRecipe instanceof CompressorRecipe)) {
            applied = BasinRecipe.apply(basin, currentRecipe);
        }

        if (!applied) {
            return;
        }

        basin.inputTank.sendDataImmediately();
        if (wasEmpty && matchBasinRecipe(currentRecipe)) {
            continueWithPreviousRecipe();
            sendData();
        }

        basin.notifyChangeOfContents();
    }

    @Override
    protected List<Recipe<?>> getMatchingRecipes() {
        List<Recipe<?>> matchingRecipes = new ArrayList<>();
        Optional<BasinBlockEntity> optionalBasin = getBasin();
        if (optionalBasin.isEmpty()) {
            return matchingRecipes;
        }

        BasinBlockEntity basin = optionalBasin.get();
        if (basin.isEmpty()) {
            return matchingRecipes;
        }

        try {
            IItemHandler availableItems = level.getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), null);
            IFluidHandler availableBasinFluids = level.getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(),
                    null);
            IFluidHandler availableChamberFluids = getInputTankCapability();
            IFluidHandler availableFluids = new CombinedTankWrapper(availableBasinFluids, availableChamberFluids);

            if (availableItems == null && availableBasinFluids == null && availableChamberFluids == null) {
                return matchingRecipes;
            }

            RecipeTrie<?> trie = RecipeTrieFinder.get(getRecipeCacheKey(), level, this::matchStaticFilters);
            Set<AbstractVariant> availableVariants = RecipeTrie.getVariants(availableItems, availableFluids);

            for (Recipe<?> recipe : trie.lookup(availableVariants)) {
                if (matchBasinRecipe(recipe) && MachineRecipeStageManager.canProcess(this, recipe)) {
                    matchingRecipes.add(recipe);
                }
            }
        } catch (Exception ignored) {
            matchingRecipes.clear();
            for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(getRecipeCacheKey(), level,
                    this::matchStaticFilters)) {
                if (matchBasinRecipe(holder.value()) && MachineRecipeStageManager.canProcess(this, holder)) {
                    matchingRecipes.add(holder.value());
                }
            }
        }

        matchingRecipes.sort((first, second) -> recipeWeight(second) - recipeWeight(first));
        return matchingRecipes;
    }

    private int recipeWeight(Recipe<?> recipe) {
        int weight = recipe.getIngredients().size();
        if (recipe instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?> processingRecipe) {
            weight += processingRecipe.getFluidIngredients().size();
        }
        return weight;
    }

    @Override
    protected <I extends net.minecraft.world.item.crafting.RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
        if (recipe == null) {
            return false;
        }

        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty()) {
            return false;
        }

        if (recipe instanceof VacuumizingRecipe vacuumizingRecipe) {
            return !pressurizingMode && vacuumizingRecipe.matches(basin.get(), this);
        }
        if (recipe instanceof PressurizingRecipe pressurizingRecipe) {
            return pressurizingMode && pressurizingRecipe.matches(basin.get(), this);
        }

        return BasinRecipe.match(basin.get(), recipe);
    }

    public boolean acceptOutputs(List<FluidStack> outputFluids, boolean simulate) {
        outputTank.allowInsertion();
        boolean accepted = acceptOutputsInner(outputFluids, simulate);
        outputTank.forbidInsertion();
        return accepted;
    }

    private boolean acceptOutputsInner(List<FluidStack> outputFluids, boolean simulate) {
        IFluidHandler targetTank = outputTank.getCapability();
        if (outputFluids.isEmpty()) {
            return true;
        }
        if (targetTank == null) {
            return false;
        }

        for (FluidStack fluidStack : outputFluids) {
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            int fill = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler
                    ? internalHandler.forceFill(fluidStack.copy(), action)
                    : targetTank.fill(fluidStack.copy(), action);
            if (fill != fluidStack.getAmount()) {
                return false;
            }
        }
        return true;
    }

    public boolean changeMode() {
        pressurizingMode = !pressurizingMode;
        basinChecker.scheduleUpdate();
        sendData();
        return pressurizingMode;
    }

    public boolean canChangeMode() {
        return runningTicks == 0;
    }

    public boolean isPressurizingMode() {
        return pressurizingMode;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IFluidHandler getInputTankCapability() {
        return inputTank == null ? null : inputTank.getCapability();
    }

    public ItemInteractionResult handleItemUse(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, this)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, this)) {
            return ItemInteractionResult.SUCCESS;
        }

        if (GenericItemEmptying.canItemBeEmptied(level, stack) || GenericItemFilling.canItemBeFilled(level, stack)) {
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public float getRenderedHeadOffset(float partialTicks) {
        int localTick;
        float offset = 0;
        if (running) {
            if (runningTicks < 20) {
                localTick = runningTicks;
                float progress = (localTick + partialTicks) / 20f;
                progress = (2 - Mth.cos(progress * (float) Math.PI)) / 2;
                offset = Mth.clamp(progress - .5f, 0, 10 / 16f);
            } else if (runningTicks <= 20) {
                offset = 10 / 16f;
            } else {
                localTick = 40 - runningTicks;
                float progress = (localTick - partialTicks) / 20f;
                progress = (2 - Mth.cos(progress * (float) Math.PI)) / 2;
                offset = Mth.clamp(progress - .5f, 0, 10 / 16f);
            }
        }
        return offset + 7 / 16f;
    }

    @Override
    public float calculateStressApplied() {
        return 4.0f;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, -1.5, 0);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        running = tag.getBoolean("Running");
        runningTicks = tag.getInt("Ticks");
        processingTicks = tag.getInt("ProcessingTicks");
        pressurizingMode = tag.getBoolean("PressurizingMode");
        super.read(tag, registries, clientPacket);

        if (clientPacket && hasLevel()) {
            getBasin().ifPresent(basin -> basin.setAreFluidsMoving(running && runningTicks <= 20));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("Running", running);
        tag.putInt("Ticks", runningTicks);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.putBoolean("PressurizingMode", pressurizingMode);
        super.write(tag, registries, clientPacket);
    }

    @Override
    public void startProcessingBasin() {
        if (running && runningTicks <= 20) {
            return;
        }
        super.startProcessingBasin();
        running = true;
        runningTicks = 0;
        processingTicks = -1;
    }

    @Override
    public boolean continueWithPreviousRecipe() {
        runningTicks = 20;
        return true;
    }

    @Override
    protected void onBasinRemoved() {
        if (!running) {
            return;
        }
        runningTicks = 40;
        running = false;
        processingTicks = -1;
    }

    @Override
    protected Object getRecipeCacheKey() {
        return RECIPE_CACHE_KEY;
    }

    @Override
    protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipeHolder) {
        Recipe<?> recipe = recipeHolder.value();
        return recipe.getType() == ModRecipes.VACUUMIZING.getType()
                || recipe.getType() == ModRecipes.PRESSURIZING.getType();
    }

    @Override
    protected boolean isRunning() {
        return running;
    }

    public void renderParticles() {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty() || level == null) {
            return;
        }
        if (!level.getBlockState(getBlockPos().above()).is(Blocks.AIR)) {
            return;
        }
        if (Math.abs(getSpeed()) < IRotate.SpeedLevel.MEDIUM.getSpeedValue()) {
            return;
        }

        float angle = level.random.nextFloat() * 360;
        Vec3 offset = new Vec3(0, 0, 0.25f);
        offset = VecHelper.rotate(offset, angle, Direction.Axis.Y);
        Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Direction.Axis.Y).add(0, .25f, 0);
        Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
        target = VecHelper.offsetRandomly(target.subtract(offset), level.random, 1 / 128f);

        if (pressurizingMode) {
            level.addParticle(ParticleTypes.CLOUD, center.x + target.x * 10, center.y + 0.5f + target.y * 10,
                    center.z + target.z * 10, -target.x * 0.6, -target.y * 0.6, -target.z * 0.6);
        } else {
            level.addParticle(ParticleTypes.CLOUD, center.x, center.y + 0.5f, center.z, target.x, target.y, target.z);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        if (runningTicks == 25) {
            AllSoundEvents.STEAM.playAt(level, worldPosition, 0.75f, 1, true);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        Component modeText = Component.translatable(
                pressurizingMode ? "tooltip.guguaddons.vacuum_chamber.mode.pressurizing"
                        : "tooltip.guguaddons.vacuum_chamber.mode.vacuumizing")
                .withStyle(pressurizingMode ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_AQUA);
        CreateLang.builder()
                .add(Component.translatable("tooltip.guguaddons.vacuum_chamber.mode", modeText))
                .forGoggles(tooltip);

        IFluidHandler fluids = fluidCapability;
        if (fluids == null) {
            return true;
        }

        LangBuilder millibuckets = CreateLang.translate("generic.unit.millibuckets");
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluidStack = fluids.getFluidInTank(tank);
            if (fluidStack.isEmpty()) {
                continue;
            }
            CreateLang.text("")
                    .add(CreateLang.fluidName(fluidStack)
                            .add(CreateLang.text(" "))
                            .style(ChatFormatting.GRAY)
                            .add(CreateLang.number(fluidStack.getAmount())
                                    .add(millibuckets)
                                    .style(ChatFormatting.BLUE)))
                    .forGoggles(tooltip, 1);
        }

        return true;
    }
}
