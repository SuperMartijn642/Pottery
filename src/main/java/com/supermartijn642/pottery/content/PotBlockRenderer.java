package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.CustomBlockEntityRenderer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 27/12/2023 by SuperMartijn642
 */
public class PotBlockRenderer implements CustomBlockEntityRenderer<PotBlockEntity,PotBlockRenderer.State> {

    @Override
    public State createStateHolder(){
        return new State();
    }

    @Override
    public void updateState(State state, PotBlockEntity entity, UpdateContext context){
        DecoratedPotBlockEntity.WobbleStyle wobbleStyle = entity.lastWobbleStyle;
        state.wobbleStyle = wobbleStyle;
        if(wobbleStyle != null && entity.hasLevel())
            //noinspection DataFlowIssue
            state.wobble = (entity.getLevel().getGameTime() - entity.wobbleStartedAtTick + context.partialTicks()) / wobbleStyle.duration;
        state.facing = entity.getFacing();
        state.regularQuads.clear();
        state.patternQuads.clear();
        BlockState blockState = entity.getBlockState();
        PotBlock block = (PotBlock)blockState.getBlock();
        BlockPos pos = entity.getBlockPos();
        PotModelHelper.collectQuads(
            block.getType(),
            block.getColor(),
            blockState.getValue(PotBlock.HORIZONTAL_FACING),
            entity.getDecorations(),
            state.regularQuads::add,
            state.patternQuads::add,
            context.randomSource(blockState.getSeed(pos))
        );
        if(state.tintLayers != null)
            state.tintLayers.clear();
        List<BlockTintSource> tintSources = ClientUtils.getMinecraft().getBlockColors().getTintSources(blockState);
        if(!tintSources.isEmpty()){
            if(state.tintLayers == null)
                state.tintLayers = new IntArrayList(tintSources.size());
            BlockAndTintGetter level = entity.getLevel() instanceof BlockAndTintGetter l ? l : BlockAndTintGetter.EMPTY;
            for(BlockTintSource tintSource : tintSources)
                state.tintLayers.add(tintSource.colorInWorld(blockState, level, pos));
        }
    }

    @Override
    public void submit(SubmitNodeCollector output, State state, RenderContext context){
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();

        if(state.wobbleStyle != null && state.wobble >= 0 && state.wobble <= 1){
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
            if(state.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE){
                float rotation = state.wobble * ((float)Math.PI * 2);
                float xAxis = -1.5f * (Mth.cos(rotation) + 0.5f) * Mth.sin(rotation / 2);
                poseStack.rotate(Axis.XP, xAxis * 0.015625f);
                float zAxis = Mth.sin(rotation);
                poseStack.rotate(Axis.ZP, zAxis * 0.015625f);
            }else{
                float h = Mth.sin(-state.wobble * 3 * (float)Math.PI) * 0.125f;
                float k = 1 - state.wobble;
                poseStack.rotate(Axis.YP, h * k);
            }
            poseStack.rotateDegrees(Axis.YP, state.facing.toYRot() - 180);
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        // Render the block
        ModelFeatureRenderer.CrumblingOverlay breakingOverlay = context.breakingOverlay();
        int[] tintLayers = state.tintLayers == null ? BlockModelRenderState.EMPTY_TINTS : state.tintLayers.toIntArray();
        PotModelHelper.submitQuads(
            poseStack,
            output,
            state.regularQuads, state.patternQuads,
            tintLayers,
            context.packedLight(),
            breakingOverlay == null ? OverlayTexture.NO_OVERLAY : breakingOverlay.progress(),
            0
        );

        poseStack.popPose();
    }

    public static class State {
        public DecoratedPotBlockEntity.WobbleStyle wobbleStyle = DecoratedPotBlockEntity.WobbleStyle.POSITIVE;
        public float wobble;
        public Direction facing;
        public final List<BakedQuad> regularQuads = new ArrayList<>(), patternQuads = new ArrayList<>(4);
        public IntList tintLayers;
    }
}
