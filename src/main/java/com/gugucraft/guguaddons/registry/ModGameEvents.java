package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameEvents {
    private static final DeferredRegister<GameEvent> GAME_EVENTS = DeferredRegister.create(Registries.GAME_EVENT, GuGuAddons.MODID);
    public static final DeferredHolder<GameEvent, GameEvent> ABYSS_SETTLING = GAME_EVENTS.register("abyss_settling", () -> new GameEvent(16));
    public static final DeferredHolder<GameEvent, GameEvent> ABYSS_ACTIVATING = GAME_EVENTS.register("abyss_activating", () -> new GameEvent(16));
    public static final DeferredHolder<GameEvent, GameEvent> ABYSS_CONDENSING = GAME_EVENTS.register("abyss_condensing", () -> new GameEvent(16));

    public static void register(IEventBus eventBus) {
        GAME_EVENTS.register(eventBus);
    }
}
