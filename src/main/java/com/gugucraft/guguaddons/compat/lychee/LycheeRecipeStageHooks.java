package com.gugucraft.guguaddons.compat.lychee;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import snownee.lychee.context.LootParamsContext;
import snownee.lychee.util.context.LycheeContext;
import snownee.lychee.util.context.LycheeContextKey;

public final class LycheeRecipeStageHooks {
    private LycheeRecipeStageHooks() {
    }

    public static boolean canProcess(LycheeContext context, RecipeHolder<?> holder) {
        ServerPlayer player = getServerPlayer(context);
        if (player != null) {
            return MachineRecipeStageManager.canProcess(player, holder);
        }
        return MachineRecipeStageManager.canProcessGlobally(holder);
    }

    public static boolean canProcess(Entity entity, RecipeHolder<?> holder) {
        if (entity instanceof ServerPlayer player) {
            return MachineRecipeStageManager.canProcess(player, holder);
        }
        return MachineRecipeStageManager.canProcessGlobally(holder);
    }

    public static boolean canCraft(LycheeContext context, Level level, Recipe<?> recipe) {
        ServerPlayer player = getServerPlayer(context);
        if (player != null) {
            return MachineRecipeStageManager.canProcess(player, level, recipe);
        }
        return MachineRecipeStageManager.canProcessGlobally(level, recipe);
    }

    private static ServerPlayer getServerPlayer(LycheeContext context) {
        if (context == null || !context.has(LycheeContextKey.LOOT_PARAMS, false)) {
            return null;
        }

        LootParamsContext lootParams = context.get(LycheeContextKey.LOOT_PARAMS);
        Object entity = lootParams.getOrNull(LootContextParams.THIS_ENTITY);
        return entity instanceof ServerPlayer player ? player : null;
    }
}
