package com.gugucraft.guguaddons.compat.kubejs;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class MachineRecipeStageKubePlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(MachineRecipeStageKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type().isServer()) {
            bindings.add("GuGuMachineStages", Methods.class);
        }
    }

    public interface Methods {
        static String[] getSupportedRecipeTypes() {
            return MachineRecipeStageManager.supportedRecipeTypeIds().toArray(String[]::new);
        }
    }
}
