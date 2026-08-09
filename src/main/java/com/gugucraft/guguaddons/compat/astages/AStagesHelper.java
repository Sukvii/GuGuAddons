package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.engine.ARestrictionManager;
import com.alessandro.astages.engine.server.restriction.item.ABaseItemRestriction;
import com.alessandro.astages.engine.store.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AStagesHelper {
    private AStagesHelper() {
    }

    public static boolean isUnknownInventoryItem(ServerPlayer player, ItemStack stack) {
        ABaseItemRestriction<?, ?> restriction = getInventoryRestriction(player, stack);
        return restriction != null && restriction.isDisabled(Attributes.STORING_IN_INVENTORY);
    }

    public static boolean isUnknownEquipmentItem(ServerPlayer player, ItemStack stack) {
        ABaseItemRestriction<?, ?> restriction = getEquipmentRestriction(player, stack);
        return restriction != null && restriction.isDisabled(Attributes.EQUIPPING);
    }

    public static boolean isUnknownPickupItem(ServerPlayer player, ItemStack stack) {
        ABaseItemRestriction<?, ?> restriction = getPickupRestriction(player, stack);
        return restriction != null && restriction.isDisabled(Attributes.PICKUP);
    }

    public static boolean isStillUnknownItem(ServerPlayer player, ItemStack stack) {
        return getItemRestriction(player, stack) != null;
    }

    private static ABaseItemRestriction<?, ?> getInventoryRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getInventoryRestriction(AHolder.serverAndPlayer(player), stack);
    }

    private static ABaseItemRestriction<?, ?> getEquipmentRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getEquipmentRestriction(AHolder.serverAndPlayer(player), stack);
    }

    private static ABaseItemRestriction<?, ?> getPickupRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getRestriction(AHolder.player(player), stack);
    }

    private static ABaseItemRestriction<?, ?> getItemRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getRestriction(AHolder.serverAndPlayer(player), stack);
    }

}
