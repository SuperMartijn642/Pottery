package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.CustomBlockEntityRenderer;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Created 27/12/2023 by SuperMartijn642
 */
public class PotBlockRenderer implements CustomBlockEntityRenderer<PotBlockEntity,PotBlockRenderer.State> {

    private static final Matrix4fc IDENTITY_MATRIX = new Matrix4f().identity();

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
        state.blockRenderState.clear();
        BlockState blockState = entity.getBlockState();
        BlockPos pos = entity.getBlockPos();
        BlockAndTintGetter level = entity.getLevel() instanceof BlockAndTintGetter l ? l : BlockAndTintGetter.EMPTY;
        BlockStateModel model = ClientUtils.getMinecraft().getModelManager().getBlockStateModelSet().get(blockState);
        long seed = blockState.getSeed(pos);
        RandomSource random = context.randomSource(seed);
        QuadEmitter emitter = state.blockRenderState.setupMesh(IDENTITY_MATRIX, model.hasMaterialFlag(level, pos, blockState, random, BakedQuad.FLAG_TRANSLUCENT));
        random.setSeed(seed);
        model.emitQuads(emitter, level, pos, blockState, random, _ -> false);
        IntList tintLayers = state.blockRenderState.tintLayers();
        for(BlockTintSource tintSource : ClientUtils.getMinecraft().getBlockColors().getTintSources(blockState))
            tintLayers.add(tintSource.colorInWorld(blockState, level, pos));
    }

    @Override
    public void submit(SubmitNodeCollector output, State state, RenderContext context){
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();

        if(state.wobbleStyle != null && state.wobble >= 0 && state.wobble <= 1){
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
            if(state.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE){
                float rotation = state.wobble * ((float)Math.PI * 2);
                float xAxis = -1.5f * (Mth.cos(rotation) + 0.5f) * Mth.sin(rotation / 2);
                poseStack.mulPose(Axis.XP.rotation(xAxis * 0.015625f));
                float zAxis = Mth.sin(rotation);
                poseStack.mulPose(Axis.ZP.rotation(zAxis * 0.015625f));
            }else{
                float h = Mth.sin(-state.wobble * 3 * (float)Math.PI) * 0.125f;
                float k = 1 - state.wobble;
                poseStack.mulPose(Axis.YP.rotation(h * k));
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(state.facing.toYRot() - 180));
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        // Render the regular block
        ModelFeatureRenderer.CrumblingOverlay breakingOverlay = context.breakingOverlay();
        state.blockRenderState.submit(poseStack, output, context.packedLight(), breakingOverlay == null ? OverlayTexture.NO_OVERLAY : breakingOverlay.progress(), 0);

        poseStack.popPose();
    }

    public static class State {
        private DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
        private float wobble;
        private Direction facing;
        private final BlockModelRenderState blockRenderState = new BlockModelRenderState();
    }
}
