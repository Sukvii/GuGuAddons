package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.infrastructure.hook.CommonEventSettings;
import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.item.UnknownBagItem;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class UnknownBagAStagesEvents {
    private UnknownBagAStagesEvents() {
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

            if (UnknownBagItem.store(bag, stack, player.registryAccess())) {
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
