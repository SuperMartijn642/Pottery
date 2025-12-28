package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.util.Pair;
import com.supermartijn642.pottery.Pottery;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
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
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBakedModel implements DynamicBlockStateModel {

    private static final ResourceLocation DUMMY_PATTERN_SPRITE = ResourceLocation.fromNamespaceAndPath(Pottery.MODID, "dummy_pattern");
    private static final int BLOCK_VERTEX_DATA_UV_OFFSET = findUVOffset(DefaultVertexFormat.BLOCK);

    private final BlockStateModel original;

    public PotBakedModel(BlockStateModel original){
        this.original = original;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource randomSource, List<BlockModelPart> parts){
        BlockEntity entity;
        if(!(state.getBlock() instanceof PotBlock) || !((entity = level.getBlockEntity(pos)) instanceof PotBlockEntity)){
            this.original.collectParts(level, pos, state, randomSource, parts);
            return;
        }

        PotType type = ((PotBlock)state.getBlock()).getType();
        PotColor color = ((PotBlock)state.getBlock()).getColor();
        PotDecorations decorations = ((PotBlockEntity)entity).getDecorations();
        Direction facing = state.getValue(PotBlock.HORIZONTAL_FACING);
        PotData data = new PotData(type, color, facing, decorations);
        for(BlockModelPart part : this.original.collectParts(level, pos, state, randomSource)){
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return part.getQuads(cullDirection).stream()
                        .map(quad -> adjustQuad(quad, data))
                        .toList();
                }

                @Override
                public boolean useAmbientOcclusion(){
                    //noinspection deprecation
                    return part.useAmbientOcclusion();
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return part.particleIcon();
                }
            });
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        BlockEntity entity;
        if(!(state.getBlock() instanceof PotBlock) || !((entity = level.getBlockEntity(pos)) instanceof PotBlockEntity))
            return this;
        PotType type = ((PotBlock)state.getBlock()).getType();
        PotColor color = ((PotBlock)state.getBlock()).getColor();
        PotDecorations decorations = ((PotBlockEntity)entity).getDecorations();
        Direction facing = state.getValue(PotBlock.HORIZONTAL_FACING);
        PotData data = new PotData(type, color, facing, decorations);
        return Pair.of(this, data);
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
            TextureAtlasSprite target = ClientUtils.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(data.color.getPatternLocation(decorationKey));
            return swapSprite(quad, sprite, target);
        }

        // Swap side
        if(spriteName.getNamespace().equals("pottery") && spriteName.getPath().equals(data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side")){
            // Find the correct decoration for the quad's side of the pot
            Optional<Item> decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.direction());
            // Ignore bricks
            if(decorationItem.isPresent()){
                TextureAtlasSprite target = ClientUtils.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ResourceLocation.fromNamespaceAndPath(Pottery.MODID, data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side_decorated"));
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
        //noinspection deprecation
        return this.original.particleIcon();
    }

    private record PotData(PotType type, PotColor color, Direction facing, PotDecorations decorations) {
    }
}
