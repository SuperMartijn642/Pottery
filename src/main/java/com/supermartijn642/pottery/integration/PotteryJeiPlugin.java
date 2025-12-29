package com.supermartijn642.pottery.integration;

import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

/**
 * Created 04/12/2023 by SuperMartijn642
 */
@JeiPlugin
public class PotteryJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid(){
        return Identifier.fromNamespaceAndPath(Pottery.MODID, "pot_recipes");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration){
        registration.getCraftingCategory().addExtension(PotRecipe.class, new ICraftingCategoryExtension<>() {
            @Override
            public void onDisplayedIngredientsUpdate(RecipeHolder<PotRecipe> recipeHolder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses){
                // Update the output stack for the currently shown sherds
                ItemStack decoratedPot = recipeHolder.value().assemble(
                    CraftingInput.of(3, 3, recipeSlots.stream().skip(1).map(display -> display.getDisplayedItemStack().orElse(ItemStack.EMPTY)).toList()),
                    null
                );
                recipeSlots.getFirst().createDisplayOverrides().add(decoratedPot);
            }

            @Override
            public List<SlotDisplay> getIngredients(RecipeHolder<PotRecipe> recipeHolder){
                return ((ShapedCraftingRecipeDisplay)recipeHolder.value().display().getFirst()).ingredients();
            }

            @Override
            public int getWidth(RecipeHolder<PotRecipe> recipeHolder){
                return recipeHolder.value().getWidth();
            }

            @Override
            public int getHeight(RecipeHolder<PotRecipe> recipeHolder){
                return recipeHolder.value().getHeight();
            }
        });
    }
}
