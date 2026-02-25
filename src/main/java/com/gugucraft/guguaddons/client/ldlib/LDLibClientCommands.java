package com.gugucraft.guguaddons.client.ldlib;

import com.gugucraft.guguaddons.GuGuAddons;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.brigadier.Command;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class LDLibClientCommands {
    private LDLibClientCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("gugu_ldlib_ui")
                        .executes(context -> openLDLibDemoScreen()));
    }

    private static int openLDLibDemoScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }

        minecraft.setScreen(new ModularUIScreen(
                LDLibUiFactory.createDemoUi(minecraft.player),
                Component.translatable("screen.guguaddons.ldlib.title")));
        return Command.SINGLE_SUCCESS;
    }
}
