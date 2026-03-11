package com.gugucraft.guguaddons.client;

import com.gugucraft.guguaddons.GuGuAddons;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class ModPartialModels {
    public static final PartialModel VACUUM_COG = block("vacuum_chamber/cog");
    public static final PartialModel VACUUM_PIPE = block("vacuum_chamber/head");
    public static final PartialModel VACUUM_CHAMBER_ARROWS = block("vacuum_chamber/arrows");
    public static final PartialModel CENTRIFUGE_BEAMS = block("centrifuge/head");
    public static final PartialModel CENTRIFUGE_BASIN = block("centrifuge/basin");

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "block/" + path));
    }

    public static void init() {
    }
}
