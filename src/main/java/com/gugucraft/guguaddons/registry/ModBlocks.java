package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import com.gugucraft.guguaddons.block.custom.TestPortalBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GuGuAddons.MODID);

    public static final DeferredBlock<Block> TEST_PORTAL_FRAME = BLOCKS.register("test_portal_frame",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> TEST_PORTAL = BLOCKS.register("test_portal",
            () -> new TestPortalBlock(BlockBehaviour.Properties.of().noCollission().strength(-1.0F)
                    .sound(SoundType.GLASS).lightLevel((state) -> 11)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
