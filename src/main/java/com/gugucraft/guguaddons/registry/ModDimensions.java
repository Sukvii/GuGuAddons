package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> TEST_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "test"));

    public static final ResourceKey<DimensionType> TEST_DIMENSION_TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "test_type"));

    public static void register() {
        System.out.println("Registering ModDimensions for " + GuGuAddons.MODID);
    }
}
