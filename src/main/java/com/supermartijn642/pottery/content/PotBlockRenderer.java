package com.supermartijn642.pottery.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.render.CustomBlockEntityRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 27/12/2023 by SuperMartijn642
 */
public class PotBlockRenderer implements CustomBlockEntityRenderer<PotBlockEntity> {
    @Override
    public void render(PotBlockEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay){
        poseStack.pushPose();
        DecoratedPotBlockEntity.WobbleStyle wobbleStyle = entity.lastWobbleStyle;
        boolean cullFaces = true;
        if(wobbleStyle != null && entity.hasLevel()){
            float wobble = (entity.getLevel().getGameTime() - entity.wobbleStartedAtTick + partialTicks) / wobbleStyle.duration;
            if(wobble >= 0 && wobble <= 1){
                Direction facing = entity.getFacing();
                poseStack.translate(0.5, 0.0, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
                if(wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE){
                    float rotation = wobble * ((float)Math.PI * 2);
                    float xAxis = -1.5f * (Mth.cos(rotation) + 0.5f) * Mth.sin(rotation / 2);
                    poseStack.mulPose(Axis.XP.rotation(xAxis * 0.015625f));
                    float zAxis = Mth.sin(rotation);
                    poseStack.mulPose(Axis.ZP.rotation(zAxis * 0.015625f));
                }else{
                    float h = Mth.sin(-wobble * 3 * (float)Math.PI) * 0.125f;
                    float k = 1 - wobble;
                    poseStack.mulPose(Axis.YP.rotation(h * k));
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot() - 180));
                poseStack.translate(-0.5, 0.0, -0.5);
                cullFaces = false;
            }
        }

        // Render the regular block
        BlockRenderDispatcher blockRenderer = ClientUtils.getBlockRenderer();
        BlockState state = entity.getBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);
        blockRenderer.getModelRenderer().tesselateBlock(entity.getLevel(), model, state, entity.getBlockPos(), poseStack, bufferSource.getBuffer(ItemBlockRenderTypes.getChunkRenderType(state)), cullFaces, entity.getLevel().random, combinedLight, combinedOverlay);

        poseStack.popPose();
    }
}
