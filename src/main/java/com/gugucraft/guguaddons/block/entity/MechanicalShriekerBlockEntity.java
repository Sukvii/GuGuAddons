package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberBlock;
import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.particle.MechanicalShriekerParticleOptions;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MechanicalShriekerBlockEntity extends KineticBlockEntity {
    private static final int RUNNING_TICKS = 40;
    private static final int PROCESSING_START_TICK = 20;
    private static final int DEFAULT_PROCESSING_DURATION = 100;

    public int runningTicks;
    public int processingTicks = -1;
    public int renderingTicks;
    public boolean running;

    private boolean chamberContentsChanged = true;
    private RecipeHolder<AbyssCatalysisRecipe> activeRecipe;
    private ResourceLocation activeRecipeId;
    private ChamberContentSignature cachedCandidateSignature;
    private RecipeManager cachedCandidateRecipeManager;
    private List<RecipeHolder<AbyssCatalysisRecipe>> cachedRecipeCandidates = List.of();

    public MechanicalShriekerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MECHANICAL_SHRIEKER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (runningTicks >= RUNNING_TICKS) {
            running = false;
            runningTicks = 0;
            processingTicks = -1;
            renderingTicks = 0;
            setShrieking(false);
            chamberContentsChanged = true;
            clearActiveRecipe();
            sendData();
            return;
        }

        Optional<AbyssCatalyticChamberBlockEntity> chamber = getChamber();
        if (chamber.isEmpty()) {
            onChamberRemoved();
            return;
        }

        float speed = Math.abs(getSpeed());
        if (speed <= 0 || !isSpeedRequirementFulfilled()) {
            if (running) {
                onChamberRemoved();
                sendData();
            } else {
                setShrieking(false);
            }
            return;
        }

        if (!running && !level.isClientSide && speed > 0
                && (chamberContentsChanged || level.getGameTime() % 20 == 0)) {
            boolean forceCandidateRefresh = chamberContentsChanged;
            chamberContentsChanged = false;
            if (tryStartProcessingChamber(chamber.get(), forceCandidateRefresh)) {
                startProcessingChamber();
            }
        }

        if (!running) {
            setShrieking(false);
            return;
        }

        setShrieking(true);

        if (level.isClientSide && renderingTicks == PROCESSING_START_TICK) {
            renderParticles(getBlockState().getValue(MechanicalShriekerBlock.FACING));
            renderingTicks = 0;
        }

        if ((!level.isClientSide || isVirtual()) && runningTicks == PROCESSING_START_TICK) {
            if (processingTicks < 0) {
                processingTicks = calculateProcessingTicks(speed);
                level.playSound(null, worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0f,
                        speed < 65 ? .75f : 1.5f);
            } else {
                processingTicks--;
                if (processingTicks == 0) {
                    runningTicks++;
                    processingTicks = -1;
                    applyChamberRecipe(chamber.get());
                    sendData();
                }
            }
        }

        if (runningTicks != PROCESSING_START_TICK) {
            runningTicks++;
        }
        if (renderingTicks != PROCESSING_START_TICK) {
            renderingTicks++;
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        chamberContentsChanged = true;
        if (level != null && !level.isClientSide && !isSpeedRequirementFulfilled() && running) {
            onChamberRemoved();
            sendData();
        }
    }

    public void onChamberContentsChanged() {
        chamberContentsChanged = true;
        invalidateRecipeCandidateCache();
        setChanged();
        sendData();
    }

    public boolean consumeChamberContentsChanged() {
        boolean changed = chamberContentsChanged;
        chamberContentsChanged = false;
        return changed;
    }

    public Optional<AbyssCatalyticChamberBlockEntity> getChamber() {
        if (level == null) {
            return Optional.empty();
        }

        BlockPos chamberProbePos = worldPosition.below(2);
        BlockEntity blockEntity = level.getBlockEntity(chamberProbePos);
        if (!(blockEntity instanceof AbyssCatalyticChamberBlockEntity chamber)) {
            return Optional.empty();
        }

        BlockPos bottomPos = chamber.getBottomPos();
        if (!AbyssCatalyticChamberBlock.isValidChamber(level, bottomPos)) {
            return Optional.empty();
        }
        return Optional.of(chamber);
    }

    public void startProcessingChamber() {
        if (running && runningTicks <= PROCESSING_START_TICK) {
            return;
        }

        running = true;
        runningTicks = 0;
        renderingTicks = 0;
        processingTicks = -1;
        setShrieking(true);
        sendData();
    }

    public boolean continueWithPreviousRecipe() {
        runningTicks = PROCESSING_START_TICK;
        renderingTicks = PROCESSING_START_TICK;
        return true;
    }

    protected boolean isRunning() {
        return running;
    }

    protected boolean tryStartProcessingChamber(AbyssCatalyticChamberBlockEntity chamber) {
        return tryStartProcessingChamber(chamber, chamberContentsChanged);
    }

    protected boolean tryStartProcessingChamber(AbyssCatalyticChamberBlockEntity chamber,
            boolean forceCandidateRefresh) {
        RecipeHolder<AbyssCatalysisRecipe> matchingRecipe = findMatchingRecipe(chamber, forceCandidateRefresh);
        setActiveRecipe(matchingRecipe);
        return matchingRecipe != null;
    }

    protected void applyChamberRecipe(AbyssCatalyticChamberBlockEntity chamber) {
        RecipeHolder<AbyssCatalysisRecipe> recipe = resolveActiveRecipe();
        if (recipe == null || !MachineRecipeStageManager.canProcess(this, recipe)
                || !AbyssCatalysisRecipe.apply(chamber, recipe.value())) {
            clearActiveRecipe();
            chamberContentsChanged = true;
            setChanged();
            return;
        }

        if (AbyssCatalysisRecipe.match(chamber, recipe.value())
                && MachineRecipeStageManager.canProcess(this, recipe)) {
            continueWithPreviousRecipe();
        } else {
            clearActiveRecipe();
            chamberContentsChanged = true;
        }

        setChanged();
    }

    protected void onChamberRemoved() {
        clearActiveRecipe();
        if (!running) {
            return;
        }

        runningTicks = RUNNING_TICKS;
        running = false;
        processingTicks = -1;
        renderingTicks = 0;
        setShrieking(false);
        sendData();
    }

    private RecipeHolder<AbyssCatalysisRecipe> findMatchingRecipe(AbyssCatalyticChamberBlockEntity chamber,
            boolean forceCandidateRefresh) {
        if (level == null) {
            return null;
        }

        ChamberContentSignature signature = createChamberContentSignature(chamber);
        if (signature == null) {
            invalidateRecipeCandidateCache();
            return null;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        if (forceCandidateRefresh || !signature.equals(cachedCandidateSignature)
                || recipeManager != cachedCandidateRecipeManager) {
            rebuildRecipeCandidates(chamber, signature, recipeManager);
        }

        RecipeHolder<AbyssCatalysisRecipe> bestMatch = null;
        int bestWeight = -1;
        for (RecipeHolder<AbyssCatalysisRecipe> holder : cachedRecipeCandidates) {
            AbyssCatalysisRecipe recipe = holder.value();
            int weight = recipeWeight(recipe);
            if (weight <= bestWeight) {
                continue;
            }
            if (MachineRecipeStageManager.canProcess(this, holder)) {
                bestMatch = holder;
                bestWeight = weight;
            }
        }

        return bestMatch;
    }

    private void rebuildRecipeCandidates(AbyssCatalyticChamberBlockEntity chamber, ChamberContentSignature signature,
            RecipeManager recipeManager) {
        List<RecipeHolder<AbyssCatalysisRecipe>> candidates = new ArrayList<>();
        for (RecipeHolder<AbyssCatalysisRecipe> holder : getAbyssCatalysisRecipes()) {
            if (AbyssCatalysisRecipe.match(chamber, holder.value())) {
                candidates.add(holder);
            }
        }

        cachedCandidateSignature = signature;
        cachedCandidateRecipeManager = recipeManager;
        cachedRecipeCandidates = List.copyOf(candidates);
    }

    private ChamberContentSignature createChamberContentSignature(AbyssCatalyticChamberBlockEntity chamber) {
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

        return new ChamberContentSignature(bottom.getBlockPos(), middle.getBlockPos(), top.getBlockPos(),
                bottom.getContentsVersion(), middle.getContentsVersion(), top.getContentsVersion(),
                level.getBlockState(bottomPos.below()));
    }

    private void invalidateRecipeCandidateCache() {
        cachedCandidateSignature = null;
        cachedCandidateRecipeManager = null;
        cachedRecipeCandidates = List.of();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<RecipeHolder<AbyssCatalysisRecipe>> getAbyssCatalysisRecipes() {
        return (List) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) ModRecipes.ABYSS_CATALYSIS.getType());
    }

    private RecipeHolder<AbyssCatalysisRecipe> resolveActiveRecipe() {
        if (activeRecipe != null) {
            return activeRecipe;
        }
        if (level == null || activeRecipeId == null) {
            return null;
        }

        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(activeRecipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof AbyssCatalysisRecipe)) {
            return null;
        }

        activeRecipe = castRecipeHolder(holder.get());
        return activeRecipe;
    }

    private void setActiveRecipe(RecipeHolder<AbyssCatalysisRecipe> recipe) {
        activeRecipe = recipe;
        activeRecipeId = recipe == null ? null : recipe.id();
    }

    private void clearActiveRecipe() {
        activeRecipe = null;
        activeRecipeId = null;
        invalidateRecipeCandidateCache();
    }

    @SuppressWarnings("unchecked")
    private RecipeHolder<AbyssCatalysisRecipe> castRecipeHolder(RecipeHolder<?> holder) {
        return (RecipeHolder<AbyssCatalysisRecipe>) holder;
    }

    private int recipeWeight(AbyssCatalysisRecipe recipe) {
        return recipe.getTopItemIngredients().size()
                + recipe.getBottomItemIngredients().size()
                + recipe.getCatalystItemIngredients().size()
                + recipe.getTopFluidIngredients().size()
                + recipe.getBottomFluidIngredients().size()
                + recipe.getCatalystFluidIngredients().size();
    }

    private int calculateProcessingTicks(float speed) {
        if (speed <= 0) {
            return 1;
        }

        float recipeSpeed = DEFAULT_PROCESSING_DURATION / 100f;
        int speedRatio = Math.max(1, (int) (512 / speed));
        return Mth.clamp(Mth.log2(speedRatio) * Mth.ceil(recipeSpeed * 15) + 1, 1, 512);
    }

    private void setShrieking(boolean shrieking) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(MechanicalShriekerBlock.SHRIEKING)
                || state.getValue(MechanicalShriekerBlock.SHRIEKING) == shrieking) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(MechanicalShriekerBlock.SHRIEKING, shrieking), 2);
    }

    private void renderParticles(Direction direction) {
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        for (int i = 0; i < 4; i++) {
            level.addParticle(new MechanicalShriekerParticleOptions(i * 5, direction), false, center.x, center.y,
                    center.z, 0, 0, 0);
        }

        center = center.add(0, -3, 0);
        RandomSource random = level.random;
        for (int i = 0; i < 10; i++) {
            Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f)
                    .multiply(1, .25d, 1)
                    .normalize();
            Vec3 particlePos = center.add(motion.scale(.5d + random.nextDouble() * .125d))
                    .add(0, .125d, 0);
            Vec3 particleMotion = motion.scale(.03125d);
            level.addParticle(ParticleTypes.SONIC_BOOM, particlePos.x, particlePos.y, particlePos.z,
                    particleMotion.x, particleMotion.y, particleMotion.z);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        running = tag.getBoolean("Running");
        runningTicks = tag.getInt("Ticks");
        processingTicks = tag.getInt("ProcessingTicks");
        renderingTicks = tag.getInt("RenderingTicks");
        chamberContentsChanged = tag.getBoolean("ChamberContentsChanged");
        activeRecipeId = tag.contains("ActiveRecipe") ? ResourceLocation.tryParse(tag.getString("ActiveRecipe"))
                : null;
        activeRecipe = null;
        invalidateRecipeCandidateCache();
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("Running", running);
        tag.putInt("Ticks", runningTicks);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.putInt("RenderingTicks", renderingTicks);
        tag.putBoolean("ChamberContentsChanged", chamberContentsChanged);
        if (activeRecipeId != null) {
            tag.putString("ActiveRecipe", activeRecipeId.toString());
        }
        super.write(tag, registries, clientPacket);
    }

    @Override
    public float calculateStressApplied() {
        return 4.0f;
    }

    private record ChamberContentSignature(BlockPos bottomPos, BlockPos middlePos, BlockPos topPos,
            int bottomContentsVersion, int middleContentsVersion, int topContentsVersion,
            BlockState heatSourceState) {
    }
}
