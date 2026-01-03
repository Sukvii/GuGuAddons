package com.gugucraft.guguaddons.item;

import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GemBlankItem extends Item {
    private static final String[] ATTRIBUTES = {
            "generic.attack_damage",
            "generic.attack_speed",
            "generic.armor",
            "generic.armor_toughness"
    };

    private final Random random = new Random();

    public GemBlankItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            /*
            // Create the Gem Item
            ItemStack gemStack = new ItemStack(ModItems.GEM_ITEM.get());

            // Generate Random Attributes
            CompoundTag rootTag = new CompoundTag();
            CompoundTag attributesTag = new CompoundTag();

            int attributeCount = random.nextInt(4) + 1; // 1 to 4 attributes
            List<String> selectedAttributes = new ArrayList<>();
            Collections.addAll(selectedAttributes, ATTRIBUTES);
            Collections.shuffle(selectedAttributes);

            for (int i = 0; i < attributeCount; i++) {
                String attr = selectedAttributes.get(i);
                double value = generateValue(attr);
                attributesTag.putDouble(attr, value);
            }

            rootTag.put("GemAttributes", attributesTag);
            gemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));

            // If stack > 1, decrement, add gem to inventory.

            if (stack.getCount() > 1) {
                stack.shrink(1);
                if (!player.getInventory().add(gemStack)) {
                    player.drop(gemStack, false);
                }
                // Play sound only once per tick/batch?
            } else {
                player.getInventory().setItem(slotId, gemStack);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            */
        }
    }

    private double generateValue(String attribute) {
        return switch (attribute) {
            case "generic.attack_damage" -> 1.0 + random.nextDouble() * 4.0; // 1.0 - 5.0
            case "generic.attack_speed" -> 0.1 + random.nextDouble() * 0.9; // 0.1 - 1.0
            case "generic.armor" -> 1.0 + random.nextDouble() * 4.0; // 1.0 - 5.0
            case "generic.armor_toughness" -> 1.0 + random.nextDouble() * 2.0; // 1.0 - 3.0
            default -> 0.0;
        };
    }
}
