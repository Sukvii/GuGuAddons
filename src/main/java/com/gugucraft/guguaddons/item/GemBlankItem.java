package com.gugucraft.guguaddons.item;



public class GemBlankItem extends Item {


    public GemBlankItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {

        }
    }


}
