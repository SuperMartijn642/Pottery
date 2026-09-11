package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.pottery.Pottery;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 11/09/2026 by SuperMartijn642
 */
public class PotModelHelper {

    private static final Identifier DUMMY_PATTERN_SPRITE = Identifier.fromNamespaceAndPath(Pottery.MODID, "dummy_pattern");
    private static final Direction[] CULL_DIRECTIONS = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static void collectQuads(PotType type, PotColor color, Direction facing, PotDecorations decorations, Consumer<BakedQuad> regularQuads, Consumer<BakedQuad> patternQuads, RandomSource random){
        BlockState blockState = type.getBlock(color).defaultBlockState().setValue(PotBlock.HORIZONTAL_FACING, facing);
        BlockStateModel model = ClientUtils.getMinecraft().getModelManager().getBlockStateModelSet().get(blockState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        for(BlockStateModelPart part : parts){
            for(Direction cullDirection : CULL_DIRECTIONS){
                for(BakedQuad quad : part.getQuads(cullDirection)){
                    collectQuad(quad, type, color, facing, decorations, regularQuads, patternQuads);
                }
            }
        }
    }

    private static void collectQuad(BakedQuad quad, PotType type, PotColor color, Direction facing, PotDecorations decorations, Consumer<BakedQuad> regularQuads, Consumer<BakedQuad> patternQuads){
        if(quad.direction().getAxis().isVertical()){
            regularQuads.accept(quad);
            return;
        }

        // Check the quad is a dummy pattern or if it is the side of a pot
        TextureAtlasSprite sprite = quad.materialInfo().sprite();
        Identifier spriteName = sprite.contents().name();
        boolean isDummySprite = DUMMY_PATTERN_SPRITE.equals(spriteName);
        if(!isDummySprite && !(spriteName.getNamespace().equals("pottery") && spriteName.getPath().equals(type.getIdentifier() + "/" + type.getIdentifier(color) + "_side"))){
            regularQuads.accept(quad);
            return;
        }

        // Get the pattern to use
        DecoratedPotPattern pattern = DecorationUtils.getDecorationItem(decorations, facing, quad.direction())
            .map(t -> t.get(DataComponents.PROVIDES_POTTERY_PATTERN))
            .filter(Holder::isBound)
            .map(Holder::value)
            .orElse(null);
        Identifier patternIdentifier;
        if(pattern == null){
            if(isDummySprite)
                patternIdentifier = Identifier.withDefaultNamespace("decorated_pot_side");
            else{
                regularQuads.accept(quad);
                return;
            }
        }else
            patternIdentifier = pattern.assetId();

        // Replace the quad's uv
        TextureAtlasSprite target = ClientUtils.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.DECORATED_POT).getSprite(color.getPatternLocation(patternIdentifier));
        patternQuads.accept(swapSprite(quad, sprite, target));
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
                newSprite.transparency().hasTranslucent() ? Sheets.translucentBlockItemGlintSheet() : Sheets.cutoutBlockItemGlintSheet(),
                newSprite.transparency().hasTranslucent() ? Sheets.translucentBlockItemGlintSpecialSheet() : Sheets.cutoutBlockItemGlintSpecialSheet(),
                quad.materialInfo().tintIndex(),
                quad.materialInfo().shadeDirectionOverride(),
                quad.materialInfo().lightEmission()
            )
        );
    }

    public static void submitQuads(PoseStack poseStack, SubmitNodeCollector output, List<BakedQuad> regularQuads, List<BakedQuad> patternQuads, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor){
        if(!regularQuads.isEmpty()){
            DummyBlockStateModelPart regularQuadsPart = new DummyBlockStateModelPart(regularQuads);
            output.submitBlockModel(
                poseStack,
                (regularQuadsPart.flags & BakedQuad.FLAG_TRANSLUCENT) != 0 ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet(),
                List.of(regularQuadsPart),
                tintLayers,
                lightCoords,
                overlayCoords,
                outlineColor
            );
        }
        if(!patternQuads.isEmpty()){
            DummyBlockStateModelPart patternQuadsPart = new DummyBlockStateModelPart(patternQuads);
            output.submitBlockModel(
                poseStack,
                RenderTypes.entitySolid(Sheets.DECORATED_POT_SHEET),
                List.of(patternQuadsPart),
                tintLayers,
                lightCoords,
                overlayCoords,
                outlineColor
            );
        }
    }

    private record DummyBlockStateModelPart(List<BakedQuad> quads, Material.Baked particleSprite, int flags) implements BlockStateModelPart {
        DummyBlockStateModelPart(List<BakedQuad> quads){
            int flags = 0;
            for(BakedQuad quad : quads)
                flags |= quad.materialInfo().flags();
            this(
                quads,
                new Material.Baked(quads.getFirst().materialInfo().sprite(), false),
                flags
            );
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
            return cullDirection == null ? this.quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion(){
            return true;
        }

        @Override
        public Material.Baked particleMaterial(){
            return this.particleSprite;
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags(){
            return this.flags;
        }
    }
}
