package com.gugucraft.guguaddons.stage;

import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.wrapper.RecipeWrapper;
import com.alessandro.astages.engine.ARestrictionManager;
import com.alessandro.astages.engine.server.restriction.recipe.ARecipeRestriction;
import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.compat.kubejs.MachineRecipeStageKubeEvent;
import com.gugucraft.guguaddons.compat.kubejs.MachineRecipeStageKubeEvents;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class MachineRecipeStageManager {
    private static final ResourceLocation LYCHEE_CRAFTING_ID = id("lychee", "crafting");
    private static final UUID UNOWNED_MACHINE_HOLDER = new UUID(0L, 0L);
    private static final Map<RecipeType<?>, Map<ResourceLocation, String>> SERVER_RESTRICTIONS = new LinkedHashMap<>();
    private static final Map<RecipeType<?>, Set<ResourceLocation>> SERVER_EXPLICIT_RECIPES = new LinkedHashMap<>();
    private static final Set<String> REGISTERED_RESTRICTION_IDS = new LinkedHashSet<>();
    private static final Map<RecipeManager, Map<RecipeType<?>, RecipeIdLookup>> SERVER_RECIPE_IDS =
            new WeakHashMap<>();

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
            id("lychee", "block_interacting"),
            id("lychee", "block_clicking"),
            id("lychee", "item_burning"),
            id("lychee", "item_inside"),
            id("lychee", "anvil_crafting"),
            id("lychee", "block_crushing"),
            id("lychee", "lightning_channeling"),
            id("lychee", "item_exploding"),
            id("lychee", "entity_ticking"),
            id("lychee", "block_exploding"),
            id("lychee", "random_block_ticking"),
            id("lychee", "dripstone_dripping"),
            LYCHEE_CRAFTING_ID,
            id("minecraft", "smelting"),
            id("minecraft", "smoking"),
            id("minecraft", "blasting"),
            id(GuGuAddons.MODID, "vacuumizing"),
            id(GuGuAddons.MODID, "pressurizing"),
            id(GuGuAddons.MODID, "centrifugation"),
            id(GuGuAddons.MODID, "abyss_catalysis"));

    private MachineRecipeStageManager() {
    }

    public static void reloadFromKubeJS() {
        clearServer();
        MachineRecipeStageKubeEvents.REGISTER.post(new MachineRecipeStageKubeEvent());
        publishRestrictions();
    }

    private static void clearServer() {
        for (String restrictionId : REGISTERED_RESTRICTION_IDS) {
            ARestrictionManager.RECIPE_INSTANCE.removeRestriction(restrictionId);
        }
        REGISTERED_RESTRICTION_IDS.clear();
        SERVER_RESTRICTIONS.clear();
        SERVER_EXPLICIT_RECIPES.clear();
        SERVER_RECIPE_IDS.clear();
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
        ResourceLocation requestedTypeId = parseSupportedRecipeTypeId(recipeTypeId);
        RecipeType<?> recipeType = resolveSupportedRecipeType(requestedTypeId, recipeTypeId);
        Set<String> namespaces = new LinkedHashSet<>(Arrays.asList(modIds));
        List<ResourceLocation> recipeIds = getAllRecipeIds(recipeType, requestedTypeId).stream()
                .filter(id -> namespaces.contains(id.getNamespace()))
                .toList();
        addRecipes(recipeType, recipeIds, stageId, false);
    }

    public static void addRecipesByMods(String recipeTypeId, List<String> modIds, String stageId) {
        addRecipeByMods(recipeTypeId, modIds.toArray(String[]::new), stageId);
    }

    public static void addRecipeByMachine(String recipeTypeId, String stageId) {
        ResourceLocation requestedTypeId = parseSupportedRecipeTypeId(recipeTypeId);
        RecipeType<?> recipeType = resolveSupportedRecipeType(requestedTypeId, recipeTypeId);
        addRecipes(recipeType, getAllRecipeIds(recipeType, requestedTypeId), stageId, false);
    }

    public static boolean canProcess(ServerPlayer player, RecipeHolder<?> holder) {
        return holder == null || isAllowed(player, wrapper(holder));
    }

    public static boolean canProcess(ServerPlayer player, Level level, Recipe<?> recipe) {
        RecipeWrapper wrapper = wrapper(level, recipe);
        return wrapper == null || isAllowed(player, wrapper);
    }

    public static boolean canProcess(UUID ownerId, RecipeHolder<?> holder) {
        return holder == null || isAllowed(ownerId, wrapper(holder));
    }

    public static boolean canProcess(BlockEntity machine, RecipeHolder<?> holder) {
        return holder == null || isAllowed(MachineOwnerHelper.getOwner(machine), wrapper(holder));
    }

    public static boolean canProcess(BlockEntity machine, Recipe<?> recipe) {
        RecipeWrapper wrapper = wrapper(machine, recipe);
        return wrapper == null || isAllowed(MachineOwnerHelper.getOwner(machine), wrapper);
    }

    public static boolean canProcessGlobally(RecipeHolder<?> holder) {
        return holder == null || isAllowed(AHolder.server(), wrapper(holder));
    }

    public static boolean canProcessGlobally(Level level, Recipe<?> recipe) {
        RecipeWrapper wrapper = wrapper(level, recipe);
        return wrapper == null || isAllowed(AHolder.server(), wrapper);
    }

    public static boolean canProcessIncludingSequenced(BlockEntity machine, RecipeHolder<?> holder) {
        return canProcess(machine, holder) && canProcessSequencedRestriction(MachineOwnerHelper.getOwner(machine), holder);
    }

    public static boolean canProcessIncludingSequenced(UUID ownerId, RecipeHolder<?> holder) {
        return canProcess(ownerId, holder) && canProcessSequencedRestriction(ownerId, holder);
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

    private static ResourceLocation parseSupportedRecipeTypeId(String recipeTypeId) {
        ResourceLocation id = parseId(recipeTypeId, "recipe type id");
        if (!SUPPORTED_RECIPE_TYPE_IDS.contains(id)) {
            throw new IllegalArgumentException("Unsupported machine recipe type: " + recipeTypeId
                    + ". Supported types: " + String.join(", ", supportedRecipeTypeIds()));
        }
        return id;
    }

    private static RecipeType<?> parseSupportedRecipeType(String recipeTypeId) {
        ResourceLocation id = parseSupportedRecipeTypeId(recipeTypeId);
        return resolveSupportedRecipeType(id, recipeTypeId);
    }

    private static RecipeType<?> resolveSupportedRecipeType(ResourceLocation id, String recipeTypeId) {
        RecipeType<?> recipeType = LYCHEE_CRAFTING_ID.equals(id)
                ? RecipeType.CRAFTING
                : BuiltInRegistries.RECIPE_TYPE.get(id);
        if (recipeType == null) {
            throw new IllegalArgumentException("Unknown recipe type: " + recipeTypeId);
        }
        return recipeType;
    }

    private static List<ResourceLocation> getAllRecipeIds(RecipeType<?> recipeType) {
        return getAllRecipeIds(recipeType, BuiltInRegistries.RECIPE_TYPE.getKey(recipeType));
    }

    private static List<ResourceLocation> getAllRecipeIds(RecipeType<?> recipeType, ResourceLocation requestedTypeId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Machine recipe stage registration requires a running server");
        }

        return getAllRecipes(server, recipeType).stream()
                .filter(holder -> matchesRequestedRecipeType(requestedTypeId, holder))
                .map(RecipeHolder::id)
                .toList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<RecipeHolder<?>> getAllRecipes(MinecraftServer server, RecipeType<?> recipeType) {
        return getAllRecipes(server.getRecipeManager(), recipeType);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<RecipeHolder<?>> getAllRecipes(RecipeManager recipeManager, RecipeType<?> recipeType) {
        return (List) recipeManager.getAllRecipesFor((RecipeType) recipeType);
    }

    private static RecipeWrapper wrapper(BlockEntity machine, Recipe<?> recipe) {
        if (machine == null || recipe == null || machine.getLevel() == null) {
            return null;
        }
        return wrapper(machine.getLevel(), recipe);
    }

    private static RecipeWrapper wrapper(Level level, Recipe<?> recipe) {
        if (level == null || recipe == null) {
            return null;
        }
        RecipeType<?> recipeType = recipe.getType();
        ResourceLocation recipeId = getRecipeId(level.getRecipeManager(), recipeType, recipe);
        return recipeId == null ? null : new RecipeWrapper(recipeType, recipeId);
    }

    private static RecipeWrapper wrapper(RecipeHolder<?> holder) {
        return new RecipeWrapper(holder.value().getType(), holder.id());
    }

    private static ResourceLocation getRecipeId(RecipeManager recipeManager, RecipeType<?> recipeType,
                                                Recipe<?> recipe) {
        RecipeIdLookup lookup = SERVER_RECIPE_IDS
                .computeIfAbsent(recipeManager, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(recipeType, type -> createRecipeIdLookup(recipeManager, type));
        return lookup.get(recipe);
    }

    private static RecipeIdLookup createRecipeIdLookup(RecipeManager recipeManager, RecipeType<?> recipeType) {
        IdentityHashMap<Recipe<?>, ResourceLocation> byIdentity = new IdentityHashMap<>();
        Map<Recipe<?>, ResourceLocation> byEquality = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : getAllRecipes(recipeManager, recipeType)) {
            Recipe<?> value = holder.value();
            byIdentity.put(value, holder.id());
            byEquality.putIfAbsent(value, holder.id());
        }
        return new RecipeIdLookup(byIdentity, byEquality);
    }

    private static boolean canProcessSequencedRestriction(UUID ownerId, RecipeHolder<?> holder) {
        return holder == null
                || isAllowed(ownerId, new RecipeWrapper(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType(), holder.id()));
    }

    private static boolean matchesRequestedRecipeType(ResourceLocation requestedTypeId, RecipeHolder<?> holder) {
        if (!LYCHEE_CRAFTING_ID.equals(requestedTypeId)) {
            return true;
        }

        ResourceLocation serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer());
        return LYCHEE_CRAFTING_ID.equals(serializerId);
    }

    private static boolean isAllowed(AHolder holder, RecipeWrapper wrapper) {
        return ARestrictionManager.RECIPE_INSTANCE.getRestriction(holder, wrapper) == null;
    }

    private static boolean isAllowed(ServerPlayer player, RecipeWrapper wrapper) {
        AHolder holder = player == null
                ? AHolder.player(UNOWNED_MACHINE_HOLDER)
                : AHolder.serverAndPlayer(player);
        return isAllowed(holder, wrapper);
    }

    private static boolean isAllowed(UUID ownerId, RecipeWrapper wrapper) {
        if (ownerId == null) {
            return isAllowed(AHolder.player(UNOWNED_MACHINE_HOLDER), wrapper);
        }
        if (isAllowed(AHolder.server(), wrapper)) {
            return true;
        }
        return isAllowed(AHolder.player(ownerId), wrapper);
    }

    private static void publishRestrictions() {
        int restrictionIndex = 0;
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, String>> typeEntry : SERVER_RESTRICTIONS.entrySet()) {
            Map<String, List<ResourceLocation>> recipesByStage = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, String> recipeEntry : typeEntry.getValue().entrySet()) {
                recipesByStage.computeIfAbsent(recipeEntry.getValue(), ignored -> new ArrayList<>())
                        .add(recipeEntry.getKey());
            }

            for (Map.Entry<String, List<ResourceLocation>> stageEntry : recipesByStage.entrySet()) {
                String restrictionId = GuGuAddons.MODID + ":machine_recipe_stage/" + restrictionIndex++;
                ARecipeRestriction restriction = new ARecipeRestriction(restrictionId, stageEntry.getKey());
                for (ResourceLocation recipeId : stageEntry.getValue()) {
                    restriction.restrict(new RecipeWrapper(typeEntry.getKey(), recipeId));
                }
                ARestrictionManager.RECIPE_INSTANCE.addRestriction(restriction);
                REGISTERED_RESTRICTION_IDS.add(restrictionId);
                restriction.markAsDirty();
            }
        }

        if (restrictionIndex == 0) {
            new ARecipeRestriction(GuGuAddons.MODID + ":machine_recipe_stage/refresh", "internal")
                    .markAsDirty();
        }
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

    private record RecipeIdLookup(IdentityHashMap<Recipe<?>, ResourceLocation> byIdentity,
                                  Map<Recipe<?>, ResourceLocation> byEquality) {
        ResourceLocation get(Recipe<?> recipe) {
            ResourceLocation recipeId = byIdentity.get(recipe);
            return recipeId == null ? byEquality.get(recipe) : recipeId;
        }
    }
}
