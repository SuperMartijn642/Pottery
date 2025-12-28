package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.CustomBlockEntityRenderer;
import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        state.pos = entity.getBlockPos();
        state.blockState = entity.getBlockState();
        state.level = entity.getLevel();
    }

    @Override
    public void submit(SubmitNodeCollector output, State state, RenderContext context){
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();

        boolean cullFaces = true;
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
            cullFaces = false;
        }

        // Render the regular block
        BlockRenderDispatcher blockRenderer = ClientUtils.getBlockRenderer();
        BlockStateModel model = blockRenderer.getBlockModel(state.blockState);
        ModelFeatureRenderer.CrumblingOverlay breakingOverlay = context.breakingOverlay();
        output.submitBlockStateModel(
            poseStack,
            RenderLayerHelper::getEntityBlockLayer,
            model,
            1, 1, 1,
            context.packedLight(),
            breakingOverlay == null ? OverlayTexture.NO_OVERLAY : breakingOverlay.progress(),
            0,
            state.level == null ? EmptyBlockAndTintGetter.INSTANCE : state.level,
            state.pos,
            state.blockState
        );

        poseStack.popPose();
    }

    public static class State {
        private DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
        private float wobble;
        private Direction facing;
        private BlockPos pos;
        private BlockState blockState;

        // TODO remove this once Fabric has alternative for supplying context to block models
        private Level level;
    }
}
