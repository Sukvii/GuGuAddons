package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberBlock;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class AbyssCatalyticChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int INVENTORY_SLOTS = 9;
    public static final int TANKS = 2;
    public static final int TANK_CAPACITY = 8000;

    private final boolean hasOutputs;
    private final SmartInventory inputInv;
    @Nullable
    private final SmartInventory outputInv;
    private final IItemHandlerModifiable itemCapability;

    private SmartFluidTankBehaviour inputTank;
    @Nullable
    private SmartFluidTankBehaviour outputTank;
    private IFluidHandler fluidCapability;

    private int contentsVersion;
    private int recipeBackupCheck = 20;
    private final List<Direction> disabledSpoutputs = new ArrayList<>();
    @Nullable
    private Direction preferredSpoutput;
    private final List<ItemStack> spoutputBuffer = new ArrayList<>();
    private final List<FluidStack> spoutputFluidBuffer = new ArrayList<>();

    public AbyssCatalyticChamberBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ABYSS_CATALYTIC_CHAMBER.get(), pos, state, true);
    }

    protected AbyssCatalyticChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, false);
    }

    private AbyssCatalyticChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            boolean hasOutputs) {
        super(type, pos, state);
        this.hasOutputs = hasOutputs;
        inputInv = new SmartInventory(INVENTORY_SLOTS, this).whenContentsChanged(slot -> onContentsChanged());
        outputInv = hasOutputs
                ? new SmartInventory(INVENTORY_SLOTS, this).whenContentsChanged(slot -> onContentsChanged())
                        .forbidInsertion()
                : null;
        itemCapability = outputInv == null ? inputInv : new ChamberInventoryHandler(inputInv, outputInv);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, TANKS, TANK_CAPACITY, true)
                .whenFluidUpdates(this::onContentsChanged);
        behaviours.add(inputTank);

        if (shouldCreateOutputBehaviours()) {
            outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, TANKS, TANK_CAPACITY, true)
                    .whenFluidUpdates(this::onContentsChanged)
                    .forbidInsertion();
            behaviours.add(outputTank);
            fluidCapability = new ChamberFluidHandler(inputTank.getCapability(), outputTank.getCapability());
        } else {
            fluidCapability = inputTank.getCapability();
        }
    }

    private boolean shouldCreateOutputBehaviours() {
        // SmartBlockEntity calls addBehaviours from its constructor before this.hasOutputs is assigned.
        return hasOutputs || getType() == ModBlockEntities.ABYSS_CATALYTIC_CHAMBER.get();
    }

    public SmartInventory getInputInventory() {
        return inputInv;
    }

    @Nullable
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

    @Nullable
    public IFluidHandler getOutputTankCapability() {
        return outputTank == null ? null : outputTank.getCapability();
    }

    public int getContentsVersion() {
        return contentsVersion;
    }

    public BlockPos getBottomPos() {
        return AbyssCatalyticChamberBlock.getBottomPos(worldPosition, getBlockState());
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

        if (isWrench(stack) || !canInsertItem(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack remainder = insertIntoInput(stack.copy(), level.isClientSide);
        if (remainder.getCount() != stack.getCount()) {
            if (!level.isClientSide && !player.isCreative()) {
                player.setItemInHand(hand, remainder);
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, 1f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    public InteractionResult handleEmptyHandUse(Player player) {
        if (level == null) {
            return InteractionResult.PASS;
        }

        SmartInventory inventory = outputInv != null && !isInventoryEmpty(outputInv) ? outputInv : inputInv;
        if (isInventoryEmpty(inventory)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!takeItemsBackToPlayer(player, inventory)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f,
                    1f + level.random.nextFloat());
            onContentsChanged();
        }
        return InteractionResult.SUCCESS;
    }

    private ItemStack insertIntoInput(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(inputInv, stack, simulate);
    }

    public void notifyContentsChanged() {
        onContentsChanged();
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
        if (!hasOutputs || outputInv == null || outputTank == null) {
            return outputItems.isEmpty() && outputFluids.isEmpty();
        }

        outputInv.allowInsertion();
        outputTank.allowInsertion();
        try {
            return acceptOutputsInner(outputItems, outputFluids, simulate);
        } finally {
            outputInv.forbidInsertion();
            outputTank.forbidInsertion();
        }
    }

    private boolean acceptOutputsInner(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
        if (level == null || outputInv == null || outputTank == null) {
            return false;
        }

        Direction direction = getBlockState().getValue(AbyssCatalyticChamberBlock.FACING);
        if (direction == Direction.DOWN) {
            return insertItemsIntoTarget(outputItems, outputInv, simulate)
                    && fillFluidsIntoTarget(outputFluids, outputTank.getCapability(), simulate);
        }

        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.below().relative(direction));
        InvManipulationBehaviour inserter = targetBlockEntity == null ? null
                : BlockEntityBehaviour.get(level, targetBlockEntity.getBlockPos(), InvManipulationBehaviour.TYPE);
        IItemHandler targetInv = targetBlockEntity == null ? null
                : level.getCapability(Capabilities.ItemHandler.BLOCK, targetBlockEntity.getBlockPos(),
                        direction.getOpposite());
        if (targetInv == null && inserter != null) {
            targetInv = inserter.getInventory();
        }

        IFluidHandler targetTank = targetBlockEntity == null ? null
                : level.getCapability(Capabilities.FluidHandler.BLOCK, targetBlockEntity.getBlockPos(),
                        direction.getOpposite());
        boolean externalTankMissing = targetTank == null;
        if (!outputItems.isEmpty() && targetInv == null) {
            return false;
        }

        if (!outputFluids.isEmpty() && externalTankMissing) {
            targetTank = outputTank.getCapability();
            if (targetTank == null || !fillFluidsIntoTarget(outputFluids, targetTank, simulate)) {
                return false;
            }
        }

        if (simulate) {
            return true;
        }

        for (ItemStack itemStack : outputItems) {
            if (!itemStack.isEmpty()) {
                spoutputBuffer.add(itemStack.copy());
            }
        }
        if (!externalTankMissing) {
            for (FluidStack fluidStack : outputFluids) {
                if (!fluidStack.isEmpty()) {
                    spoutputFluidBuffer.add(fluidStack.copy());
                }
            }
        }
        return true;
    }

    private boolean insertItemsIntoTarget(List<ItemStack> outputItems, IItemHandler target, boolean simulate) {
        for (ItemStack itemStack : outputItems) {
            if (!ItemHandlerHelper.insertItemStacked(target, itemStack.copy(), simulate).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean fillFluidsIntoTarget(List<FluidStack> outputFluids, IFluidHandler target, boolean simulate) {
        for (FluidStack fluidStack : outputFluids) {
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            int filled = target instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler
                    ? internalHandler.forceFill(fluidStack.copy(), action)
                    : target.fill(fluidStack.copy(), action);
            if (filled != fluidStack.getAmount()) {
                return false;
            }
        }
        return true;
    }

    public void onWrenched(Direction face) {
        if (!hasOutputs || level == null) {
            return;
        }

        Direction currentFacing = getBlockState().getValue(AbyssCatalyticChamberBlock.FACING);
        disabledSpoutputs.remove(face);
        if (currentFacing == face) {
            if (preferredSpoutput == face) {
                preferredSpoutput = null;
            }
            disabledSpoutputs.add(face);
        } else {
            preferredSpoutput = face;
        }

        updateSpoutput();
        setChanged();
        sendData();
    }

    public boolean canContinueProcessing() {
        AbyssCatalyticChamberBlockEntity bottom = getBottom();
        return bottom != null && bottom.isBufferEmpty();
    }

    public boolean isBufferEmpty() {
        return spoutputBuffer.isEmpty() && spoutputFluidBuffer.isEmpty();
    }

    @Nullable
    public AbyssCatalyticChamberBlockEntity getBottom() {
        if (level == null) {
            return null;
        }
        BlockPos bottomPos = getBottomPos();
        return level.getBlockEntity(bottomPos) instanceof AbyssCatalyticChamberBlockEntity bottom ? bottom : null;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasOutputs) {
            updateSpoutput();
        }

        if (recipeBackupCheck-- > 0) {
            return;
        }

        recipeBackupCheck = 20;
        if (!isEmptyForRecipeCheck()) {
            notifyOperatorOfContentsChanged();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (hasOutputs && level != null && !level.isClientSide
                && (!spoutputBuffer.isEmpty() || !spoutputFluidBuffer.isEmpty())) {
            tryClearingSpoutputOverflow();
        }
    }

    private void updateSpoutput() {
        if (!hasOutputs || level == null || outputInv == null || outputTank == null) {
            return;
        }

        BlockState blockState = getBlockState();
        Direction currentFacing = blockState.getValue(AbyssCatalyticChamberBlock.FACING);
        Direction newFacing = Direction.DOWN;
        for (Direction test : Direction.Plane.HORIZONTAL) {
            if (BasinBlock.canOutputTo(level, worldPosition, test) && !disabledSpoutputs.contains(test)) {
                newFacing = test;
            }
        }

        if (preferredSpoutput != null && preferredSpoutput.getAxis().isHorizontal()
                && BasinBlock.canOutputTo(level, worldPosition, preferredSpoutput)) {
            newFacing = preferredSpoutput;
        }

        if (newFacing == currentFacing) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, blockState.setValue(AbyssCatalyticChamberBlock.FACING, newFacing));
        if (newFacing.getAxis().isVertical()) {
            return;
        }

        for (int slot = 0; slot < outputInv.getSlots(); slot++) {
            ItemStack extracted = outputInv.extractItem(slot, 64, true);
            if (!extracted.isEmpty() && acceptOutputs(Collections.singletonList(extracted), Collections.emptyList(), true)) {
                acceptOutputs(Collections.singletonList(outputInv.extractItem(slot, 64, false)),
                        Collections.emptyList(), false);
            }
        }

        IFluidHandler handler = outputTank.getCapability();
        if (handler != null) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack fluidStack = handler.getFluidInTank(tank).copy();
                if (!fluidStack.isEmpty()
                        && acceptOutputs(Collections.emptyList(), Collections.singletonList(fluidStack), true)) {
                    handler.drain(fluidStack, FluidAction.EXECUTE);
                    acceptOutputs(Collections.emptyList(), Collections.singletonList(fluidStack), false);
                }
            }
        }

        onContentsChanged();
    }

    private void tryClearingSpoutputOverflow() {
        Direction direction = getBlockState().getValue(AbyssCatalyticChamberBlock.FACING);
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.below().relative(direction));
        FilteringBehaviour filter = null;
        InvManipulationBehaviour inserter = null;
        if (targetBlockEntity != null) {
            filter = BlockEntityBehaviour.get(level, targetBlockEntity.getBlockPos(), FilteringBehaviour.TYPE);
            inserter = BlockEntityBehaviour.get(level, targetBlockEntity.getBlockPos(), InvManipulationBehaviour.TYPE);
        }
        if (filter != null && filter.isRecipeFilter()) {
            filter = null;
        }

        IItemHandler targetInv = targetBlockEntity == null ? null
                : level.getCapability(Capabilities.ItemHandler.BLOCK, targetBlockEntity.getBlockPos(),
                        direction.getOpposite());
        if (targetInv == null && inserter != null) {
            targetInv = inserter.getInventory();
        }
        IFluidHandler targetTank = targetBlockEntity == null ? null
                : level.getCapability(Capabilities.FluidHandler.BLOCK, targetBlockEntity.getBlockPos(),
                        direction.getOpposite());

        boolean update = false;
        Iterator<ItemStack> itemIterator = spoutputBuffer.iterator();
        while (itemIterator.hasNext()) {
            ItemStack itemStack = itemIterator.next();
            if (direction == Direction.DOWN) {
                Block.popResource(level, worldPosition, itemStack);
                itemIterator.remove();
                update = true;
                continue;
            }
            if (targetInv == null) {
                break;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, itemStack, true);
            if (remainder.getCount() != itemStack.getCount()
                    && (filter == null || filter.test(itemStack))) {
                remainder = ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), false);
                update = true;
                if (remainder.isEmpty()) {
                    itemIterator.remove();
                } else {
                    itemStack.setCount(remainder.getCount());
                }
            }
        }

        Iterator<FluidStack> fluidIterator = spoutputFluidBuffer.iterator();
        while (fluidIterator.hasNext()) {
            FluidStack fluidStack = fluidIterator.next();
            if (direction == Direction.DOWN) {
                fluidIterator.remove();
                update = true;
                continue;
            }
            if (targetTank == null) {
                break;
            }

            int filled = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler
                    ? internalHandler.forceFill(fluidStack.copy(), FluidAction.SIMULATE)
                    : targetTank.fill(fluidStack.copy(), FluidAction.SIMULATE);
            if (filled != fluidStack.getAmount()) {
                continue;
            }

            filled = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler
                    ? internalHandler.forceFill(fluidStack.copy(), FluidAction.EXECUTE)
                    : targetTank.fill(fluidStack.copy(), FluidAction.EXECUTE);
            if (filled == fluidStack.getAmount()) {
                fluidIterator.remove();
                update = true;
            }
        }

        if (update) {
            onContentsChanged();
        }
    }

    private boolean isInventoryEmpty(SmartInventory inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
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

    private void onContentsChanged() {
        contentsVersion++;
        setChanged();
        sendData();
        notifyOperatorOfContentsChanged();
    }

    private void notifyOperatorOfContentsChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        getOperator().ifPresent(MechanicalShriekerBlockEntity::onChamberContentsChanged);
    }

    public Optional<MechanicalShriekerBlockEntity> getOperator() {
        if (level == null) {
            return Optional.empty();
        }

        BlockPos bottomPos = getBottomPos();
        if (level.getBlockEntity(bottomPos.above(4)) instanceof MechanicalShriekerBlockEntity shrieker) {
            return Optional.of(shrieker);
        }
        if (level.getBlockEntity(bottomPos.above(3)) instanceof MechanicalShriekerBlockEntity shrieker) {
            return Optional.of(shrieker);
        }
        return Optional.empty();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null) {
            return;
        }

        ItemHelper.dropContents(level, worldPosition, inputInv);
        if (outputInv != null) {
            ItemHelper.dropContents(level, worldPosition, outputInv);
        }
        for (ItemStack itemStack : spoutputBuffer) {
            if (!itemStack.isEmpty()) {
                Block.popResource(level, worldPosition, itemStack);
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        contentsVersion = tag.getInt("ContentsVersion");
        inputInv.deserializeNBT(registries, tag.getCompound("InputInventory"));
        if (outputInv != null) {
            outputInv.deserializeNBT(registries, tag.getCompound("OutputInventory"));
        }
        readSpoutputState(tag, registries);
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            contentsVersion++;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt("ContentsVersion", contentsVersion);
        tag.put("InputInventory", inputInv.serializeNBT(registries));
        if (outputInv != null) {
            tag.put("OutputInventory", outputInv.serializeNBT(registries));
        }
        writeSpoutputState(tag, registries);
        super.write(tag, registries, clientPacket);
    }

    private void readSpoutputState(CompoundTag tag, HolderLookup.Provider registries) {
        preferredSpoutput = null;
        if (tag.contains("PreferredSpoutput")) {
            try {
                preferredSpoutput = Direction.valueOf(tag.getString("PreferredSpoutput"));
            } catch (IllegalArgumentException ignored) {
                preferredSpoutput = null;
            }
        }

        disabledSpoutputs.clear();
        ListTag disabledList = tag.getList("DisabledSpoutput", Tag.TAG_STRING);
        for (Tag element : disabledList) {
            if (element instanceof StringTag stringTag) {
                try {
                    disabledSpoutputs.add(Direction.valueOf(stringTag.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid legacy data.
                }
            }
        }

        spoutputBuffer.clear();
        ListTag overflowItems = tag.getList("Overflow", Tag.TAG_COMPOUND);
        for (Tag element : overflowItems) {
            if (element instanceof CompoundTag itemTag) {
                ItemStack stack = ItemStack.parseOptional(registries, itemTag);
                if (!stack.isEmpty()) {
                    spoutputBuffer.add(stack);
                }
            }
        }

        spoutputFluidBuffer.clear();
        ListTag overflowFluids = tag.getList("FluidOverflow", Tag.TAG_COMPOUND);
        for (Tag element : overflowFluids) {
            if (element instanceof CompoundTag fluidTag) {
                FluidStack fluidStack = FluidStack.parseOptional(registries, fluidTag);
                if (!fluidStack.isEmpty()) {
                    spoutputFluidBuffer.add(fluidStack);
                }
            }
        }
    }

    private void writeSpoutputState(CompoundTag tag, HolderLookup.Provider registries) {
        if (preferredSpoutput != null) {
            tag.putString("PreferredSpoutput", preferredSpoutput.name());
        }

        ListTag disabledList = new ListTag();
        for (Direction direction : disabledSpoutputs) {
            disabledList.add(StringTag.valueOf(direction.name()));
        }
        tag.put("DisabledSpoutput", disabledList);

        ListTag overflowItems = new ListTag();
        for (ItemStack itemStack : spoutputBuffer) {
            if (!itemStack.isEmpty()) {
                overflowItems.add(itemStack.saveOptional(registries));
            }
        }
        tag.put("Overflow", overflowItems);

        ListTag overflowFluids = new ListTag();
        for (FluidStack fluidStack : spoutputFluidBuffer) {
            if (!fluidStack.isEmpty()) {
                overflowFluids.add(fluidStack.saveOptional(registries));
            }
        }
        tag.put("FluidOverflow", overflowFluids);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("guguaddons.gui.goggles.abyss_catalytic_chamber_contents")
                        .withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        appendItemsForGoggles(tooltip, "guguaddons.gui.goggles.abyss_catalytic_chamber_input", inputInv);
        appendFluidsForGoggles(tooltip, "guguaddons.gui.goggles.abyss_catalytic_chamber_input",
                getInputTankCapability());

        if (outputInv != null) {
            appendItemsForGoggles(tooltip, "guguaddons.gui.goggles.abyss_catalytic_chamber_output", outputInv);
        }
        if (outputTank != null) {
            appendFluidsForGoggles(tooltip, "guguaddons.gui.goggles.abyss_catalytic_chamber_output",
                    outputTank.getCapability());
        }

        return true;
    }

    private void appendItemsForGoggles(List<Component> tooltip, String sectionKey, IItemHandler inventory) {
        boolean addedHeader = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }
            if (!addedHeader) {
                CreateLang.builder()
                        .add(Component.translatable(sectionKey).withStyle(ChatFormatting.DARK_GRAY))
                        .forGoggles(tooltip, 1);
                addedHeader = true;
            }
            CreateLang.text("")
                    .add(Component.translatable(stackInSlot.getDescriptionId()).withStyle(ChatFormatting.GRAY))
                    .add(CreateLang.text(" x" + stackInSlot.getCount()).style(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 2);
        }
    }

    private void appendFluidsForGoggles(List<Component> tooltip, String sectionKey, IFluidHandler fluids) {
        if (fluids == null) {
            return;
        }

        LangBuilder millibuckets = CreateLang.translate("generic.unit.millibuckets");
        boolean addedHeader = false;
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluidStack = fluids.getFluidInTank(tank);
            if (fluidStack.isEmpty()) {
                continue;
            }
            if (!addedHeader) {
                CreateLang.builder()
                        .add(Component.translatable(sectionKey).withStyle(ChatFormatting.DARK_GRAY))
                        .forGoggles(tooltip, 1);
                addedHeader = true;
            }
            CreateLang.text("")
                    .add(CreateLang.fluidName(fluidStack)
                            .add(CreateLang.text(" "))
                            .style(ChatFormatting.GRAY)
                            .add(CreateLang.number(fluidStack.getAmount())
                                    .add(millibuckets)
                                    .style(ChatFormatting.BLUE)))
                    .forGoggles(tooltip, 2);
        }
    }

    private boolean canInsertItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }

        if (getOperator().isEmpty()) {
            return true;
        }

        for (RecipeHolder<AbyssCatalysisRecipe> holder : getAbyssCatalysisRecipes()) {
            if (canRecipeUseItem(holder.value(), stack)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<RecipeHolder<AbyssCatalysisRecipe>> getAbyssCatalysisRecipes() {
        return (List) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) ModRecipes.ABYSS_CATALYSIS.getType());
    }

    private boolean canRecipeUseItem(AbyssCatalysisRecipe recipe, ItemStack stack) {
        BlockPos bottomPos = getBottomPos();
        List<net.minecraft.world.item.crafting.Ingredient> ingredients;
        if (worldPosition.equals(bottomPos.above(2))) {
            ingredients = recipe.getTopItemIngredients();
        } else if (worldPosition.equals(bottomPos.above())) {
            ingredients = recipe.getCatalystItemIngredients();
        } else {
            ingredients = recipe.getBottomItemIngredients();
        }

        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    private boolean isEmptyForRecipeCheck() {
        if (!isInventoryEmpty(inputInv) || inputTank != null && !inputTank.isEmpty()) {
            return false;
        }
        if (outputInv != null && !isInventoryEmpty(outputInv)) {
            return false;
        }
        return outputTank == null || outputTank.isEmpty();
    }

    private static boolean isWrench(ItemStack stack) {
        return AllItems.WRENCH.isIn(stack);
    }

    private class ChamberInventoryHandler extends CombinedInvWrapper {
        private final IItemHandlerModifiable outputHandler;

        public ChamberInventoryHandler(IItemHandlerModifiable inputHandler, IItemHandlerModifiable outputHandler) {
            super(inputHandler, outputHandler);
            this.outputHandler = outputHandler;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (outputHandler == getHandlerFromIndex(getIndexForSlot(slot))) {
                return false;
            }
            return super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (outputHandler == getHandlerFromIndex(getIndexForSlot(slot))) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    }

    private class ChamberFluidHandler extends CombinedTankWrapper {
        private final IFluidHandler inputHandler;

        public ChamberFluidHandler(IFluidHandler inputHandler, IFluidHandler outputHandler) {
            super(outputHandler, inputHandler);
            this.inputHandler = inputHandler;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            int index = getIndexForSlot(tank);
            IFluidHandler handler = getHandlerFromIndex(index);
            return handler == inputHandler && handler.isFluidValid(getSlotFromIndex(tank, index), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return inputHandler.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return super.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return super.drain(maxDrain, action);
        }
    }

}
