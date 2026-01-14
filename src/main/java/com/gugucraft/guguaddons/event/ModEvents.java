package com.gugucraft.guguaddons.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.item.DeathRecallItem;
import com.gugucraft.guguaddons.registry.ModItems;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class ModEvents {

    private static final String SAVED_ITEMS_TAG = "GuGuAddons_SavedItems";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GlobalPos deathPos = GlobalPos.of(player.level().dimension(), player.blockPosition());

            // Update item locations BEFORE they are dropped (this inventory is still full
            // here)
            player.getInventory().items.forEach(stack -> tryRecordDeath(stack, deathPos));
            player.getInventory().offhand.forEach(stack -> tryRecordDeath(stack, deathPos));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            List<ItemStack> savedItems = new ArrayList<>();
            Iterator<ItemEntity> iterator = event.getDrops().iterator();

            while (iterator.hasNext()) {
                ItemEntity drops = iterator.next();
                ItemStack stack = drops.getItem();

                if (stack.getItem() instanceof DeathRecallItem) {
                    savedItems.add(stack.copy());
                    iterator.remove(); // Remove from drops so it doesn't fall on ground
                }
            }

            // Save these items to the player's persistent data so we can recover them in
            // Clone
            if (!savedItems.isEmpty()) {
                CompoundTag persistentData = player.getPersistentData();
                ListTag listTag = new ListTag();
                for (ItemStack stack : savedItems) {
                    listTag.add(stack.saveOptional(player.registryAccess()));
                }
                persistentData.put(SAVED_ITEMS_TAG, listTag);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

            CompoundTag persistentData = original.getPersistentData();
            if (persistentData.contains(SAVED_ITEMS_TAG)) {
                ListTag listTag = persistentData.getList(SAVED_ITEMS_TAG, 10); // 10 = CompoundTag
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag itemTag = listTag.getCompound(i);
                    ItemStack stack = ItemStack.parseOptional(newPlayer.registryAccess(), itemTag);
                    if (!stack.isEmpty()) {
                        if (!newPlayer.getInventory().add(stack)) {
                            // If inventory full, drop at feet (safe fallback)
                            newPlayer.drop(stack, true);
                        }
                    }
                }
                // Clear the data after restoring
                persistentData.remove(SAVED_ITEMS_TAG);
            }
        }
    }

    private static void tryRecordDeath(ItemStack stack, GlobalPos pos) {
        if (stack.getItem() instanceof DeathRecallItem) {
            DeathRecallItem.saveDeathLocation(stack, pos);
        }
    }
}
