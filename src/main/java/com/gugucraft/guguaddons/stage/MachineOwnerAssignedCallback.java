package com.gugucraft.guguaddons.stage;

/**
 * Implemented by block entities that need to react the moment their machine owner is assigned
 * (e.g. when a schematicannon prints them), in addition to the normal NBT load path.
 */
public interface MachineOwnerAssignedCallback {
    void guguaddons$onMachineOwnerAssigned();
}
