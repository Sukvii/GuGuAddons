package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.compat.astages.AStagesHelper;
import com.gugucraft.guguaddons.compat.kubejs.MachineRecipeStageKubeEvent;
import com.gugucraft.guguaddons.compat.kubejs.MachineRecipeStageKubeEvents;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MachineRecipeStageManager {
    private static final Map<RecipeType<?>, Map<ResourceLocation, String>> SERVER_RESTRICTIONS = new LinkedHashMap<>();
    private static final Map<RecipeType<?>, Set<ResourceLocation>> SERVER_EXPLICIT_RECIPES = new LinkedHashMap<>();
    private static final Map<RecipeType<?>, Map<ResourceLocation, String>> CLIENT_RESTRICTIONS = new LinkedHashMap<>();

    private static final Set<ResourceLocation> SUPPORTED_RECIPE_TYPE_IDS = Set.of(
            id("create", "crushing"),
            id("create", "cutting"),
            id("create", "milling"),
            id("create", "basin"),
            id("create", "mixing"),
            id("create", "compacting"),
            id("create", "pressing"),
            id("create", "sandpaper_polishing"),
            id("create", "splashing"),
            id("create", "haunting"),
            id("create", "deploying"),
            id("create", "filling"),
            id("create", "emptying"),
            id("create", "item_application"),
            id("create", "mechanical_crafting"),
            id("create", "sequenced_assembly"),
            id("minecraft", "smelting"),
            id("minecraft", "smoking"),
            id("minecraft", "blasting"),
            id(GuGuAddons.MODID, "vacuumizing"),
            id(GuGuAddons.MODID, "pressurizing"),
            id(GuGuAddons.MODID, "centrifugation"));

    private MachineRecipeStageManager() {
    }

    public static void reloadFromKubeJS(MinecraftServer server) {
        clearServer();
        MachineRecipeStageKubeEvents.REGISTER.post(new MachineRecipeStageKubeEvent());
        MachineRecipeStageNetwork.syncAll(server);
    }

    public static void clearServer() {
        SERVER_RESTRICTIONS.clear();
        SERVER_EXPLICIT_RECIPES.clear();
    }

    public static void clearClient() {
        CLIENT_RESTRICTIONS.clear();
    }

    public static void applyClientSnapshot(Collection<MachineRecipeStageRestriction> restrictions) {
        clearClient();
        for (MachineRecipeStageRestriction restriction : restrictions) {
            Map<ResourceLocation, String> byRecipe = CLIENT_RESTRICTIONS.computeIfAbsent(
                    restriction.recipeType(), ignored -> new LinkedHashMap<>());
            for (ResourceLocation recipeId : restriction.recipeIds()) {
                byRecipe.put(recipeId, restriction.stageId());
            }
        }
    }

    public static void addRecipe(String recipeTypeId, String recipeId, String stageId) {
        ResourceLocation id = parseId(recipeId, "recipe id");
        addRecipes(recipeTypeId, List.of(id), stageId, true);
    }

    public static void addRecipes(String recipeTypeId, String[] recipeIds, String stageId) {
        List<ResourceLocation> parsedIds = new ArrayList<>(recipeIds.length);
        for (String recipeId : recipeIds) {
            parsedIds.add(parseId(recipeId, "recipe id"));
        }
        addRecipes(recipeTypeId, parsedIds, stageId, true);
    }

    public static void addRecipes(String recipeTypeId, List<String> recipeIds, String stageId) {
        addRecipes(recipeTypeId, recipeIds.stream()
                .map(id -> parseId(id, "recipe id"))
                .toList(), stageId, true);
    }

    public static void addRecipeByMod(String recipeTypeId, String modId, String stageId) {
        addRecipeByMods(recipeTypeId, new String[] { modId }, stageId);
    }

    public static void addRecipeByMods(String recipeTypeId, String[] modIds, String stageId) {
        RecipeType<?> recipeType = parseSupportedRecipeType(recipeTypeId);
        Set<String> namespaces = new LinkedHashSet<>(Arrays.asList(modIds));
        List<ResourceLocation> recipeIds = getAllRecipeIds(recipeType).stream()
                .filter(id -> namespaces.contains(id.getNamespace()))
                .toList();
        addRecipes(recipeType, recipeIds, stageId, false);
    }

    public static void addRecipesByMods(String recipeTypeId, List<String> modIds, String stageId) {
        addRecipeByMods(recipeTypeId, modIds.toArray(String[]::new), stageId);
    }

    public static void addRecipeByMachine(String recipeTypeId, String stageId) {
        RecipeType<?> recipeType = parseSupportedRecipeType(recipeTypeId);
        addRecipes(recipeType, getAllRecipeIds(recipeType), stageId, false);
    }

    public static boolean canProcess(ServerPlayer player, RecipeHolder<?> holder) {
        String stage = getStage(SERVER_RESTRICTIONS, holder);
        return stage == null || AStagesHelper.hasStage(player, stage);
    }

    public static boolean canProcess(UUID ownerId, RecipeHolder<?> holder) {
        String stage = getStage(SERVER_RESTRICTIONS, holder);
        return stage == null || AStagesHelper.hasStage(ownerId, stage);
    }

    public static boolean canProcess(BlockEntity machine, RecipeHolder<?> holder) {
        String stage = getStage(SERVER_RESTRICTIONS, holder);
        return stage == null || AStagesHelper.hasStage(MachineOwnerHelper.getOwner(machine), stage);
    }

    public static boolean canProcess(BlockEntity machine, Recipe<?> recipe) {
        String stage = getStage(SERVER_RESTRICTIONS, machine, recipe);
        return stage == null || AStagesHelper.hasStage(MachineOwnerHelper.getOwner(machine), stage);
    }

    public static boolean canProcessIncludingSequenced(BlockEntity machine, RecipeHolder<?> holder) {
        return canProcess(machine, holder) && canProcessSequencedRestriction(MachineOwnerHelper.getOwner(machine), holder);
    }

    public static boolean canProcessIncludingSequenced(UUID ownerId, RecipeHolder<?> holder) {
        return canProcess(ownerId, holder) && canProcessSequencedRestriction(ownerId, holder);
    }

    public static boolean clientCanSee(RecipeType<?> recipeType, ResourceLocation recipeId) {
        String stage = getStage(CLIENT_RESTRICTIONS, recipeType, recipeId);
        return stage == null || clientHasStage(stage);
    }

    public static boolean clientShouldHide(RecipeHolder<?> holder) {
        String stage = getStage(CLIENT_RESTRICTIONS, holder);
        return stage != null && !clientHasStage(stage);
    }

    public static boolean clientShouldHide(ResourceLocation recipeId) {
        for (Map<ResourceLocation, String> byRecipe : CLIENT_RESTRICTIONS.values()) {
            String stage = byRecipe.get(recipeId);
            if (stage != null && !clientHasStage(stage)) {
                return true;
            }
        }
        return false;
    }

    public static List<MachineRecipeStageRestriction> serverSnapshot() {
        return snapshot(SERVER_RESTRICTIONS);
    }

    public static List<String> supportedRecipeTypeIds() {
        return SUPPORTED_RECIPE_TYPE_IDS.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    public static boolean isSupportedRecipeType(RecipeType<?> recipeType) {
        ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
        return id != null && SUPPORTED_RECIPE_TYPE_IDS.contains(id);
    }

    private static void addRecipes(String recipeTypeId, List<ResourceLocation> recipeIds, String stageId,
                                   boolean explicit) {
        addRecipes(parseSupportedRecipeType(recipeTypeId), recipeIds, stageId, explicit);
    }

    private static void addRecipes(RecipeType<?> recipeType, List<ResourceLocation> recipeIds, String stageId,
                                   boolean explicit) {
        if (stageId == null || stageId.isBlank()) {
            throw new IllegalArgumentException("Machine recipe stage id must not be blank");
        }

        Map<ResourceLocation, String> byRecipe = SERVER_RESTRICTIONS.computeIfAbsent(
                recipeType, ignored -> new LinkedHashMap<>());
        Set<ResourceLocation> explicitIds = SERVER_EXPLICIT_RECIPES.computeIfAbsent(
                recipeType, ignored -> new LinkedHashSet<>());

        for (ResourceLocation recipeId : recipeIds) {
            if (explicit) {
                byRecipe.put(recipeId, stageId);
                explicitIds.add(recipeId);
            } else if (!byRecipe.containsKey(recipeId) && !explicitIds.contains(recipeId)) {
                byRecipe.put(recipeId, stageId);
            }
        }
    }

    private static RecipeType<?> parseSupportedRecipeType(String recipeTypeId) {
        ResourceLocation id = parseId(recipeTypeId, "recipe type id");
        if (!SUPPORTED_RECIPE_TYPE_IDS.contains(id)) {
            throw new IllegalArgumentException("Unsupported machine recipe type: " + recipeTypeId
                    + ". Supported types: " + String.join(", ", supportedRecipeTypeIds()));
        }

        RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(id);
        if (recipeType == null) {
            throw new IllegalArgumentException("Unknown recipe type: " + recipeTypeId);
        }
        return recipeType;
    }

    private static List<ResourceLocation> getAllRecipeIds(RecipeType<?> recipeType) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Machine recipe stage registration requires a running server");
        }

        return getAllRecipes(server, recipeType).stream()
                .map(RecipeHolder::id)
                .toList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<RecipeHolder<?>> getAllRecipes(MinecraftServer server, RecipeType<?> recipeType) {
        return (List) server.getRecipeManager().getAllRecipesFor((RecipeType) recipeType);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<RecipeHolder<?>> getAllRecipes(Level level, RecipeType<?> recipeType) {
        return (List) level.getRecipeManager().getAllRecipesFor((RecipeType) recipeType);
    }

    private static String getStage(Map<RecipeType<?>, Map<ResourceLocation, String>> restrictions,
                                   RecipeHolder<?> holder) {
        if (holder == null) {
            return null;
        }
        return getStage(restrictions, holder.value().getType(), holder.id());
    }

    private static String getStage(Map<RecipeType<?>, Map<ResourceLocation, String>> restrictions,
                                   RecipeType<?> recipeType, ResourceLocation recipeId) {
        Map<ResourceLocation, String> byRecipe = restrictions.get(recipeType);
        return byRecipe == null ? null : byRecipe.get(recipeId);
    }

    private static String getStage(Map<RecipeType<?>, Map<ResourceLocation, String>> restrictions,
                                   BlockEntity machine, Recipe<?> recipe) {
        if (machine == null || recipe == null || machine.getLevel() == null) {
            return null;
        }

        RecipeType<?> recipeType = recipe.getType();
        if (!restrictions.containsKey(recipeType)) {
            return null;
        }

        for (RecipeHolder<?> holder : getAllRecipes(machine.getLevel(), recipeType)) {
            if (holder.value() == recipe || holder.value().equals(recipe)) {
                return getStage(restrictions, holder);
            }
        }
        return null;
    }

    private static boolean canProcessSequencedRestriction(UUID ownerId, RecipeHolder<?> holder) {
        String stage = getStage(SERVER_RESTRICTIONS, AllRecipeTypes.SEQUENCED_ASSEMBLY.getType(), holder.id());
        return stage == null || AStagesHelper.hasStage(ownerId, stage);
    }

    private static boolean clientHasStage(String stage) {
        return AStagesHelper.clientHasStage(stage);
    }

    private static List<MachineRecipeStageRestriction> snapshot(
            Map<RecipeType<?>, Map<ResourceLocation, String>> restrictions) {
        List<MachineRecipeStageRestriction> snapshot = new ArrayList<>();
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, String>> typeEntry : restrictions.entrySet()) {
            Map<String, List<ResourceLocation>> idsByStage = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, String> recipeEntry : typeEntry.getValue().entrySet()) {
                idsByStage.computeIfAbsent(recipeEntry.getValue(), ignored -> new ArrayList<>())
                        .add(recipeEntry.getKey());
            }

            for (Map.Entry<String, List<ResourceLocation>> stageEntry : idsByStage.entrySet()) {
                List<ResourceLocation> ids = stageEntry.getValue().stream()
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .toList();
                snapshot.add(new MachineRecipeStageRestriction(stageEntry.getKey(), typeEntry.getKey(), ids));
            }
        }
        return snapshot;
    }

    private static ResourceLocation parseId(String id, String label) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid " + label + ": " + id);
        }
        return parsed;
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
