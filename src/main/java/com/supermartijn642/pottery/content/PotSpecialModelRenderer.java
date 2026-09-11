package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.supermartijn642.core.ClientUtils;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 11/09/2026 by SuperMartijn642
 */
public class PotSpecialModelRenderer implements SpecialModelRenderer<PotSpecialModelRenderer.PotData> {

    public static final PotSpecialModelRenderer INSTANCE = new PotSpecialModelRenderer();
    public static final PotSpecialModelRenderer.Unbaked UNBAKED = new PotSpecialModelRenderer.Unbaked();
    public static final MapCodec<PotSpecialModelRenderer.Unbaked> CODEC = MapCodec.unit(UNBAKED);

    private static final RandomSource RANDOM_SOURCE = RandomSource.create();
    private static final List<Vector3fc> EXTENTS = List.of(
        new Vector3f(0, 0, 0),
        new Vector3f(1, 0, 0),
        new Vector3f(1, 0, 1),
        new Vector3f(0, 0, 1),
        new Vector3f(0, 1, 0),
        new Vector3f(1, 1, 0),
        new Vector3f(1, 1, 1),
        new Vector3f(0, 1, 1)
    );

    private PotSpecialModelRenderer(){
    }

    @Override
    public void submit(@Nullable PotData potData, PoseStack poseStack, SubmitNodeCollector output, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor){
        if(potData == null)
            potData = PotData.DEFAULT;
        List<BakedQuad> regularQuads = new ArrayList<>(), patternQuads = new ArrayList<>(4);
        RANDOM_SOURCE.setSeed(0);
        PotModelHelper.collectQuads(
            potData.type,
            potData.color,
            Direction.SOUTH,
            potData.decorations,
            regularQuads::add,
            patternQuads::add,
            RANDOM_SOURCE
        );
        PotModelHelper.submitQuads(
            poseStack,
            output,
            regularQuads,
            patternQuads,
            potData.tintLayers,
            lightCoords,
            overlayCoords,
            outlineColor
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output){
        EXTENTS.forEach(output);
    }

    @Override
    public @Nullable PotData extractArgument(ItemStack stack){
        Block block = stack.getItem() instanceof BlockItem ? ((BlockItem)stack.getItem()).getBlock() : null;
        if(!(block instanceof PotBlock))
            return PotData.DEFAULT;
        PotType type = ((PotBlock)block).getType();
        PotColor color = ((PotBlock)block).getColor();
        PotDecorations decorations = stack.get(DataComponents.POT_DECORATIONS);
        if(decorations == null) decorations = PotDecorations.EMPTY;
        int[] tintLayers = PotData.DEFAULT.tintLayers;
        List<BlockTintSource> tintSources = ClientUtils.getMinecraft().getBlockColors().getTintSources(block.defaultBlockState());
        if(!tintSources.isEmpty()){
            tintLayers = new int[tintSources.size()];
            for(int i = 0; i < tintSources.size(); i++)
                tintLayers[i] = tintSources.get(i).color(block.defaultBlockState());
        }
        return new PotData(type, color, decorations, tintLayers);
    }

    public record PotData(PotType type, PotColor color, PotDecorations decorations, int[] tintLayers) {
        static final PotData DEFAULT = new PotData(PotType.DEFAULT, PotColor.BLANK, PotDecorations.EMPTY, new int[0]);
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<PotData> {
        private Unbaked(){
        }

        @Override
        public @Nullable SpecialModelRenderer<PotData> bake(BakingContext context){
            return INSTANCE;
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<PotData>> type(){
            return CODEC;
        }
    }
}
