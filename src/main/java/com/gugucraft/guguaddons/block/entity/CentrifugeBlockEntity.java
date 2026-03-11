package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.recipe.CentrifugationRecipe;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.ArrayList;
import java.util.List;

public class CentrifugeBlockEntity extends KineticBlockEntity {
    public static final int MAX_BASINS = 4;

    private final SmartInventory inputInv;
    private final SmartInventory outputInv;
    private final IItemHandlerModifiable itemCapability;

    private SmartFluidTankBehaviour inputTank;
    private SmartFluidTankBehaviour outputTank;
    private IFluidHandler fluidCapability;

    private CentrifugationRecipe activeRecipe;
    private int processingTicks;
    private int basins;
    private boolean contentsChanged = true;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inputInv = new SmartInventory(9, this).whenContentsChanged(slot -> onContentsChanged());
        outputInv = new SmartInventory(9, this).whenContentsChanged(slot -> onContentsChanged()).forbidInsertion();
        itemCapability = new CentrifugeInventoryHandler(inputInv, outputInv);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 1000, true)
                .whenFluidUpdates(this::onContentsChanged);
        outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, 1000, true)
                .whenFluidUpdates(this::onContentsChanged)
                .forbidInsertion();

        behaviours.add(inputTank);
        behaviours.add(outputTank);

        fluidCapability = new CentrifugeFluidHandler(inputTank.getCapability(), outputTank.getCapability());
    }

    private void onContentsChanged() {
        contentsChanged = true;
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        if (basins < MAX_BASINS) {
            resetProcessing();
            return;
        }

        if (contentsChanged) {
            if (activeRecipe != null && !CentrifugationRecipe.match(this, activeRecipe)) {
                resetProcessing();
            }
            contentsChanged = false;
        }

        if (activeRecipe == null) {
            activeRecipe = findMatchingRecipe();
            if (activeRecipe != null) {
                processingTicks = Math.max(activeRecipe.getProcessingDuration(), 1);
                sendData();
            } else if (processingTicks != 0) {
                processingTicks = 0;
                sendData();
            }
            return;
        }

        float speed = Math.abs(getSpeed());
        if (speed < activeRecipe.getMinimalRPM()) {
            return;
        }

        processingTicks = Math.max(0, processingTicks - getProcessingSpeed());
        if (processingTicks > 0) {
            return;
        }

        if (CentrifugationRecipe.apply(this, activeRecipe)) {
            resetProcessing();
            contentsChanged = true;
            setChanged();
            sendData();
        } else {
            resetProcessing();
            sendData();
        }
    }

    private void resetProcessing() {
        activeRecipe = null;
        processingTicks = 0;
    }

    private CentrifugationRecipe findMatchingRecipe() {
        if (level == null) {
            return null;
        }

        List<CentrifugationRecipe> matches = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.CENTRIFUGATION.getType())) {
            if (!(holder.value() instanceof CentrifugationRecipe recipe)) {
                continue;
            }
            if (CentrifugationRecipe.match(this, recipe)) {
                matches.add(recipe);
            }
        }

        matches.sort((first, second) -> recipeWeight(second) - recipeWeight(first));
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private int recipeWeight(CentrifugationRecipe recipe) {
        return recipe.getIngredients().size() + recipe.getFluidIngredients().size();
    }

    public boolean addBasin(ItemStack stack) {
        if (basins >= MAX_BASINS || !stack.is(AllBlocks.BASIN.asItem())) {
            return false;
        }
        basins++;
        onContentsChanged();
        return true;
    }

    public int getBasins() {
        return basins;
    }

    public int getProcessingTicks() {
        return processingTicks;
    }

    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }

    public boolean canExternalAccess() {
        return basins == MAX_BASINS && getSpeed() == 0;
    }

    public SmartInventory getInputInventory() {
        return inputInv;
    }

    public SmartInventory getOutputInventory() {
        return outputInv;
    }

    public IItemHandlerModifiable getItemCapability() {
        return itemCapability;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IFluidHandler getInputTankCapability() {
        return inputTank == null ? null : inputTank.getCapability();
    }

    public ItemInteractionResult handleItemUse(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(AllBlocks.BASIN.asItem()) && basins < MAX_BASINS) {
            if (!level.isClientSide && addBasin(stack) && !player.isCreative()) {
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (!canExternalAccess()) {
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

        ItemStack remainder = insertIntoInput(stack.copy(), level.isClientSide);
        if (remainder.getCount() != stack.getCount()) {
            if (!level.isClientSide && !player.isCreative()) {
                player.setItemInHand(hand, remainder);
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, 1f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult handleEmptyHandUse(Player player) {
        if (!canExternalAccess()) {
            return InteractionResult.PASS;
        }

        boolean success = takeItemsBackToPlayer(player, outputInv);
        success |= takeItemsBackToPlayer(player, inputInv);
        if (!success) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f,
                    1f + level.random.nextFloat());
            onContentsChanged();
        }
        return InteractionResult.SUCCESS;
    }

    private boolean takeItemsBackToPlayer(Player player, SmartInventory inventory) {
        boolean success = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }
            player.getInventory().placeItemBackInInventory(stackInSlot.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            success = true;
        }
        return success;
    }

    public void absorbItemEntity(ItemEntity itemEntity) {
        if (level == null || level.isClientSide || !itemEntity.isAlive() || !canExternalAccess()) {
            return;
        }

        ItemStack remainder = insertIntoInput(itemEntity.getItem().copy(), false);
        if (remainder.isEmpty()) {
            itemEntity.discard();
        } else if (remainder.getCount() < itemEntity.getItem().getCount()) {
            itemEntity.setItem(remainder);
        }
    }

    private ItemStack insertIntoInput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(inputInv, stack, simulate);
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
        outputInv.allowInsertion();
        outputTank.allowInsertion();
        boolean accepted = acceptOutputsInner(outputItems, outputFluids, simulate);
        outputInv.forbidInsertion();
        outputTank.forbidInsertion();
        return accepted;
    }

    private boolean acceptOutputsInner(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
        for (ItemStack itemStack : outputItems) {
            if (!ItemHandlerHelper.insertItemStacked(outputInv, itemStack.copy(), simulate).isEmpty()) {
                return false;
            }
        }

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

    @Override
    public float calculateStressApplied() {
        return 2.0f;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null) {
            return;
        }

        if (basins > 0) {
            SmartInventory basinsInventory = new SmartInventory(1, this);
            basinsInventory.setStackInSlot(0, AllBlocks.BASIN.asStack(basins));
            ItemHelper.dropContents(level, worldPosition, basinsInventory);
        }

        ItemHelper.dropContents(level, worldPosition, inputInv);
        ItemHelper.dropContents(level, worldPosition, outputInv);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        basins = tag.getInt("Basins");
        processingTicks = tag.getInt("ProcessingTicks");
        inputInv.deserializeNBT(registries, tag.getCompound("InputInventory"));
        outputInv.deserializeNBT(registries, tag.getCompound("OutputInventory"));
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            contentsChanged = true;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt("Basins", basins);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.put("InputInventory", inputInv.serializeNBT(registries));
        tag.put("OutputInventory", outputInv.serializeNBT(registries));
        super.write(tag, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (basins < MAX_BASINS) {
            CreateLang.builder()
                    .add(Component.translatable("tooltip.guguaddons.centrifuge.basins_missing", MAX_BASINS - basins)
                            .withStyle(ChatFormatting.GOLD))
                    .forGoggles(tooltip);
            return true;
        }

        CreateLang.builder()
                .add(Component.translatable("tooltip.guguaddons.centrifuge.basins", basins, MAX_BASINS)
                        .withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        if (activeRecipe != null && Math.abs(getSpeed()) < activeRecipe.getMinimalRPM()) {
            CreateLang.builder()
                    .add(Component.translatable("tooltip.guguaddons.centrifuge.minimal_rpm", activeRecipe.getMinimalRPM())
                            .withStyle(ChatFormatting.RED))
                    .forGoggles(tooltip);
        }

        appendItemsForGoggles(tooltip, inputInv);
        appendItemsForGoggles(tooltip, outputInv);
        appendFluidsForGoggles(tooltip, fluidCapability);
        return true;
    }

    private void appendItemsForGoggles(List<Component> tooltip, IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }
            CreateLang.text("")
                    .add(Component.translatable(stackInSlot.getDescriptionId()).withStyle(ChatFormatting.GRAY))
                    .add(CreateLang.text(" x" + stackInSlot.getCount()).style(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }
    }

    private void appendFluidsForGoggles(List<Component> tooltip, IFluidHandler fluids) {
        if (fluids == null) {
            return;
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
    }

    private class CentrifugeFluidHandler extends CombinedTankWrapper {
        private final IFluidHandler inputHandler;
        private final IFluidHandler outputHandler;

        public CentrifugeFluidHandler(IFluidHandler inputHandler, IFluidHandler outputHandler) {
            super(inputHandler, outputHandler);
            this.inputHandler = inputHandler;
            this.outputHandler = outputHandler;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (!canExternalAccess()) {
                return false;
            }

            int index = getIndexForSlot(tank);
            IFluidHandler handler = getHandlerFromIndex(index);
            if (handler != inputHandler) {
                return false;
            }

            return handler.isFluidValid(getSlotFromIndex(tank, index), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return canExternalAccess() ? inputHandler.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return canExternalAccess() ? outputHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return canExternalAccess() ? outputHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    private class CentrifugeInventoryHandler extends CombinedInvWrapper {
        public CentrifugeInventoryHandler(IItemHandlerModifiable... handlers) {
            super(handlers);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (outputInv == getHandlerFromIndex(getIndexForSlot(slot))) {
                return false;
            }
            return canExternalAccess() && super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)) || !isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (inputInv == getHandlerFromIndex(getIndexForSlot(slot)) || !canExternalAccess()) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    }
}
