package com.gugucraft.guguaddons.stage;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Implemented (via mixin) by Create's {@code LaunchedItem} so the schematicannon owner can travel
 * with the in-flight block independently of its {@code data} NBT tag.
 */
public interface LaunchedItemOwnerAccess {
    @Nullable
    UUID guguaddons$getLaunchedOwner();

    void guguaddons$setLaunchedOwner(@Nullable UUID ownerId);
}
