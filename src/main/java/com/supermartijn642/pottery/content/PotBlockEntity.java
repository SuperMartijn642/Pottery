package com.supermartijn642.pottery.content;

import com.supermartijn642.core.block.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlockEntity extends BaseBlockEntity implements ContainerSingleItem {

    private DecoratedPotBlockEntity.Decorations decorations = DecoratedPotBlockEntity.Decorations.EMPTY;
    private ItemStack items = ItemStack.EMPTY;
    public long wobbleStartedAtTick;
    @Nullable
    public WobbleStyle lastWobbleStyle;

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

    public Direction getFacing(){
        return this.getBlockState().getValue(PotBlock.HORIZONTAL_FACING);
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

    public void wobble(WobbleStyle style){
        if(this.level == null || this.level.isClientSide)
            return;
        this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, style.ordinal());
    }

    @Override
    public boolean triggerEvent(int identifier, int data){
        if(this.level != null && identifier == 1 && data >= 0 && data < WobbleStyle.values().length){
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = WobbleStyle.values()[data];
            return true;
        }
        return super.triggerEvent(identifier, data);
    }

    @Override
    protected CompoundTag writeData(){
        CompoundTag data = new CompoundTag();
        if(this.decorations.equals(DecoratedPotBlockEntity.Decorations.EMPTY))
            data.putBoolean("decorationsEmpty", true);
        else
            this.decorations.save(data);
        data.put("items", this.items.save(new CompoundTag()));
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag compound){
        super.saveAdditional(compound);
        this.decorations.save(compound);
    }

    @Override
    protected CompoundTag writeItemStackData(){
        CompoundTag data = this.writeData();
        if(data != null)
            data.remove("decorationsEmpty");
        return data;
    }

    @Override
    protected void readData(CompoundTag data){
        this.decorations = DecoratedPotBlockEntity.Decorations.load(data);
        this.items = data.contains("items", Tag.TAG_COMPOUND) ? ItemStack.of(data.getCompound("items")) : ItemStack.EMPTY;
    }

    public enum WobbleStyle {
        POSITIVE(7),
        NEGATIVE(10);

        public final int duration;

        WobbleStyle(int duration){
            this.duration = duration;
        }
    }
}
