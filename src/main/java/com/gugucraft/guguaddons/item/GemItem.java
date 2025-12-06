package com.gugucraft.guguaddons.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class GemItem extends Item {
    public GemItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("GemAttributes")) {
                CompoundTag attributes = tag.getCompound("GemAttributes");
                tooltipComponents.add(
                        Component.translatable("tooltip.guguaddons.gem_attributes").withStyle(ChatFormatting.GRAY));

                for (String key : attributes.getAllKeys()) {
                    double value = attributes.getDouble(key);
                    // Vanilla keys: attribute.name.generic.attack_damage

                    String translationKey = "attribute.name." + key;
                    tooltipComponents.add(Component.literal(" + " + String.format("%.1f", value) + " ")
                            .append(Component.translatable(translationKey))
                            .withStyle(ChatFormatting.BLUE));
                }
            }
        }
    }

}
