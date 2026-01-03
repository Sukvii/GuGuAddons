package com.gugucraft.guguaddons.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GuGuDungeonChunkGenerator extends ChunkGenerator {

    public static final MapCodec<GuGuDungeonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(GuGuDungeonChunkGenerator::getBiomeSource))
            .apply(instance, GuGuDungeonChunkGenerator::new));

    public GuGuDungeonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random,
            ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    public static boolean isTeleportRoom(long worldSeed, int chunkX, int chunkZ) {
        // Deterministic randomness
        RandomSource random = new LegacyRandomSource(worldSeed);
        random.setSeed(worldSeed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));
        return random.nextInt(100) == 0;
    }

    public static boolean isRoom(long worldSeed, int chunkX, int chunkZ) {
        if (isTeleportRoom(worldSeed, chunkX, chunkZ))
            return true;
        RandomSource cellRandom = new LegacyRandomSource(
                worldSeed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));
        return cellRandom.nextFloat() < 0.4f;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {

        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        int worldXBase = chunkPos.getMinBlockX();
        int worldZBase = chunkPos.getMinBlockZ();

        long worldSeed = 0; // consistent visual seed

        // Map layout
        boolean amIRoom = isRoom(worldSeed, chunkX, chunkZ);
        boolean isTeleport = isTeleportRoom(worldSeed, chunkX, chunkZ);

        // --- Neighbors Status (Room or Not) ---
        boolean roomN = isRoom(worldSeed, chunkX, chunkZ - 1);
        boolean roomS = isRoom(worldSeed, chunkX, chunkZ + 1);
        boolean roomW = isRoom(worldSeed, chunkX - 1, chunkZ);
        boolean roomE = isRoom(worldSeed, chunkX + 1, chunkZ);

        // --- Raw Connections (Random Hash) ---
        boolean rawN = hasConnection(worldSeed, chunkX, chunkZ - 1, false);
        boolean rawS = hasConnection(worldSeed, chunkX, chunkZ, false);
        boolean rawW = hasConnection(worldSeed, chunkX - 1, chunkZ, true);
        boolean rawE = hasConnection(worldSeed, chunkX, chunkZ, true);

        // --- Forced Connections (Anti-Isolation) ---
        // We calculate if neighbors (if they are rooms) would force a connection to ME.
        // Or if I (if I am a room) would force a connection to THEM.

        // My outgoing forces (only relevant if I am a room)
        boolean myForceN = amIRoom && roomN
                && isIsolated(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE, rawN, rawS, rawW, rawE)
                && getBackupDirection(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE) == 0;
        boolean myForceS = amIRoom && roomS
                && isIsolated(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE, rawN, rawS, rawW, rawE)
                && getBackupDirection(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE) == 1;
        boolean myForceW = amIRoom && roomW
                && isIsolated(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE, rawN, rawS, rawW, rawE)
                && getBackupDirection(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE) == 2;
        boolean myForceE = amIRoom && roomE
                && isIsolated(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE, rawN, rawS, rawW, rawE)
                && getBackupDirection(worldSeed, chunkX, chunkZ, roomN, roomS, roomW, roomE) == 3;

        // Neighbor outgoing forces (they force link to me)
        boolean neighborForceN = roomN && isNeighborIsolated(worldSeed, chunkX, chunkZ - 1, 1);
        boolean neighborForceS = roomS && isNeighborIsolated(worldSeed, chunkX, chunkZ + 1, 0);
        boolean neighborForceW = roomW && isNeighborIsolated(worldSeed, chunkX - 1, chunkZ, 3);
        boolean neighborForceE = roomE && isNeighborIsolated(worldSeed, chunkX + 1, chunkZ, 2);

        // --- Final Door/Bridge Inputs ---
        // A connection exists on a side if:
        // 1. Random Hash says YES.
        // 2. OR Anti-Isolation forces YES.

        // Note: Logic for Void Chunks ("Bridges")
        // If I am Void, I don't initiate connections.
        // BUT if a Neighbor is a Room and it opens a door to me (due to Raw Hash), I
        // must accept it.
        // Wait, 'Anti-Isolation' logic checks "if neighbor is valid (Room)".
        // If I am Void, my neighbor Room sees me as Invalid. It won't trying to
        // force-connect to me to save itself.
        // But it MIGHT connect to me via Raw Hash.
        // IF Raw Hash is true, the Room opens a door to Void.
        // Therefore, I (Void) must build a bridge.

        boolean connN, connS, connW, connE;

        if (amIRoom) {
            // Room-to-Room: Keep existing logic (Raw + Force)
            // Room-to-Void: Connect if Void is valid bridge OR if I am desperate (Island)
            boolean roomAtN2 = isRoom(worldSeed, chunkX, chunkZ - 2);
            boolean roomAtS2 = isRoom(worldSeed, chunkX, chunkZ + 2);
            boolean roomAtW2 = isRoom(worldSeed, chunkX - 2, chunkZ);
            boolean roomAtE2 = isRoom(worldSeed, chunkX + 2, chunkZ);

            boolean strictIso = !roomN && !roomS && !roomW && !roomE &&
                    !roomAtN2 && !roomAtS2 && !roomAtW2 && !roomAtE2;

            connN = roomN ? (rawN || myForceN || neighborForceN) : (roomAtN2 || strictIso);
            connS = roomS ? (rawS || myForceS || neighborForceS) : roomAtS2;
            connW = roomW ? (rawW || myForceW || neighborForceW) : roomAtW2;
            connE = roomE ? (rawE || myForceE || neighborForceE) : roomAtE2;
        } else {
            // Void-to-Void / Void-to-Room:
            // I become a Bridge if sandwiched between Rooms OR if neighbor Room is
            // desperate (Island)
            boolean desperateN = roomN && isStrictlyIsolated(worldSeed, chunkX, chunkZ - 1);
            boolean desperateS = roomS && isStrictlyIsolated(worldSeed, chunkX, chunkZ + 1);
            boolean desperateW = roomW && isStrictlyIsolated(worldSeed, chunkX - 1, chunkZ);
            boolean desperateE = roomE && isStrictlyIsolated(worldSeed, chunkX + 1, chunkZ);

            boolean bridgeNS = (roomN && roomS) || desperateN || desperateS;
            boolean bridgeWE = (roomW && roomE) || desperateW || desperateE;

            connN = bridgeNS;
            connS = bridgeNS;
            connW = bridgeWE;
            connE = bridgeWE;
        }

        // If I am a Room, I need the neighbor to be a Room to open a door?
        // No, if neighbor is Void, I might still open a door (Raw Hash).
        // If I open a door to Void, the Void chunk sees "conn" is true and builds a
        // bridge match.

        // Allow Void-to-Void connections (Bridges)
        // If 'conn' is true (due to Raw Hash or Force), we honor it.
        // This ensures that if Hash says "Connect N-S", and both chunks are Void, they
        // both build their half of the bridge.

        boolean activeN = connN;
        boolean activeS = connS;
        boolean activeW = connW;
        boolean activeE = connE;

        // If I am Void and have NO active connections, I remain real Void.
        if (!amIRoom && !activeN && !activeS && !activeW && !activeE) {
            return CompletableFuture.completedFuture(chunk);
        }

        // If I am Void but have One connection? Dead end bridge.
        // Two connections? Corridor.

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockState floorBlock = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState wallBlock = Blocks.STONE_BRICKS.defaultBlockState();

        int roomType = 0;
        if (!amIRoom) {
            // I am a Bridge/Corridor
            roomType = 0;
        } else {
            RandomSource cellRandom = new LegacyRandomSource(
                    worldSeed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));
            roomType = cellRandom.nextInt(5);
            if (isTeleport)
                roomType = 0;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = worldXBase + x;
                int worldZ = worldZBase + z;

                boolean isBlock = false;
                boolean isWall = false;

                if (amIRoom) {
                    // Enlarged Room: Walls at 0 and 15 (Internally 14x14)
                    boolean isWallX = (x == 0 || x == 15);
                    boolean isWallZ = (z == 0 || z == 15);
                    boolean isCorner = isWallX && isWallZ;
                    isWall = isWallX || isWallZ;

                    if (isWall) {
                        // Openings aligned with Corridor walking path (6-8)
                        if (activeN && z == 0 && !isCorner && x >= 6 && x <= 8)
                            isWall = false;
                        if (activeS && z == 15 && !isCorner && x >= 6 && x <= 8)
                            isWall = false;
                        if (activeW && x == 0 && !isCorner && z >= 6 && z <= 8)
                            isWall = false;
                        if (activeE && x == 15 && !isCorner && z >= 6 && z <= 8)
                            isWall = false;
                    }
                    isBlock = !isWall; // Inside
                } else {
                    // Bridge / Corridor Logic
                    boolean activeNS = activeN; // Synced N/S
                    boolean activeWE = activeW; // Synced W/E

                    boolean inNS = (x >= 5 && x <= 9);
                    boolean inWE = (z >= 5 && z <= 9);

                    boolean shapeNS = activeNS && inNS;
                    boolean shapeWE = activeWE && inWE;

                    if (shapeNS || shapeWE) {
                        isBlock = true;

                        // Walls at edges of the corridor (5 and 9)
                        boolean wallNS = shapeNS && (x == 5 || x == 9);
                        boolean wallWE = shapeWE && (z == 5 || z == 9);

                        // Cut walls only if the crossing path is active
                        boolean cutWE = activeWE && (z >= 6 && z <= 8);
                        boolean cutNS = activeNS && (x >= 6 && x <= 8);

                        isWall = (wallNS && !cutWE) || (wallWE && !cutNS);
                    }
                }

                if (isWall) {
                    for (int y = 0; y <= 6; y++) {
                        chunk.setBlockState(mutablePos.set(worldX, y, worldZ), wallBlock, false);
                    }
                } else if (isBlock) {
                    // Bridge/Room Floor
                    if (amIRoom && !isChasm(roomType, x, z)) {
                        chunk.setBlockState(mutablePos.set(worldX, 0, worldZ), floorBlock, false);
                    } else if (!amIRoom) {
                        chunk.setBlockState(mutablePos.set(worldX, 0, worldZ), floorBlock, false);
                    }

                    // No internal decorations
                    if (isTeleport && x == 7 && z == 7) {
                        chunk.setBlockState(mutablePos.set(worldX, 1, worldZ),
                                com.gugucraft.guguaddons.registry.ModBlocks.DIMENSIONAL_TELEPORT_BLOCK.get()
                                        .defaultBlockState(),
                                false);
                    }
                }
            }
        }

        // Update heightmaps
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).update(x, 0, z,
                        Blocks.STONE_BRICKS.defaultBlockState());
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).update(x, 0, z,
                        Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    // Detects if a room at (cx, cz) has NO Room neighbors at distance 1 OR 2.
    // This room is an island and needs rescuing.
    private boolean isStrictlyIsolated(long seed, int cx, int cz) {
        // Need to check Distance 1
        boolean rN = isRoom(seed, cx, cz - 1);
        boolean rS = isRoom(seed, cx, cz + 1);
        boolean rW = isRoom(seed, cx - 1, cz);
        boolean rE = isRoom(seed, cx + 1, cz);

        if (rN || rS || rW || rE)
            return false; // Has dist 1 neighbor

        // Need to check Distance 2
        boolean rN2 = isRoom(seed, cx, cz - 2);
        boolean rS2 = isRoom(seed, cx, cz + 2);
        boolean rW2 = isRoom(seed, cx - 2, cz);
        boolean rE2 = isRoom(seed, cx + 2, cz);

        if (rN2 || rS2 || rW2 || rE2)
            return false; // Has dist 2 neighbor

        return true; // No friends anywhere close
    }

    // Checks if a chunk has NO random connections to valid rooms
    private boolean isIsolated(long seed, int cx, int cz, boolean rN, boolean rS, boolean rW, boolean rE, boolean rawN,
            boolean rawS, boolean rawW, boolean rawE) {
        if (rN && rawN)
            return false;
        if (rS && rawS)
            return false;
        if (rW && rawW)
            return false;
        if (rE && rawE)
            return false;
        return true;
    }

    // Determines which neighbor we should FORCE connect to if isolated. (0=N, 1=S,
    // 2=W, 3=E)
    // Deterministic priority: N > S > W > E
    private int getBackupDirection(long seed, int cx, int cz, boolean rN, boolean rS, boolean rW, boolean rE) {
        if (rN)
            return 0;
        if (rS)
            return 1;
        if (rW)
            return 2;
        if (rE)
            return 3;
        return -1; // Should not happen if we only call this when at least one neighbor exists.
    }

    // Checks if the neighbor at (NX, NZ) is isolated and would choose 'requiredDir'
    // as its backup
    // requiredDir is looking from the neighbor's perspective (e.g. 1=South)
    private boolean isNeighborIsolated(long seed, int nx, int nz, int requiredDir) {
        // Need to fully simulate neighbor's context
        boolean nrN = isRoom(seed, nx, nz - 1);
        boolean nrS = isRoom(seed, nx, nz + 1);
        boolean nrW = isRoom(seed, nx - 1, nz);
        boolean nrE = isRoom(seed, nx + 1, nz);

        if (!nrN && !nrS && !nrW && !nrE)
            return false; // Neighbor is an island itself, can't connect? Or we connect anyway?

        boolean nrawN = hasConnection(seed, nx, nz - 1, false);
        boolean nrawS = hasConnection(seed, nx, nz, false);
        boolean nrawW = hasConnection(seed, nx - 1, nz, true);
        boolean nrawE = hasConnection(seed, nx, nz, true);

        boolean iso = isIsolated(seed, nx, nz, nrN, nrS, nrW, nrE, nrawN, nrawS, nrawW, nrawE);
        if (!iso)
            return false;

        return getBackupDirection(seed, nx, nz, nrN, nrS, nrW, nrE) == requiredDir;
    }

    private boolean hasConnection(long seed, int x, int z, boolean isEast) {
        // 40% chance to have a connection
        RandomSource rnd = new LegacyRandomSource(
                seed ^ ((long) x * 341873128712L + (long) z * 132897987541L + (isEast ? 10 : 20)));
        return rnd.nextFloat() < 0.4f;
    }

    private boolean isChasm(int type, int x, int z) {
        if (type == 2) { // Ring Chasm
            // Center is 7,7
            int dx = Math.abs(x - 7);
            int dz = Math.abs(z - 7);
            // Don't remove center, remove ring around it
            if (dx >= 2 && dx <= 4 && dz >= 2 && dz <= 4 && !(dx == 2 && dz == 2))
                return true; // Blocky ring
        }
        return false;
    }

}
