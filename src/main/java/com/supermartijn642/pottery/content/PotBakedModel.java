package com.supermartijn642.pottery.content;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.util.Pair;
import com.supermartijn642.pottery.Pottery;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBakedModel implements DynamicBlockStateModel {

    private static final Identifier DUMMY_PATTERN_SPRITE = Identifier.fromNamespaceAndPath(Pottery.MODID, "dummy_pattern");
    private static final Map<ResourceKey<Item>,ResourceKey<DecoratedPotPattern>> PATTERNS;

    static {
        ImmutableMap.Builder<ResourceKey<Item>,ResourceKey<DecoratedPotPattern>> builder = ImmutableMap.builder();
        DecoratedPotPatterns.itemToPatternMappings(builder::put);
        PATTERNS = builder.build();
    }

    private final BlockStateModel original;

    public PotBakedModel(BlockStateModel original){
        this.original = original;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource randomSource, List<BlockStateModelPart> parts){
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
        List<BlockStateModelPart> dummyParts = new ArrayList<>();
        this.original.collectParts(level, pos, state, randomSource, dummyParts);
        for(BlockStateModelPart part : dummyParts){
            parts.add(new BlockStateModelPart() {
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
                public Material.Baked particleMaterial(){
                    return part.particleMaterial();
                }

                @Override
                public @BakedQuad.MaterialFlags int materialFlags(){
                    return part.materialFlags();
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

        TextureAtlasSprite sprite = quad.materialInfo().sprite();
        Identifier spriteName = sprite.contents().name();
        // Swap pattern
        if(DUMMY_PATTERN_SPRITE.equals(spriteName)){
            // Find the correct decoration for the quad's side of the pot
            Item decorationItem = DecorationUtils.getDecorationItem(data.decorations, data.facing, quad.direction()).orElse(Items.BRICK);
            //noinspection deprecation
            ResourceKey<DecoratedPotPattern> decorationKey = PATTERNS.get(decorationItem.builtInRegistryHolder().key());
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
                TextureAtlasSprite target = ClientUtils.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath(Pottery.MODID, data.type.getIdentifier() + "/" + data.type.getIdentifier(data.color) + "_side_decorated"));
                return swapSprite(quad, sprite, target);
            }
        }

        return quad;
    }

    private static BakedQuad swapSprite(BakedQuad quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite){
        long[] uvs = new long[4];
        for(int i = 0; i < 4; i++){
            uvs[i] = UVPair.pack(
                (UVPair.unpackU(quad.packedUV(i)) - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0()) * (newSprite.getU1() - newSprite.getU0()) + newSprite.getU0(),
                (UVPair.unpackV(quad.packedUV(i)) - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0()) * (newSprite.getV1() - newSprite.getV0()) + newSprite.getV0()
            );
        }
        return new BakedQuad(
            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            uvs[0], uvs[1], uvs[2], uvs[3],
            quad.direction(),
            new BakedQuad.MaterialInfo(
                newSprite,
                ChunkSectionLayer.byTransparency(newSprite.transparency()),
                newSprite.transparency().hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet(),
                quad.materialInfo().tintIndex(),
                quad.materialInfo().shade(),
                quad.materialInfo().lightEmission()
            )
        );
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.original.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.original.materialFlags();
    }

    @Override
    public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag){
        return this.original.hasMaterialFlag(flag);
    }

    private record PotData(PotType type, PotColor color, Direction facing, PotDecorations decorations) {
    }
}
