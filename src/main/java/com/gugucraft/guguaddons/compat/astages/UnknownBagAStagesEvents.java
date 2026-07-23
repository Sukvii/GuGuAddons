package com.gugucraft.guguaddons.compat.astages;

import java.util.ArrayList;
import java.util.List;

import com.alessandro.astages.infrastructure.hook.CommonEventSettings;
import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.item.UnknownBagItem;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class UnknownBagAStagesEvents {
    private UnknownBagAStagesEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || stack.is(ModItems.UNKNOWN_BAG.get())) {
            return;
        }
        if (!AStagesHelper.isUnknownPickupItem(player, stack)) {
            return;
        }

        ItemStack bag = findFirstUnknownBag(player.getInventory());
        if (bag.isEmpty()) {
            return;
        }

        if (UnknownBagItem.store(bag, stack, player.registryAccess())) {
            stack.setCount(0);
            itemEntity.discard();
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (!CommonEventSettings.requireSlotCheck() && !CommonEventSettings.requireContainerCheck()) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack bag = findFirstUnknownBag(inventory);
        if (bag.isEmpty()) {
            return;
        }

        // AStages' own slot scan (NORMAL priority, same event) runs every tick and drops
        // restricted items on the ground, so this scan must also run every tick to claim
        // them first. Only the restriction lookup runs unconditionally; the expensive bag
        // deserialization in startStorageBatch is deferred until a slot actually matches.
        List<Integer> restrictedSlots = null;
        int mainSize = inventory.items.size();
        int equipmentLimit = mainSize + inventory.armor.size();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.is(ModItems.UNKNOWN_BAG.get())) {
                continue;
            }

            boolean isEquipmentSlot = slot >= mainSize && slot <= equipmentLimit;
            boolean shouldStore = isEquipmentSlot
                    ? AStagesHelper.isUnknownEquipmentItem(player, stack)
                    : AStagesHelper.isUnknownInventoryItem(player, stack);
            if (!shouldStore) {
                continue;
            }

            if (restrictedSlots == null) {
                restrictedSlots = new ArrayList<>();
            }
            restrictedSlots.add(slot);
        }
        if (restrictedSlots == null) {
            return;
        }

        UnknownBagItem.StorageBatch batch = UnknownBagItem.startStorageBatch(bag, player.registryAccess());
        List<Integer> storedSlots = new ArrayList<>();
        for (int slot : restrictedSlots) {
            if (batch.tryStore(inventory.getItem(slot))) {
                storedSlots.add(slot);
            }
        }

        if (batch.commit()) {
            for (int slot : storedSlots) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack findFirstUnknownBag(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.UNKNOWN_BAG.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
