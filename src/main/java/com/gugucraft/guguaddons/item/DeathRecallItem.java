package com.gugucraft.guguaddons.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;

import java.util.List;

public class DeathRecallItem extends Item {
    public DeathRecallItem(Properties properties) {
        super(properties);
    }

    public static void saveDeathLocation(ItemStack stack, GlobalPos pos) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("DeathDim", pos.dimension().location().toString());
        tag.putInt("DeathX", pos.pos().getX());
        tag.putInt("DeathY", pos.pos().getY());
        tag.putInt("DeathZ", pos.pos().getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static GlobalPos getDeathLocation(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null)
            return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("DeathDim"))
            return null;

        ResourceLocation dimLoc = ResourceLocation.parse(tag.getString("DeathDim"));
        int x = tag.getInt("DeathX");
        int y = tag.getInt("DeathY");
        int z = tag.getInt("DeathZ");

        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimLoc),
                new net.minecraft.core.BlockPos(x, y, z));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (getDeathLocation(stack) != null) {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        } else {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.recall_no_location").withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            GlobalPos pos = getDeathLocation(stack);
            if (pos != null) {
                ServerLevel targetLevel = player.getServer().getLevel(pos.dimension());
                if (targetLevel != null) {
                    player.teleportTo(targetLevel, pos.pos().getX() + 0.5, pos.pos().getY(), pos.pos().getZ() + 0.5,
                            player.getYRot(), player.getXRot());
                    player.displayClientMessage(
                            Component.translatable("message.guguaddons.recall_teleporting")
                                    .withStyle(ChatFormatting.GREEN),
                            true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.guguaddons.recall_dimension_not_found")
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60; // 3 seconds
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            Style CREATE_GOLD = Style.EMPTY.withColor(0xC7954B);

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.summary")
                    .withStyle(CREATE_GOLD));

            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.condition1")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.behaviour1")
                    .withStyle(CREATE_GOLD));

            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.condition2")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.behaviour2")
                    .withStyle(CREATE_GOLD));

            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.flavor")
                    .withStyle(ChatFormatting.DARK_PURPLE)
                    .withStyle(ChatFormatting.ITALIC));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.guguaddons.hold_for_description")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        GlobalPos pos = getDeathLocation(stack);
        if (pos != null) {
            tooltipComponents
                    .add(Component.translatable("tooltip.guguaddons.recall_location", pos.pos().toShortString())
                            .withStyle(ChatFormatting.GRAY));
            tooltipComponents
                    .add(Component
                            .translatable("tooltip.guguaddons.recall_dimension", pos.dimension().location().toString())
                            .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(
                    Component.translatable("tooltip.guguaddons.recall_no_location").withStyle(ChatFormatting.GRAY));
        }
    }
}
