package com.supermartijn642.pottery.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 29/12/2023 by SuperMartijn642
 */
@Mixin(DecoratedPotBlock.class)
public abstract class DecoratedPotBlockMixin extends BaseEntityBlock {

    protected DecoratedPotBlockMixin(Properties properties){
        super(properties);
        throw new AssertionError();
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean bl){
        if(oldState.is(newState.getBlock()))
            return;
        if(level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity entity){
            Containers.dropContents(level, pos, (Container)entity);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(oldState, level, pos, newState, bl);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult blockHitResult, Projectile projectile){
        BlockPos pos = blockHitResult.getBlockPos();
        if(!level.isClientSide && projectile.mayInteract(level, pos)){
            level.setBlock(pos, state.setValue(BlockStateProperties.CRACKED, true), 4);
            level.destroyBlock(pos, true, projectile);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state){
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos){
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
