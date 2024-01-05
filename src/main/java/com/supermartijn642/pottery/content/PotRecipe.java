package com.supermartijn642.pottery.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotRecipe extends ShapedRecipe {

    public static final Serializer SERIALIZER = new Serializer();

    private final Ingredient dyeIngredient;
    private final int[] sherdIndices;

    public PotRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack output, boolean showNotification, Ingredient dyeIngredient, int[] sherdIndices){
        super(group, category, pattern, output, showNotification);
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

        private static final Function<Integer,DataResult<Integer>> GEQUAL_TO_ZERO = integer -> integer < 0 ? DataResult.error(() -> "Value '" + integer + "' is less than 0!") : DataResult.success(integer);
        private static final Codec<PotRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShapedRecipe.Serializer.CODEC.fieldOf("recipe").forGetter(recipe -> null),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("dye_ingredient").forGetter(recipe -> Optional.of(recipe.dyeIngredient)),
            Codec.INT.flatXmap(GEQUAL_TO_ZERO, GEQUAL_TO_ZERO).listOf().fieldOf("sherds").forGetter(recipe -> IntStream.of(recipe.sherdIndices).boxed().toList())
        ).apply(instance, (shapedRecipe, dyeIngredient, sherdIndices) -> new PotRecipe(
            shapedRecipe.getGroup(),
            shapedRecipe.category(),
            shapedRecipe.pattern,
            shapedRecipe.getResultItem(null),
            shapedRecipe.showNotification(),
            dyeIngredient.orElse(null),
            sherdIndices.stream().mapToInt(i -> i).toArray()
        )));

        @Override
        public Codec<PotRecipe> codec(){
            return CODEC;
        }

        @Override
        public PotRecipe fromNetwork(FriendlyByteBuf buffer){
            ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.fromNetwork(buffer);
            Ingredient dyeIngredient = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : null;
            int[] sherdIndices = buffer.readVarIntArray(4);
            if(sherdIndices.length != 4)
                throw new IllegalArgumentException();
            return new PotRecipe(
                shapedRecipe.getGroup(),
                shapedRecipe.category(),
                shapedRecipe.pattern,
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
