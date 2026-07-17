package com.gugucraft.guguaddons.item;

import com.gugucraft.guguaddons.compat.astages.AStagesHelper;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class UnknownBagItem extends Item {
    private static final String ROOT_TAG = "UnknownBag";
    private static final String ITEMS_TAG = "Items";
    private static final String NO_AVAILABLE_ITEMS_KEY = "item.guguaddons.unknown_bag.no_available_items";
    private static final String RELEASED_ITEMS_KEY = "item.guguaddons.unknown_bag.released_items";
    private static final int MAX_STORED_ENTRIES = 256;
    private static final long MAX_CUSTOM_DATA_BYTES = 512L * 1024L;

    public UnknownBagItem(Properties properties) {
        super(properties);
    }

    public static boolean store(ItemStack bag, ItemStack stack, HolderLookup.Provider registries) {
        StorageBatch batch = startStorageBatch(bag, registries);
        return batch.tryStore(stack) && batch.commit();
    }

    public static StorageBatch startStorageBatch(ItemStack bag, HolderLookup.Provider registries) {
        return new StorageBatch(bag, registries);
    }

    public static boolean hasStoredItems(ItemStack bag) {
        if (!isUnknownBag(bag)) {
            return false;
        }
        CustomData data = bag.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag root = data.copyTag().getCompound(ROOT_TAG);
        return root.contains(ITEMS_TAG) && !root.getList(ITEMS_TAG, 10).isEmpty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack bag = player.getItemInHand(usedHand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int released = releaseAvailableItems(serverPlayer, bag);
            if (released <= 0) {
                serverPlayer.displayClientMessage(
                        Component.translatable(NO_AVAILABLE_ITEMS_KEY).withStyle(ChatFormatting.YELLOW),
                        true);
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable(RELEASED_ITEMS_KEY).withStyle(ChatFormatting.GREEN),
                        true);
            }
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide);
    }

    private static int releaseAvailableItems(ServerPlayer player, ItemStack bag) {
        HolderLookup.Provider registries = player.registryAccess();
        List<ItemStack> stored = getStoredItems(bag, registries);
        if (stored.isEmpty()) {
            return 0;
        }

        int released = 0;
        List<ItemStack> remainingStored = new ArrayList<>();
        for (ItemStack storedStack : stored) {
            if (storedStack.isEmpty()) {
                continue;
            }
            if (AStagesHelper.isStillUnknownItem(player, storedStack)) {
                remainingStored.add(storedStack.copy());
                continue;
            }

            ItemStack toInsert = storedStack.copy();
            int before = toInsert.getCount();
            player.getInventory().add(toInsert);
            released += before - toInsert.getCount();
            if (!toInsert.isEmpty()) {
                remainingStored.add(toInsert.copy());
            }
        }

        writeStoredItems(bag, remainingStored, registries);
        return released;
    }

    private static List<ItemStack> getStoredItems(ItemStack bag, HolderLookup.Provider registries) {
        List<ItemStack> stored = new ArrayList<>();
        if (!isUnknownBag(bag)) {
            return stored;
        }

        CustomData data = bag.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return stored;
        }

        CompoundTag root = data.copyTag().getCompound(ROOT_TAG);
        ListTag list = root.getList(ITEMS_TAG, 10);
        for (int i = 0; i < list.size(); i++) {
            ItemStack storedStack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!storedStack.isEmpty() && !storedStack.is(ModItems.UNKNOWN_BAG.get())) {
                stored.add(storedStack);
            }
        }
        return stored;
    }

    private static void writeStoredItems(ItemStack bag, List<ItemStack> stored, HolderLookup.Provider registries) {
        CompoundTag customData = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (stored.isEmpty()) {
            customData.remove(ROOT_TAG);
        } else {
            ListTag list = new ListTag();
            for (ItemStack storedStack : stored) {
                if (!storedStack.isEmpty() && !storedStack.is(ModItems.UNKNOWN_BAG.get())) {
                    list.add(storedStack.saveOptional(registries));
                }
            }

            if (list.isEmpty()) {
                customData.remove(ROOT_TAG);
            } else {
                CompoundTag root = new CompoundTag();
                root.put(ITEMS_TAG, list);
                customData.put(ROOT_TAG, root);
            }
        }
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    public static final class StorageBatch {
        private final ItemStack bag;
        private final HolderLookup.Provider registries;
        private final List<ItemStack> stored;
        private final List<Long> entryBytes;
        private final long baseBytes;

        private long payloadBytes;
        private boolean withinLimits;
        private boolean dirty;
        private boolean committed;

        private StorageBatch(ItemStack bag, HolderLookup.Provider registries) {
            this.bag = bag;
            this.registries = registries;
            this.stored = getStoredItems(bag, registries);
            this.entryBytes = new ArrayList<>(stored.size());
            this.baseBytes = isUnknownBag(bag) ? measureFullCustomData(createEmptyStorageData(bag)) : -1L;
            this.withinLimits = baseBytes >= 0L && stored.size() <= MAX_STORED_ENTRIES;

            for (ItemStack storedStack : stored) {
                long serializedBytes = measureEntry(storedStack.saveOptional(registries));
                entryBytes.add(serializedBytes);
                if (serializedBytes < 0L || !canAddCurrentPayload(serializedBytes)) {
                    withinLimits = false;
                } else {
                    payloadBytes += serializedBytes;
                }
            }
            if (!fitsCustomData(payloadBytes, stored.size())) {
                withinLimits = false;
            }
        }

        public boolean tryStore(ItemStack stack) {
            if (committed || !withinLimits || stack.isEmpty() || stack.is(ModItems.UNKNOWN_BAG.get())) {
                return false;
            }

            ItemStack remaining = stack.copy();
            List<EntryUpdate> updates = new ArrayList<>();
            long candidatePayloadBytes = payloadBytes;

            for (int index = 0; index < stored.size() && !remaining.isEmpty(); index++) {
                ItemStack storedStack = stored.get(index);
                if (!ItemStack.isSameItemSameComponents(storedStack, remaining)) {
                    continue;
                }

                int space = storedStack.getMaxStackSize() - storedStack.getCount();
                if (space <= 0) {
                    continue;
                }

                int moved = Math.min(space, remaining.getCount());
                ItemStack updatedStack = storedStack.copy();
                updatedStack.grow(moved);
                long updatedBytes = measureEntry(updatedStack.saveOptional(registries));
                if (updatedBytes < 0L) {
                    return false;
                }

                long replacedPayloadBytes = replacePayload(candidatePayloadBytes, entryBytes.get(index), updatedBytes);
                if (replacedPayloadBytes < 0L) {
                    return false;
                }
                candidatePayloadBytes = replacedPayloadBytes;
                remaining.shrink(moved);
                updates.add(new EntryUpdate(index, updatedStack, updatedBytes));
            }

            ItemStack appendedStack = ItemStack.EMPTY;
            long appendedBytes = 0L;
            int candidateEntries = stored.size();
            if (!remaining.isEmpty()) {
                if (candidateEntries >= MAX_STORED_ENTRIES) {
                    return false;
                }

                appendedStack = remaining.copy();
                appendedBytes = measureEntry(appendedStack.saveOptional(registries));
                if (appendedBytes < 0L || !UnknownBagItem.canAddPayload(candidatePayloadBytes, appendedBytes)) {
                    return false;
                }
                candidatePayloadBytes += appendedBytes;
                candidateEntries++;
            }

            if (!fitsCustomData(candidatePayloadBytes, candidateEntries)) {
                return false;
            }

            for (EntryUpdate update : updates) {
                stored.set(update.index(), update.stack());
                entryBytes.set(update.index(), update.serializedBytes());
            }
            if (!appendedStack.isEmpty()) {
                stored.add(appendedStack);
                entryBytes.add(appendedBytes);
            }
            payloadBytes = candidatePayloadBytes;
            dirty = true;
            return true;
        }

        public boolean commit() {
            if (committed || !dirty || !withinLimits) {
                return false;
            }

            writeStoredItems(bag, stored, registries);
            committed = true;
            return true;
        }

        private boolean canAddCurrentPayload(long additionalBytes) {
            return UnknownBagItem.canAddPayload(payloadBytes, additionalBytes);
        }

        private boolean fitsCustomData(long candidatePayloadBytes, int candidateEntries) {
            if (baseBytes < 0L || candidateEntries < 0 || candidateEntries > MAX_STORED_ENTRIES
                    || candidatePayloadBytes < 0L || baseBytes > MAX_CUSTOM_DATA_BYTES) {
                return false;
            }
            long availableBytes = MAX_CUSTOM_DATA_BYTES - baseBytes;
            return candidatePayloadBytes <= availableBytes;
        }
    }

    private static CompoundTag createEmptyStorageData(ItemStack bag) {
        CompoundTag customData = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = new CompoundTag();
        root.put(ITEMS_TAG, new ListTag());
        customData.put(ROOT_TAG, root);
        return customData;
    }

    private static long replacePayload(long currentBytes, long oldBytes, long newBytes) {
        if (oldBytes < 0L || oldBytes > currentBytes || newBytes < 0L) {
            return -1L;
        }
        return canAddPayload(currentBytes - oldBytes, newBytes) ? currentBytes - oldBytes + newBytes : -1L;
    }

    private static boolean canAddPayload(long currentBytes, long additionalBytes) {
        return currentBytes >= 0L && additionalBytes >= 0L
                && currentBytes <= MAX_CUSTOM_DATA_BYTES - additionalBytes;
    }

    private static long measureFullCustomData(CompoundTag customData) {
        return measureNbt(output -> NbtIo.write(customData, output));
    }

    private static long measureEntry(Tag entry) {
        return measureNbt(entry::write);
    }

    private static long measureNbt(NbtWriter writer) {
        LimitedCountingOutputStream counter = new LimitedCountingOutputStream(MAX_CUSTOM_DATA_BYTES);
        try {
            writer.write(new DataOutputStream(counter));
            return counter.count();
        } catch (IOException ignored) {
            return -1L;
        }
    }

    @FunctionalInterface
    private interface NbtWriter {
        void write(DataOutput output) throws IOException;
    }

    private record EntryUpdate(int index, ItemStack stack, long serializedBytes) {
    }

    private static final class LimitedCountingOutputStream extends OutputStream {
        private final long limit;
        private long count;

        private LimitedCountingOutputStream(long limit) {
            this.limit = limit;
        }

        @Override
        public void write(int value) throws IOException {
            addBytes(1L);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            if (data == null) {
                throw new NullPointerException("data");
            }
            if (offset < 0 || length < 0 || offset > data.length - length) {
                throw new IndexOutOfBoundsException();
            }
            addBytes(length);
        }

        private void addBytes(long addedBytes) throws IOException {
            if (addedBytes < 0L || count > limit - addedBytes) {
                throw new IOException("NBT size exceeds limit");
            }
            count += addedBytes;
        }

        private long count() {
            return count;
        }
    }

    private static boolean isUnknownBag(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.UNKNOWN_BAG.get());
    }
}
