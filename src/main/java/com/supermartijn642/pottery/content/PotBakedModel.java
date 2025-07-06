package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.TextureAtlases;
import com.supermartijn642.core.util.Pair;
import com.supermartijn642.pottery.Pottery;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBakedModel implements BlockStateModel {

    private static final ResourceLocation DUMMY_PATTERN_SPRITE = ResourceLocation.fromNamespaceAndPath(Pottery.MODID, "dummy_pattern");
    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormat.BLOCK);
    private static final PotData DEFAULT_POT_DATA = new PotData(PotType.DEFAULT, PotColor.BLANK, Direction.NORTH, PotDecorations.EMPTY);

    private static final ThreadLocal<PotData> MODEL_DATA = new ThreadLocal<>();

    private final BlockStateModel original;
    private final List<BlockModelPart> parts;

    public PotBakedModel(BlockStateModel original){
        this.original = original;
        this.parts = original == null ? List.of() : original.collectParts(RandomSource.create(42)).stream()
            .<BlockModelPart>map(part -> new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return PotBakedModel.getBlockQuads(part.getQuads(cullDirection));
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return part.useAmbientOcclusion();
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return part.particleIcon();
                }
            })
            .toList();
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        BlockEntity entity;
        if(!(state.getBlock() instanceof PotBlock) || !((entity = blockView.getBlockEntity(pos)) instanceof PotBlockEntity)){
            BlockStateModel.super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        PotType type = ((PotBlock)state.getBlock()).getType();
        PotColor color = ((PotBlock)state.getBlock()).getColor();
        PotDecorations decorations = ((PotBlockEntity)entity).getDecorations();
        Direction facing = state.getValue(PotBlock.HORIZONTAL_FACING);
        MODEL_DATA.set(new PotData(type, color, facing, decorations));
        try{
            BlockStateModel.super.emitQuads(emitter, blockView, pos, state, random, cullTest);
        }finally{
            MODEL_DATA.remove();
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random){
        return Pair.of(this, MODEL_DATA.get());
    }

    @Override
    public void collectParts(RandomSource randomSource, List<BlockModelPart> list){
        list.addAll(this.parts);
    }

    public static List<BakedQuad> getBlockQuads(List<BakedQuad> quads){
        PotData data = MODEL_DATA.get() == null ? DEFAULT_POT_DATA : MODEL_DATA.get();
        return quads.stream()
            .map(quad -> adjustQuad(quad, data))
            .toList();
    }

    public static List<BakedQuad> getItemQuads(ItemStack stack, List<BakedQuad> quads){
        Block block = stack.getItem() instanceof BlockItem ? ((BlockItem)stack.getItem()).getBlock() : null;
        if(!(block instanceof PotBlock))
            return List.of();

        PotType type = ((PotBlock)block).getType();
        PotColor color = ((PotBlock)block).getColor();
        PotDecorations decorations = stack.get(DataComponents.POT_DECORATIONS);
        if(decorations == null) decorations = PotDecorations.EMPTY;
        PotData data = new PotData(type, color, Direction.SOUTH, decorations);
        return quads.stream()
            .map(quad -> adjustQuad(quad, data))
            .toList();
    }

    private static BakedQuad adjustQuad(BakedQuad quad, PotData data){
        if(quad.direction().getAxis().isVertical())
            return quad;

        TextureAtlasSprite sprite = quad.sprite();
        ResourceLocation spriteName = sprite.contents().name();
        // Swap pattern
        if(DUMMY_PATTERN_SPRITE.equals(spriteName)){
            // Find the correct decoration for the quad's side of the pot
            Item decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.direction()).orElse(Items.BRICK);
            ResourceKey<DecoratedPotPattern> decorationKey = DecoratedPotPatterns.getPatternFromItem(decorationItem);
            if(decorationKey == null)
                return quad;

            // Replace the quad's uv
            TextureAtlasSprite target = ClientUtils.getMinecraft().getTextureAtlas(TextureAtlases.getBlocks()).apply(data.color.getPatternLocation(decorationKey));
            return swapSprite(quad, sprite, target);
        }

        // Swap side
        if(spriteName.getNamespace().equals("pottery") && spriteName.getPath().equals(data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side")){
            // Find the correct decoration for the quad's side of the pot
            Optional<Item> decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.direction());
            // Ignore bricks
            if(decorationItem.isPresent()){
                TextureAtlasSprite target = ClientUtils.getMinecraft().getTextureAtlas(TextureAtlases.getBlocks()).apply(ResourceLocation.fromNamespaceAndPath(Pottery.MODID, data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side_decorated"));
                return swapSprite(quad, sprite, target);
            }
        }

        return quad;
    }

    private static BakedQuad swapSprite(BakedQuad quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite){
        int[] vertexData = quad.vertices();
        // Make sure we don't change the original quad
        vertexData = Arrays.copyOf(vertexData, vertexData.length);

        int vertexSize = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
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
        return new BakedQuad(vertexData, quad.tintIndex(), quad.direction(), newSprite, quad.shade(), quad.lightEmission());
    }

    private static int findUVOffset(VertexFormat vertexFormat){
        VertexFormatElement element = null;
        for(int index = 0; index < vertexFormat.getElements().size(); index++){
            VertexFormatElement el = vertexFormat.getElements().get(index);
            if(el.usage() == VertexFormatElement.Usage.UV){
                element = el;
                break;
            }
        }
        if(element == null)
            throw new RuntimeException("Expected vertex format to have a UV attribute");
        return vertexFormat.getOffset(element);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.original.particleIcon();
    }

    private record PotData(PotType type, PotColor color, Direction facing, PotDecorations decorations) {
    }
}
