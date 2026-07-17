package com.gugucraft.guguaddons.compat.ftbchunks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.compat.numismatics.NumismaticsAccountHelper;
import com.gugucraft.guguaddons.config.Config;

import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class ChunkClaimEconomyHandler {
    private static final Map<UUID, PendingClaimSession> PENDING_SESSIONS = new HashMap<>();
    private static final Map<UUID, Set<ChunkKey>> BYPASS_CLAIMS = new HashMap<>();
    private static final AtomicInteger NEXT_SESSION_ID = new AtomicInteger(1);

    private static final String MSG_CONFIRM_REQUIRED = "message.guguaddons.chunk_claim_confirmation_required";
    private static final String MSG_CONFIRM_CANCELLED = "message.guguaddons.chunk_claim_confirmation_cancelled";
    private static final String MSG_CONFIRM_TIMEOUT = "message.guguaddons.chunk_claim_confirmation_timeout";
    private static final String MSG_NO_VALID_CHUNKS = "message.guguaddons.chunk_claim_no_valid_targets";
    private static final String MSG_TEAM_NOT_FOUND = "message.guguaddons.chunk_claim_team_not_found";
    private static final String MSG_NOT_ENOUGH_FUNDS = "message.guguaddons.chunk_claim_insufficient_funds";
    private static final String MSG_CLAIM_RESULT = "message.guguaddons.chunk_claim_result";
    private static final String MSG_REFUND_RECEIVED = "message.guguaddons.chunk_refund_received";

    private static volatile boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }

        ClaimedChunkEvent.BEFORE_CLAIM.register(ChunkClaimEconomyHandler::onBeforeClaim);
        ClaimedChunkEvent.AFTER_UNCLAIM.register(ChunkClaimEconomyHandler::onAfterUnclaim);
        initialized = true;
    }

    private static CompoundEventResult<ClaimResult> onBeforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        int unitPrice = getClaimPrice();
        if (unitPrice <= 0 || !(source.getEntity() instanceof ServerPlayer player)) {
            return CompoundEventResult.pass();
        }

        ChunkKey key = ChunkKey.from(chunk.getPos());
        Set<ChunkKey> bypass = BYPASS_CLAIMS.get(player.getUUID());
        if (bypass != null && bypass.remove(key)) {
            if (bypass.isEmpty()) {
                BYPASS_CLAIMS.remove(player.getUUID());
            }
            return CompoundEventResult.pass();
        }

        long tick = player.serverLevel().getServer().getTickCount();
        PendingClaimSession session = PENDING_SESSIONS.get(player.getUUID());
        UUID teamId = chunk.getTeamData().getTeam().getId();

        boolean mustCreateSession = session == null
                || session.prompted
                || !session.teamId.equals(teamId)
                || tick - session.captureStartTick > 1;

        if (mustCreateSession) {
            session = new PendingClaimSession(
                    NEXT_SESSION_ID.getAndIncrement(),
                    player.getUUID(),
                    teamId,
                    tick);
            PENDING_SESSIONS.put(player.getUUID(), session);
        }

        session.claims.add(key);
        session.promptAtTick = tick + 1;

        return CompoundEventResult.interruptFalse(ClaimResult.customProblem(MSG_CONFIRM_REQUIRED));
    }

    private static void onAfterUnclaim(CommandSourceStack source, ClaimedChunk chunk) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            return;
        }

        ChunkClaimEconomySavedData savedData = ChunkClaimEconomySavedData.get(server);
        Optional<ChunkClaimEconomySavedData.ClaimPayment> removed = savedData.removeClaim(chunk.getPos());
        if (removed.isEmpty()) {
            return;
        }

        double ratio = getUnclaimRefundRatio();
        if (ratio <= 0D) {
            return;
        }

        ChunkClaimEconomySavedData.ClaimPayment payment = removed.get();
        long refund = (long) Math.floor(payment.paidAmount() * ratio);
        if (refund <= 0) {
            return;
        }
        savedData.addPendingRefund(payment.payerId(), refund);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        flushPendingRefunds(server);

        if (PENDING_SESSIONS.isEmpty()) {
            return;
        }

        long tick = server.getTickCount();
        int timeout = getConfirmTimeoutTicks();

        Iterator<PendingClaimSession> iterator = PENDING_SESSIONS.values().iterator();
        while (iterator.hasNext()) {
            PendingClaimSession session = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!session.prompted && tick >= session.promptAtTick) {
                session.prompted = true;
                session.promptedTick = tick;
                ChunkClaimEconomyNetwork.sendClaimPrompt(player, session.id, session.claims.size(),
                        safeMultiply(getClaimPrice(), session.claims.size()));
                continue;
            }

            if (session.prompted && tick - session.promptedTick > timeout) {
                ChunkClaimEconomyNetwork.sendToast(player, MSG_CONFIRM_TIMEOUT);
                iterator.remove();
            }
        }
    }

    private static void flushPendingRefunds(MinecraftServer server) {
        ChunkClaimEconomySavedData savedData = ChunkClaimEconomySavedData.get(server);
        for (Map.Entry<UUID, Long> entry : savedData.getPendingRefundsSnapshot().entrySet()) {
            long totalRefund = entry.getValue();
            if (totalRefund <= 0L) {
                continue;
            }

            UUID payerId = entry.getKey();
            BankAccount account = Numismatics.BANK.getOrCreateAccount(payerId, BankAccount.Type.PLAYER);
            int deposited = NumismaticsAccountHelper.depositUpToCapacity(account, totalRefund);
            if (deposited <= 0) {
                continue;
            }
            savedData.consumePendingRefund(payerId, deposited);

            ServerPlayer payer = server.getPlayerList().getPlayer(payerId);
            if (payer != null) {
                ChunkClaimEconomyNetwork.sendToast(payer, MSG_REFUND_RECEIVED, formatSpurs(deposited));
            }
        }
    }

    public static void confirmSession(ServerPlayer player, int sessionId) {
        PendingClaimSession session = PENDING_SESSIONS.get(player.getUUID());
        if (session == null || session.id != sessionId) {
            return;
        }

        PENDING_SESSIONS.remove(player.getUUID());
        int unitPrice = getClaimPrice();
        if (unitPrice <= 0) {
            return;
        }

        Optional<Team> teamOpt = FTBTeamsAPI.api().getManager().getTeamByID(session.teamId);
        if (teamOpt.isEmpty()) {
            ChunkClaimEconomyNetwork.sendToast(player, MSG_TEAM_NOT_FOUND);
            return;
        }

        ChunkTeamData teamData = FTBChunksAPI.api().getManager().getOrCreateData(teamOpt.get());
        List<ChunkKey> targets = new ArrayList<>();
        for (ChunkKey key : session.claims) {
            if (FTBChunksAPI.api().getManager().getChunk(key.toChunkPos()) == null) {
                targets.add(key);
            }
        }

        if (targets.isEmpty()) {
            ChunkClaimEconomyNetwork.sendToast(player, MSG_NO_VALID_CHUNKS);
            return;
        }

        int maxCost = safeMultiply(unitPrice, targets.size());
        BankAccount account = Numismatics.BANK.getOrCreateAccount(player.getUUID(), BankAccount.Type.PLAYER);
        NumismaticsAccountHelper.repairNegativeBalance(account);
        if (!account.deduct(maxCost)) {
            ChunkClaimEconomyNetwork.sendToast(player, MSG_NOT_ENOUGH_FUNDS, formatSpurs(maxCost));
            return;
        }

        Set<ChunkKey> bypass = new HashSet<>(targets);
        BYPASS_CLAIMS.put(player.getUUID(), bypass);

        int successCount = 0;
        ChunkClaimEconomySavedData savedData = ChunkClaimEconomySavedData.get(player.serverLevel().getServer());
        try {
            CommandSourceStack source = player.createCommandSourceStack();
            for (ChunkKey key : targets) {
                ClaimResult result = teamData.claim(source, key.toChunkPos(), false);
                if (result != null && result.isSuccess()) {
                    successCount++;
                    savedData.recordClaim(key.toChunkPos(), player.getUUID(), unitPrice);
                }
            }
        } finally {
            BYPASS_CLAIMS.remove(player.getUUID());
        }

        int spent = safeMultiply(unitPrice, successCount);
        int refund = Math.max(0, maxCost - spent);
        if (refund > 0) {
            if (!NumismaticsAccountHelper.deposit(account, refund)) {
                savedData.addPendingRefund(player.getUUID(), refund);
            }
        }

        ChunkClaimEconomyNetwork.sendToast(
                player,
                MSG_CLAIM_RESULT,
                Integer.toString(successCount),
                Integer.toString(targets.size()),
                formatSpurs(spent));
    }

    public static void cancelSession(ServerPlayer player, int sessionId, boolean notify) {
        PendingClaimSession removed = PENDING_SESSIONS.get(player.getUUID());
        if (removed == null || removed.id != sessionId) {
            return;
        }

        PENDING_SESSIONS.remove(player.getUUID());
        if (notify) {
            ChunkClaimEconomyNetwork.sendToast(player, MSG_CONFIRM_CANCELLED);
        }
    }

    private static int getClaimPrice() {
        return Config.getConfiguredFtbChunksClaimPrice();
    }

    private static double getUnclaimRefundRatio() {
        return Config.getConfiguredFtbChunksUnclaimRefundRatio();
    }

    private static int getConfirmTimeoutTicks() {
        return Config.getConfiguredFtbChunksClaimConfirmTimeoutTicks();
    }

    private static int safeMultiply(int left, int right) {
        if (left <= 0 || right <= 0) {
            return 0;
        }
        long value = (long) left * right;
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static String formatSpurs(long amount) {
        return String.format(Locale.ROOT, "%,d sp", Math.max(0L, amount));
    }

    private static class PendingClaimSession {
        private final int id;
        private final UUID playerId;
        private final UUID teamId;
        private final LinkedHashSet<ChunkKey> claims = new LinkedHashSet<>();
        private final long captureStartTick;

        private long promptAtTick;
        private boolean prompted;
        private long promptedTick;

        private PendingClaimSession(int id, UUID playerId, UUID teamId, long tick) {
            this.id = id;
            this.playerId = playerId;
            this.teamId = teamId;
            this.captureStartTick = tick;
            this.promptAtTick = tick + 1;
            this.prompted = false;
            this.promptedTick = 0;
        }
    }

    private record ChunkKey(net.minecraft.resources.ResourceKey<Level> dimension, int x, int z) {
        private static ChunkKey from(ChunkDimPos pos) {
            return new ChunkKey(pos.dimension(), pos.x(), pos.z());
        }

        private ChunkDimPos toChunkPos() {
            return new ChunkDimPos(dimension, x, z);
        }
    }
}
