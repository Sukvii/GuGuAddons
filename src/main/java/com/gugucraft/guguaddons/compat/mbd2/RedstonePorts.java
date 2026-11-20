package com.gugucraft.guguaddons.compat.mbd2;

import com.lowdragmc.mbd2.api.blockentity.ProxyPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RedstonePorts {
    List<BlockPos> guguaddons$redstonePorts();

    @Nullable
    static ProxyPartBlockEntity portAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ProxyPartBlockEntity proxy && !proxy.getProxyCapabilities().isEmpty()
                ? proxy : null;
    }
}
