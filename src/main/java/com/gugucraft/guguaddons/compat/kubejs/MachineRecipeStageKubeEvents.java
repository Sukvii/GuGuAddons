package com.gugucraft.guguaddons.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public final class MachineRecipeStageKubeEvents {
    public static final EventGroup GROUP = EventGroup.of("GuGuMachineStageEvents");
    public static final EventHandler REGISTER = GROUP.server("register", () -> MachineRecipeStageKubeEvent.class);

    private MachineRecipeStageKubeEvents() {
    }
}
