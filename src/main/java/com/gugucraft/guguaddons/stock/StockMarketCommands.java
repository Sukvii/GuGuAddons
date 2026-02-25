package com.gugucraft.guguaddons.stock;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.stock.ui.StockUiNetwork;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class StockMarketCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> rootBuilder = Commands.literal("stock")
                .requires(source -> source.hasPermission(0))
                .executes(context -> openMarket(context.getSource()));

        LiteralCommandNode<CommandSourceStack> stockRoot = event.getDispatcher().register(rootBuilder);
        event.getDispatcher().register(Commands.literal("stocks").redirect(stockRoot));
    }

    private static int openMarket(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.guguaddons.stock.players_only"));
            return 0;
        }
        StockUiNetwork.openFor(player);
        return Command.SINGLE_SUCCESS;
    }
}
