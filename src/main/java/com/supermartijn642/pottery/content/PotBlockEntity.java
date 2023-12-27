package com.supermartijn642.pottery.content;

import com.supermartijn642.core.block.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlockEntity extends BaseBlockEntity {

    private DecoratedPotBlockEntity.Decorations decorations = DecoratedPotBlockEntity.Decorations.EMPTY;

    public PotBlockEntity(PotType type, BlockPos pos, BlockState state){
        super(type.getBlockEntityType(), pos, state);
    }

    public void decorationsFromItem(ItemStack stack){
        this.decorations = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(stack));
        this.dataChanged();
    }

    public ItemStack itemFromDecorations(){
        ItemStack stack = new ItemStack(this.getBlockState().getBlock());
        BlockItem.setBlockEntityData(stack, this.getType(), this.decorations.save(new CompoundTag()));
        return stack;
    }

    public DecoratedPotBlockEntity.Decorations getDecorations(){
        return this.decorations;
    }

    public void updateDecorations(DecoratedPotBlockEntity.Decorations decorations){
        this.decorations = decorations;
        this.dataChanged();
    }

    @Override
    protected CompoundTag writeData(){
        CompoundTag data = new CompoundTag();
        if(this.decorations.equals(DecoratedPotBlockEntity.Decorations.EMPTY))
            data.putBoolean("decorationsEmpty", true);
        else
            this.decorations.save(data);
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag compound){
        super.saveAdditional(compound);
        this.decorations.save(compound);
    }

    @Override
    protected void readData(CompoundTag tag){
        this.decorations = DecoratedPotBlockEntity.Decorations.load(tag);
        if(this.level != null && this.level.isClientSide)
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 0);
    }
}
