package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.Config;
import com.gugucraft.guguaddons.registry.ModItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@EmiEntrypoint
public class ModEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registerSlashBackSmithing(registry);
    }

    private void registerSlashBackSmithing(EmiRegistry registry) {
        // 1. Get Configured Template
        String templateId = Config.SLASH_BACK_REFILL_TEMPLATE.get();
        Item templateItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(templateId));
        EmiIngredient template = EmiStack.of(templateItem);

        // 2. Get Configured Material
        String materialId = Config.SLASH_BACK_REFILL_MATERIAL.get();
        Item materialItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(materialId));
        EmiIngredient material = EmiStack.of(materialItem);

        // 3. Create Base Item (Broken Terminal)
        ItemStack damagedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        damagedStack.setDamageValue(damagedStack.getMaxDamage() - 1);
        EmiIngredient base = EmiStack.of(damagedStack);

        // 4. Create Result Item (Repaired Terminal)
        ItemStack repairedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        repairedStack.setDamageValue(0);
        EmiStack result = EmiStack.of(repairedStack);

        // 5. Create EmiSmithingRecipe
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("guguaddons", "/slash_back_smithing_emi");

        registry.addRecipe(new EmiRecipe() {
            @Override
            public EmiRecipeCategory getCategory() {
                return VanillaEmiRecipeCategories.SMITHING;
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public List<EmiIngredient> getInputs() {
                return List.of(template, base, material);
            }

            @Override
            public List<EmiStack> getOutputs() {
                return List.of(result);
            }

            @Override
            public int getDisplayWidth() {
                return 112;
            }

            @Override
            public int getDisplayHeight() {
                return 18;
            }

            @Override
            public void addWidgets(WidgetHolder widgets) {
                // Mimic standard smithing layout
                // Template (0,0), Base (18,0), Material (36,0) -> Arrow -> Output (94,0)
                widgets.addSlot(template, 0, 0);
                widgets.addSlot(base, 18, 0);
                widgets.addSlot(material, 36, 0);

                // EMI API usually has standard texture constants.
                // EmiTexture.EMPTY_ARROW is at 62, 1 in standard smithing.
                widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);

                widgets.addSlot(result, 94, 0).recipeContext(this);
            }
        });
    }
}
