package com.supermartijn642.pottery.mixin;

import com.supermartijn642.pottery.content.PotBlock;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Created 08/07/2025 by SuperMartijn642
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(
        method = "addToTooltip",
        at = @At("HEAD"),
        cancellable = true
    )
    public void addToTooltip(DataComponentType<?> componentType, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
        //noinspection DataFlowIssue,ConstantValue
        if(componentType == DataComponents.POT_DECORATIONS
            && ((ItemStack)(Object)this).getItem() instanceof BlockItem item && item.getBlock() instanceof PotBlock)
            ci.cancel();
    }
}
