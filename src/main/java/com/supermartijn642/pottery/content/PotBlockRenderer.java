package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.CustomBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Created 27/12/2023 by SuperMartijn642
 */
public class PotBlockRenderer implements CustomBlockEntityRenderer<PotBlockEntity,PotBlockRenderer.State> {

    private static final RandomSource RANDOM_SOURCE = RandomSource.create();

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

        // Collect model parts here since block model needs world context
        BlockAndTintGetter level = entity.getLevel() == null ? EmptyBlockAndTintGetter.INSTANCE : entity.getLevel();
        BlockPos pos = entity.getBlockPos();
        RANDOM_SOURCE.setSeed(entity.getBlockState().getSeed(pos));
        BlockStateModel model = ClientUtils.getBlockRenderer().getBlockModel(entity.getBlockState());
        state.blockState = entity.getBlockState();
        state.modelData = model.getModelData(level, pos, entity.getBlockState(), ModelData.EMPTY);
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
        int combinedLight = context.packedLight();
        ModelFeatureRenderer.CrumblingOverlay breakingOverlay = context.breakingOverlay();
        ModelData modelData = state.modelData;
        BlockStateModel model = ClientUtils.getBlockRenderer().getBlockModel(state.blockState);
        for(ChunkSectionLayer layer : model.getRenderTypes(state.blockState, RANDOM_SOURCE, modelData)){
            output.submitCustomGeometry(poseStack, RenderTypeHelper.getEntityRenderType(layer), (pose, vertexConsumer) ->
                ModelBlockRenderer.renderModel(pose, vertexConsumer, model, 1, 1, 1, combinedLight, breakingOverlay == null ? OverlayTexture.NO_OVERLAY : breakingOverlay.progress(), modelData, layer)
            );
        }

        poseStack.popPose();
    }

    public static class State {
        private DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
        private float wobble;
        private Direction facing;
        private BlockState blockState;
        private ModelData modelData;
    }
}
