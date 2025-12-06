package com.gugucraft.guguaddons.event;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.getItem() instanceof SwordItem && right.getItem() == ModItems.GEM_ITEM.get()) {
            CustomData gemData = right.get(DataComponents.CUSTOM_DATA);
            if (gemData != null && gemData.contains("GemAttributes")) {
                ItemStack output = left.copy();
                CustomData outputData = output.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag outputTag = outputData.copyTag();

                ListTag socketedGems = new ListTag();

                // Add the gem's attributes to the list
                CompoundTag gemAttributes = gemData.copyTag().getCompound("GemAttributes");
                socketedGems.add(gemAttributes);

                outputTag.put("SocketedGems", socketedGems);
                output.set(DataComponents.CUSTOM_DATA, CustomData.of(outputTag));

                event.setOutput(output);
                event.setCost(1);
                event.setMaterialCost(1);
            }
        }
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("SocketedGems")) {
            CompoundTag tag = customData.copyTag();
            ListTag socketedGems = tag.getList("SocketedGems", Tag.TAG_COMPOUND);

            for (int i = 0; i < socketedGems.size(); i++) {
                CompoundTag gemAttributes = socketedGems.getCompound(i);
                for (String key : gemAttributes.getAllKeys()) {
                    double value = gemAttributes.getDouble(key);

                    // We need to find the Attribute Holder from the registry
                    ResourceLocation id = ResourceLocation.parse(key);
                    Holder<Attribute> attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);

                    if (attributeHolder != null) {

                        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID,
                                "gem_modifier_" + i + "_" + key.replace(":", "_"));

                        event.addModifier(attributeHolder,
                                new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.bySlot(EquipmentSlot.MAINHAND));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("SocketedGems")) {
            CompoundTag tag = customData.copyTag();
            ListTag socketedGems = tag.getList("SocketedGems", Tag.TAG_COMPOUND);

            if (!socketedGems.isEmpty()) {
                event.getToolTip().add(Component.empty());
                event.getToolTip()
                        .add(Component.translatable("tooltip.guguaddons.socketed_gems").withStyle(ChatFormatting.GOLD));

                for (int i = 0; i < socketedGems.size(); i++) {
                    CompoundTag gemAttributes = socketedGems.getCompound(i);
                    for (String key : gemAttributes.getAllKeys()) {
                        double value = gemAttributes.getDouble(key);
                        String translationKey = "attribute.name." + key;
                        event.getToolTip()
                                .add(Component.literal(" - " + String.format("%.1f", value) + " ")
                                        .append(Component.translatable(translationKey))
                                        .withStyle(ChatFormatting.BLUE));
                    }
                }
            }
        }
    }
}
