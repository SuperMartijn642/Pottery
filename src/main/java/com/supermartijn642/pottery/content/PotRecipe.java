package com.supermartijn642.pottery.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

import java.util.Objects;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotRecipe extends ShapedRecipe {

    public static final Serializer SERIALIZER = new Serializer();

    private final Ingredient dyeIngredient;
    private final int[] sherdIndices;

    public PotRecipe(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack output, boolean showNotification, Ingredient dyeIngredient, int[] sherdIndices){
        super(identifier, group, category, width, height, ingredients, output, showNotification);
        this.dyeIngredient = dyeIngredient;
        this.sherdIndices = sherdIndices;
    }

    public Ingredient getDyeIngredient(){
        return this.dyeIngredient;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level){
        return this.findRecipeDecorations(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess){
        ItemStack stack = super.assemble(container, registryAccess);

        // Add the decorations
        DecoratedPotBlockEntity.Decorations decorations = this.findRecipeDecorations(container);
        Objects.requireNonNull(decorations);
        if(!decorations.equals(DecoratedPotBlockEntity.Decorations.EMPTY))
            stack.addTagElement(BlockItem.BLOCK_ENTITY_TAG, decorations.save(new CompoundTag()));

        return stack;
    }

    @Override
    public RecipeSerializer<?> getSerializer(){
        return SERIALIZER;
    }

    private DecoratedPotBlockEntity.Decorations findRecipeDecorations(CraftingContainer container){
        if(!this.canCraftInDimensions(container.getWidth(), container.getHeight()))
            return null;

        for(int x = 0; x <= container.getWidth() - this.getWidth(); ++x){
            for(int y = 0; y <= container.getHeight() - this.getHeight(); ++y){
                DecoratedPotBlockEntity.Decorations decorations = this.matchesSubGrid(container, x, y, true);
                if(decorations == null)
                    decorations = this.matchesSubGrid(container, x, y, false);
                if(decorations != null)
                    return decorations;
            }
        }
        return null;
    }

    private DecoratedPotBlockEntity.Decorations matchesSubGrid(CraftingContainer container, int startX, int startY, boolean mirrored){
        boolean foundDye = false;
        for(int x = 0; x < container.getWidth(); ++x){
            for(int y = 0; y < container.getHeight(); ++y){
                ItemStack stack = container.getItem(x + y * container.getWidth());
                if(this.dyeIngredient != null && this.dyeIngredient.test(stack)){
                    if(foundDye)
                        return null;
                    foundDye = true;
                    continue;
                }

                int relativeX = x - startX;
                int relativeY = y - startY;
                if(relativeX >= 0 && relativeY >= 0 && relativeX < this.getWidth() && relativeY < this.getHeight()){
                    Ingredient ingredient = this.getIngredients().get(mirrored ?
                        this.getWidth() - relativeX - 1 + relativeY * this.getWidth() :
                        relativeX + relativeY * this.getWidth()
                    );
                    if(!ingredient.test(stack))
                        return null;
                }else if(!Ingredient.EMPTY.test(stack))
                    return null;
            }
        }

        if(this.dyeIngredient != null && !foundDye)
            return null;

        Item front = container.getItem(startX + this.sherdIndices[0] % this.getWidth() + (startY + this.sherdIndices[0] / this.getWidth()) * container.getWidth()).getItem();
        Item left = container.getItem(startX + this.sherdIndices[1] % this.getWidth() + (startY + this.sherdIndices[1] / this.getWidth()) * container.getWidth()).getItem();
        Item right = container.getItem(startX + this.sherdIndices[2] % this.getWidth() + (startY + this.sherdIndices[2] / this.getWidth()) * container.getWidth()).getItem();
        Item back = container.getItem(startX + this.sherdIndices[3] % this.getWidth() + (startY + this.sherdIndices[3] / this.getWidth()) * container.getWidth()).getItem();
        return new DecoratedPotBlockEntity.Decorations(back, left, right, front);
    }

    public static class Serializer implements RecipeSerializer<PotRecipe> {

        @Override
        public PotRecipe fromJson(ResourceLocation identifier, JsonObject json){
            if(!json.has("recipe") || !json.get("recipe").isJsonObject())
                throw new JsonParseException("Missing object property 'recipe'!");
            ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.fromJson(identifier, json.getAsJsonObject("recipe"));
            Ingredient dyeIngredient = null;
            if(json.has("dye_ingredient"))
                dyeIngredient = Ingredient.fromJson(json.get("dye_ingredient"), false);
            if(!json.has("sherds") || !json.get("sherds").isJsonArray())
                throw new JsonParseException("Missing array property 'sherds'!");
            JsonArray sherdsJson = json.getAsJsonArray("sherds");
            if(sherdsJson.size() != 4)
                throw new JsonParseException("Array 'sherds' must have 4 elements!");
            int[] sherdIndices = sherdsJson.asList().stream().mapToInt(JsonElement::getAsInt).toArray();
            return new PotRecipe(
                identifier,
                shapedRecipe.getGroup(),
                shapedRecipe.category(),
                shapedRecipe.getWidth(),
                shapedRecipe.getHeight(),
                shapedRecipe.getIngredients(),
                shapedRecipe.getResultItem(null),
                shapedRecipe.showNotification(),
                dyeIngredient,
                sherdIndices
            );
        }

        @Override
        public PotRecipe fromNetwork(ResourceLocation identifier, FriendlyByteBuf buffer){
            ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.fromNetwork(identifier, buffer);
            Ingredient dyeIngredient = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : null;
            int[] sherdIndices = buffer.readVarIntArray(4);
            if(sherdIndices.length != 4)
                throw new IllegalArgumentException();
            return new PotRecipe(
                identifier,
                shapedRecipe.getGroup(),
                shapedRecipe.category(),
                shapedRecipe.getWidth(),
                shapedRecipe.getHeight(),
                shapedRecipe.getIngredients(),
                shapedRecipe.getResultItem(null),
                shapedRecipe.showNotification(),
                dyeIngredient,
                sherdIndices
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PotRecipe recipe){
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
            buffer.writeBoolean(recipe.dyeIngredient != null);
            if(recipe.dyeIngredient != null)
                recipe.dyeIngredient.toNetwork(buffer);
            buffer.writeVarIntArray(recipe.sherdIndices);
        }
    }
}
