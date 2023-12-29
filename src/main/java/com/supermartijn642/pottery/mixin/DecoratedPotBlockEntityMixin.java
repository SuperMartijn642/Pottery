package com.supermartijn642.pottery.mixin;

import com.supermartijn642.pottery.content.PotBlockEntity;
import com.supermartijn642.pottery.extensions.PotteryDecoratedPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 29/12/2023 by SuperMartijn642
 */
@Mixin(DecoratedPotBlockEntity.class)
public abstract class DecoratedPotBlockEntityMixin extends BlockEntity implements ContainerSingleItem, PotteryDecoratedPotBlockEntity {

    @Unique
    private ItemStack items = ItemStack.EMPTY;
    @Unique
    public long wobbleStartedAtTick;
    @Nullable
    @Unique
    public PotBlockEntity.WobbleStyle lastWobbleStyle;

    public DecoratedPotBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState){
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public boolean triggerEvent(int identifier, int data){
        if(this.level != null && identifier == 1 && data >= 0 && data < PotBlockEntity.WobbleStyle.values().length){
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = PotBlockEntity.WobbleStyle.values()[data];
            return true;
        }
        return super.triggerEvent(identifier, data);
    }

    @Inject(
        method = "saveAdditional",
        at = @At("TAIL")
    )
    private void saveAdditional(CompoundTag data, CallbackInfo ci){
        data.put("items", this.items.save(new CompoundTag()));
    }

    @Inject(
        method = "load",
        at = @At("TAIL")
    )
    private void load(CompoundTag data, CallbackInfo ci){
        this.items = data.contains("items", Tag.TAG_COMPOUND) ? ItemStack.of(data.getCompound("items")) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItem(int index){
        if(index != 0)
            throw new IllegalArgumentException("Invalid index '" + index + "'!");
        return this.items;
    }

    @Override
    public ItemStack removeItem(int index, int amount){
        if(index != 0)
            throw new IllegalArgumentException("Invalid index '" + index + "'!");
        this.setChanged();
        return this.items.split(amount);
    }

    @Override
    public void setItem(int index, ItemStack stack){
        if(index != 0)
            throw new IllegalArgumentException("Invalid index '" + index + "'!");
        this.items = stack;
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player){
        return false;
    }

    @Override
    public void potteryWobble(PotBlockEntity.WobbleStyle style){
        //noinspection DataFlowIssue
        BlockEntity entity = (BlockEntity)(Object)this;
        Level level = entity.getLevel();
        if(level == null || level.isClientSide)
            return;
        level.blockEvent(entity.getBlockPos(), entity.getBlockState().getBlock(), 1, style.ordinal());
    }

    @Override
    public long potteryGetWobbleStartedAtTick(){
        return this.wobbleStartedAtTick;
    }

    @Override
    public PotBlockEntity.WobbleStyle potteryGetLastWobbleStyle(){
        return this.lastWobbleStyle;
    }
}
