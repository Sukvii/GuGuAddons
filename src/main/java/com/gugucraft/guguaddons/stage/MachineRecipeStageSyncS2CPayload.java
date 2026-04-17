package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public record MachineRecipeStageSyncS2CPayload(List<MachineRecipeStageRestriction> restrictions)
        implements CustomPacketPayload {
    public static final Type<MachineRecipeStageSyncS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "machine_recipe_stage_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipeStageSyncS2CPayload> STREAM_CODEC =
            StreamCodec.of(MachineRecipeStageSyncS2CPayload::write, MachineRecipeStageSyncS2CPayload::read);

    public MachineRecipeStageSyncS2CPayload {
        restrictions = List.copyOf(restrictions);
    }

    @Override
    public Type<MachineRecipeStageSyncS2CPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, MachineRecipeStageSyncS2CPayload payload) {
        buffer.writeVarInt(payload.restrictions.size());
        for (MachineRecipeStageRestriction restriction : payload.restrictions) {
            buffer.writeResourceLocation(BuiltInRegistries.RECIPE_TYPE.getKey(restriction.recipeType()));
            buffer.writeUtf(restriction.stageId());
            buffer.writeVarInt(restriction.recipeIds().size());
            for (ResourceLocation recipeId : restriction.recipeIds()) {
                buffer.writeResourceLocation(recipeId);
            }
        }
    }

    private static MachineRecipeStageSyncS2CPayload read(RegistryFriendlyByteBuf buffer) {
        int restrictionCount = buffer.readVarInt();
        List<MachineRecipeStageRestriction> restrictions = new ArrayList<>(restrictionCount);
        for (int i = 0; i < restrictionCount; i++) {
            RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(buffer.readResourceLocation());
            String stageId = buffer.readUtf();
            int recipeCount = buffer.readVarInt();
            List<ResourceLocation> recipeIds = new ArrayList<>(recipeCount);
            for (int j = 0; j < recipeCount; j++) {
                recipeIds.add(buffer.readResourceLocation());
            }
            restrictions.add(new MachineRecipeStageRestriction(stageId, recipeType, recipeIds));
        }
        return new MachineRecipeStageSyncS2CPayload(restrictions);
    }
}
