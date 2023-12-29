package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.TextureAtlases;
import com.supermartijn642.pottery.Pottery;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBakedModel implements BakedModel, IDynamicBakedModel {

    private static final ResourceLocation DUMMY_PATTERN_SPRITE = new ResourceLocation(Pottery.MODID, "dummy_pattern");
    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormat.BLOCK);
    private static final PotData DEFAULT_POT_DATA = new PotData(PotType.DEFAULT, PotColor.BLANK, Direction.NORTH, DecoratedPotBlockEntity.Decorations.EMPTY);
    private static final ModelProperty<PotData> MODEL_PROPERTY = new ModelProperty<>();

    private final BakedModel original;
    private PotData itemModelData;

    public PotBakedModel(BakedModel original){
        this.original = original;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        BlockEntity entity;
        if(!(state.getBlock() instanceof PotBlock) || !((entity = level.getBlockEntity(pos)) instanceof PotBlockEntity))
            return modelData;

        PotType type = ((PotBlock)state.getBlock()).getType();
        PotColor color = ((PotBlock)state.getBlock()).getColor();
        DecoratedPotBlockEntity.Decorations decorations = ((PotBlockEntity)entity).getDecorations();
        Direction facing = state.getValue(PotBlock.HORIZONTAL_FACING);
        return ModelData.builder().with(MODEL_PROPERTY, new PotData(type, color, facing, decorations)).build();
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous){
        Block block = stack.getItem() instanceof BlockItem ? ((BlockItem)stack.getItem()).getBlock() : null;
        if(block == null || !(block instanceof PotBlock))
            return List.of(this);

        PotType type = ((PotBlock)block).getType();
        PotColor color = ((PotBlock)block).getColor();
        DecoratedPotBlockEntity.Decorations decorations = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(stack));
        this.itemModelData = new PotData(type, color, Direction.SOUTH, decorations);
        return List.of(this);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random, @NotNull ModelData modelData, @Nullable RenderType renderType){
        PotData data = modelData.has(MODEL_PROPERTY) ? modelData.get(MODEL_PROPERTY) : DEFAULT_POT_DATA;
        if(data == DEFAULT_POT_DATA && state == null && this.itemModelData != null)
            data = this.itemModelData;
        PotData finalData = data;
        return this.original.getQuads(state, side, random).stream()
            .map(quad -> this.adjustQuad(quad, finalData))
            .toList();
    }

    private BakedQuad adjustQuad(BakedQuad quad, PotData data){
        if(quad.getDirection().getAxis().isVertical())
            return quad;

        TextureAtlasSprite sprite = quad.getSprite();
        ResourceLocation spriteName = sprite.contents().name();
        // Swap pattern
        if(DUMMY_PATTERN_SPRITE.equals(spriteName)){
            // Find the correct decoration for the quad's side of the pot
            Item decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.getDirection());
            ResourceKey<String> decorationKey = DecoratedPotPatterns.getResourceKey(decorationItem);
            if(decorationKey == null)
                return quad;

            // Replace the quad's uv
            TextureAtlasSprite target = ClientUtils.getMinecraft().getTextureAtlas(TextureAtlases.getBlocks()).apply(data.color.getPatternLocation(decorationKey));
            return swapSprite(quad, sprite, target);
        }

        // Swap side
        if(spriteName.getNamespace().equals("pottery") && spriteName.getPath().equals(data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side")){
            // Find the correct decoration for the quad's side of the pot
            Item decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.getDirection());
            // Ignore bricks
            if(decorationItem != Items.BRICK){
                TextureAtlasSprite target = ClientUtils.getMinecraft().getTextureAtlas(TextureAtlases.getBlocks()).apply(new ResourceLocation(Pottery.MODID, data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side_decorated"));
                return swapSprite(quad, sprite, target);
            }
        }

        return quad;
    }

    private static BakedQuad swapSprite(BakedQuad quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite){
        int[] vertexData = quad.getVertices();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        int vertexSize = DefaultVertexFormat.BLOCK.getIntegerSize();
        int vertices = vertexData.length / vertexSize;
        int uvOffset = BLOCK_VERTEX_DATA_UV_OFFSET / 4;

        float oldWidth = oldSprite.getU1() - oldSprite.getU0(), oldHeight = oldSprite.getV1() - oldSprite.getV0();
        float newWidth = newSprite.getU1() - newSprite.getU0(), newHeight = newSprite.getV1() - newSprite.getV0();
        for(int i = 0; i < vertices; i++){
            int offset = i * vertexSize + uvOffset;

            float u = newSprite.getU0() + (Float.intBitsToFloat(vertexData[offset]) - oldSprite.getU0()) / oldWidth * newWidth;
            vertexData[offset] = Float.floatToRawIntBits(u);
            float v = newSprite.getV0() + (Float.intBitsToFloat(vertexData[offset + 1]) - oldSprite.getV0()) / oldHeight * newHeight;
            vertexData[offset + 1] = Float.floatToRawIntBits(v);
        }
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), newSprite, quad.isShade());
    }

    private static int findUVOffset(VertexFormat vertexFormat){
        int index;
        VertexFormatElement element = null;
        for(index = 0; index < vertexFormat.getElements().size(); index++){
            VertexFormatElement el = vertexFormat.getElements().get(index);
            if(el.getUsage() == VertexFormatElement.Usage.UV){
                element = el;
                break;
            }
        }
        if(index == vertexFormat.getElements().size() || element == null)
            throw new RuntimeException("Expected vertex format to have a UV attribute");
        if(element.getType() != VertexFormatElement.Type.FLOAT)
            throw new RuntimeException("Expected UV attribute to have data type FLOAT");
        if(element.getByteSize() < 4)
            throw new RuntimeException("Expected UV attribute to have at least 4 dimensions");
        return vertexFormat.offsets.getInt(index);
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.original.getOverrides();
    }

    private record PotData(PotType type, PotColor color, Direction facing,
                           DecoratedPotBlockEntity.Decorations decorations) {
    }
}
