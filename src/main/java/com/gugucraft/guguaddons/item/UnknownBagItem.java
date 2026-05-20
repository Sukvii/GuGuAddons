package com.gugucraft.guguaddons.item;

import com.gugucraft.guguaddons.compat.astages.AStagesHelper;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class UnknownBagItem extends Item {
    private static final String ROOT_TAG = "UnknownBag";
    private static final String ITEMS_TAG = "Items";
    private static final String NO_AVAILABLE_ITEMS_KEY = "item.guguaddons.unknown_bag.no_available_items";
    private static final String RELEASED_ITEMS_KEY = "item.guguaddons.unknown_bag.released_items";

    public UnknownBagItem(Properties properties) {
        super(properties);
    }

    public static boolean store(ItemStack bag, ItemStack stack, HolderLookup.Provider registries) {
        if (!isUnknownBag(bag) || stack.isEmpty() || stack.is(ModItems.UNKNOWN_BAG.get())) {
            return false;
        }

        List<ItemStack> stored = getStoredItems(bag, registries);
        ItemStack remaining = stack.copy();
        mergeIntoStored(stored, remaining);
        if (!remaining.isEmpty()) {
            stored.add(remaining.copy());
            remaining.setCount(0);
        }

        writeStoredItems(bag, stored, registries);
        return true;
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

    private static void mergeIntoStored(List<ItemStack> stored, ItemStack remaining) {
        for (ItemStack storedStack : stored) {
            if (remaining.isEmpty()) {
                return;
            }
            if (!ItemStack.isSameItemSameComponents(storedStack, remaining)) {
                continue;
            }

            int space = storedStack.getMaxStackSize() - storedStack.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            storedStack.grow(moved);
            remaining.shrink(moved);
        }
    }

    private static boolean isUnknownBag(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.UNKNOWN_BAG.get());
    }
}
