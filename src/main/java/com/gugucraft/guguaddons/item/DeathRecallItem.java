package com.gugucraft.guguaddons.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

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
        if (!tag.contains("DeathDim", Tag.TAG_STRING)
                || !tag.contains("DeathX", Tag.TAG_INT)
                || !tag.contains("DeathY", Tag.TAG_INT)
                || !tag.contains("DeathZ", Tag.TAG_INT)) {
            return null;
        }

        ResourceLocation dimLoc = ResourceLocation.tryParse(tag.getString("DeathDim"));
        if (dimLoc == null) {
            return null;
        }
        int x = tag.getInt("DeathX");
        int y = tag.getInt("DeathY");
        int z = tag.getInt("DeathZ");

        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimLoc),
                new net.minecraft.core.BlockPos(x, y, z));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        // Check durability (Max Damage - current Damage <= 1 means 0 usable durability
        // left)
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.slash_back_terminal_no_durability")
                                .withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        GlobalPos pos = getDeathLocation(stack);
        if (pos == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.recall_no_location").withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            MinecraftServer server = player.getServer();
            if (server == null || server.getLevel(pos.dimension()) == null) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.recall_dimension_not_found")
                                .withStyle(ChatFormatting.RED),
                        true);
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            GlobalPos pos = getDeathLocation(stack);
            if (pos == null) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.recall_no_location").withStyle(ChatFormatting.RED),
                        true);
                return stack;
            }

            MinecraftServer server = player.getServer();
            ServerLevel targetLevel = server == null ? null : server.getLevel(pos.dimension());
            if (targetLevel == null) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.recall_dimension_not_found")
                                .withStyle(ChatFormatting.RED),
                        true);
                return stack;
            }

            player.teleportTo(targetLevel, pos.pos().getX() + 0.5, pos.pos().getY(), pos.pos().getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            player.displayClientMessage(
                    Component.translatable("message.guguaddons.recall_teleporting")
                            .withStyle(ChatFormatting.GREEN),
                    true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            // Consume durability
            if (!player.getAbilities().instabuild) {
                stack.setDamageValue(Math.min(stack.getDamageValue() + 1, stack.getMaxDamage() - 1));
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
}
