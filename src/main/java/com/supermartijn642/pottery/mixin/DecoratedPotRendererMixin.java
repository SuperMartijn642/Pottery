package com.supermartijn642.pottery.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supermartijn642.pottery.content.PotBlockEntity;
import com.supermartijn642.pottery.extensions.PotteryDecoratedPotBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 29/12/2023 by SuperMartijn642
 */
@Mixin(DecoratedPotRenderer.class)
public class DecoratedPotRendererMixin {

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
            shift = At.Shift.AFTER
        )
    )
    private void render(DecoratedPotBlockEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, CallbackInfo ci){
        PotBlockEntity.WobbleStyle wobbleStyle = ((PotteryDecoratedPotBlockEntity)entity).potteryGetLastWobbleStyle();
        if(wobbleStyle != null && entity.hasLevel()){
            float wobble = (entity.getLevel().getGameTime() - ((PotteryDecoratedPotBlockEntity)entity).potteryGetWobbleStartedAtTick() + partialTicks) / wobbleStyle.duration;
            if(wobble >= 0 && wobble <= 1){
                if(wobbleStyle == PotBlockEntity.WobbleStyle.POSITIVE){
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
            }
        }
    }
}
