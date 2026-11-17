package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.particle.MechanicalShriekerParticleOptions;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe.ChamberLayers;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe.Stage;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModGameEvents;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MechanicalShriekerBlockEntity extends KineticBlockEntity {
    private static final ResourceLocation DREGS_ID = ResourceLocation.fromNamespaceAndPath("kubejs", "sculk_dregs");

    public int processingTicks;
    public int renderingTicks;
    public boolean running;
    private Stage processingStage;
    private boolean batchFailed;

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

        if (level.isClientSide) {
            if (running && ++renderingTicks >= 20) {
                renderParticles(getBlockState().getValue(MechanicalShriekerBlock.FACING));
                renderingTicks = 0;
            }
            return;
        }
        Optional<AbyssCatalyticChamberBlockEntity> chamber = getChamber();
        ChamberLayers layers = chamber.map(AbyssCatalysisRecipe::getLayers).orElse(null);
        if (layers == null) {
            if (processingStage != null) {
                finishBatch();
            }
            return;
        }
        if (processingStage != null && processingStage != Stage.CONDENSING && !batchFailed) {
            for (int slot = 0; slot < layers.top().getInputInventory().getSlots(); slot++) {
                if (!layers.top().getInputInventory().getStackInSlot(slot).isEmpty()) {
                    batchFailed = true;
                    setChanged();
                    break;
                }
            }
        }
        if (batchFailed) {
            setRunning(false);
            List<ItemStack> waste = List.of(new ItemStack(BuiltInRegistries.ITEM.get(DREGS_ID)));
            if (layers.bottom().acceptOutputs(waste, List.of(), true)) {
                layers.bottom().acceptOutputs(waste, List.of(), false);
                layers.bottom().notifyContentsChanged();
                finishBatch();
            }
            return;
        }
        float speed = Math.abs(getSpeed());
        if (speed <= 0 || !isSpeedRequirementFulfilled()) {
            setRunning(false);
            return;
        }
        if (processingStage == null) {
            if (chamberContentsChanged || level.getGameTime() % 20 == 0) {
                RecipeHolder<AbyssCatalysisRecipe> recipe = findMatchingRecipe(layers, chamberContentsChanged);
                chamberContentsChanged = false;
                if (recipe != null && recipe.value().processStage(layers, Stage.SETTLING, false)) {
                    setActiveRecipe(recipe);
                    startStage(Stage.SETTLING, layers, speed);
                }
            }
            return;
        }
        RecipeHolder<AbyssCatalysisRecipe> recipe = resolveActiveRecipe();
        if (recipe == null || !MachineRecipeStageManager.canProcess(this, recipe)
                || !recipe.value().hasRequiredHeat(layers)) {
            setRunning(false);
            return;
        }
        setRunning(true);
        if (processingTicks > 0) {
            processingTicks--;
            setChanged();
            if (processingTicks > 0) {
                return;
            }
        }
        switch (processingStage) {
            case SETTLING -> {
                if (recipe.value().processStage(layers, Stage.ACTIVATING, true)) {
                    startStage(Stage.ACTIVATING, layers, speed);
                }
            }
            case ACTIVATING -> {
                if (recipe.value().processStage(layers, Stage.ACTIVATING, false)) {
                    startStage(Stage.CONDENSING, layers, speed);
                }
            }
            case CONDENSING -> {
                if (chamberContentsChanged || level.getGameTime() % 20 == 0) {
                    chamberContentsChanged = false;
                    if (recipe.value().processStage(layers, Stage.CONDENSING, false)) {
                        finishBatch();
                    }
                }
            }
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        chamberContentsChanged = true;
    }

    public void onChamberContentsChanged() {
        chamberContentsChanged = true;
        invalidateRecipeCandidateCache();
        setChanged();
        sendData();
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

        return Optional.of(chamber);
    }

    private void startStage(Stage stage, ChamberLayers layers, float speed) {
        processingStage = stage;
        processingTicks = stage == Stage.CONDENSING ? 0 : Math.max(1, Mth.ceil(40 * 64 / speed));
        renderingTicks = 0;
        chamberContentsChanged = true;
        setRunning(true);
        level.gameEvent(switch (stage) {
            case SETTLING -> ModGameEvents.ABYSS_SETTLING;
            case ACTIVATING -> ModGameEvents.ABYSS_ACTIVATING;
            case CONDENSING -> ModGameEvents.ABYSS_CONDENSING;
        }, layers.bottom().getBlockPos(), GameEvent.Context.of(layers.bottom().getBlockState()));
        level.playSound(null, worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0f,
                speed < 65 ? .75f : 1.5f);
        setChanged();
        sendData();
    }

    private void finishBatch() {
        clearActiveRecipe();
        processingStage = null;
        batchFailed = false;
        processingTicks = 0;
        renderingTicks = 0;
        chamberContentsChanged = true;
        setRunning(false);
        setChanged();
        sendData();
    }

    private void setRunning(boolean value) {
        if (running == value) {
            return;
        }
        running = value;
        setShrieking(value);
        sendData();
    }

    private RecipeHolder<AbyssCatalysisRecipe> findMatchingRecipe(ChamberLayers layers,
            boolean forceCandidateRefresh) {
        ChamberContentSignature signature = createChamberContentSignature(layers);
        RecipeManager recipeManager = level.getRecipeManager();
        if (forceCandidateRefresh || !signature.equals(cachedCandidateSignature)
                || recipeManager != cachedCandidateRecipeManager) {
            rebuildRecipeCandidates(layers, signature, recipeManager);
        }

        RecipeHolder<AbyssCatalysisRecipe> bestMatch = null;
        int bestWeight = -1;
        boolean bestHasCatalyst = false;
        for (RecipeHolder<AbyssCatalysisRecipe> holder : cachedRecipeCandidates) {
            if (!MachineRecipeStageManager.canProcess(this, holder)) {
                continue;
            }
            AbyssCatalysisRecipe recipe = holder.value();
            int weight = recipeWeight(recipe);
            boolean hasCatalyst = recipe.processStage(layers, Stage.ACTIVATING, true);
            if (bestMatch == null || hasCatalyst && !bestHasCatalyst
                    || hasCatalyst == bestHasCatalyst && weight > bestWeight) {
                bestMatch = holder;
                bestWeight = weight;
                bestHasCatalyst = hasCatalyst;
            }
        }

        return bestMatch;
    }

    private void rebuildRecipeCandidates(ChamberLayers layers, ChamberContentSignature signature,
            RecipeManager recipeManager) {
        List<RecipeHolder<AbyssCatalysisRecipe>> candidates = new ArrayList<>();
        for (RecipeHolder<AbyssCatalysisRecipe> holder : getAbyssCatalysisRecipes()) {
            if (holder.value().processStage(layers, Stage.SETTLING, true)) {
                candidates.add(holder);
            }
        }

        cachedCandidateSignature = signature;
        cachedCandidateRecipeManager = recipeManager;
        cachedRecipeCandidates = List.copyOf(candidates);
    }

    private ChamberContentSignature createChamberContentSignature(ChamberLayers layers) {
        return new ChamberContentSignature(layers.bottom().getBlockPos(), layers.middle().getBlockPos(), layers.top().getBlockPos(),
                layers.bottom().getContentsVersion(), layers.middle().getContentsVersion(), layers.top().getContentsVersion(),
                level.getBlockState(layers.bottom().getBlockPos().below()));
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
        processingStage = tag.contains("ProcessingStage") ? Stage.valueOf(tag.getString("ProcessingStage")) : null;
        running = processingStage != null && tag.getBoolean("Running");
        processingTicks = tag.getInt("ProcessingTicks");
        renderingTicks = tag.getInt("RenderingTicks");
        chamberContentsChanged = tag.getBoolean("ChamberContentsChanged");
        activeRecipeId = tag.contains("ActiveRecipe") ? ResourceLocation.tryParse(tag.getString("ActiveRecipe"))
                : null;
        activeRecipe = null;
        batchFailed = tag.getBoolean("BatchFailed");
        invalidateRecipeCandidateCache();
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putBoolean("Running", running);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.putInt("RenderingTicks", renderingTicks);
        tag.putBoolean("ChamberContentsChanged", chamberContentsChanged);
        tag.putBoolean("BatchFailed", batchFailed);
        if (processingStage != null) {
            tag.putString("ProcessingStage", processingStage.name());
        }
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
