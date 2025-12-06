package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModPOIs {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE,
            GuGuAddons.MODID);

    public static final DeferredHolder<PoiType, PoiType> TEST_PORTAL = POI_TYPES.register("test_portal",
            () -> new PoiType(
                    Set.copyOf(ModBlocks.TEST_PORTAL.get().getStateDefinition().getPossibleStates()),
                    0, 1));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
