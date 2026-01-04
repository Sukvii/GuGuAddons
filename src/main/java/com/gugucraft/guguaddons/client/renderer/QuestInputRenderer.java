package com.gugucraft.guguaddons.client.renderer;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class QuestInputRenderer extends KineticBlockEntityRenderer {
    public QuestInputRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected BlockState getRenderedBlockState(KineticBlockEntity be) {
        return AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, be.getBlockState().getValue(QuestInputBlock.FACING).getAxis());
    }
}
