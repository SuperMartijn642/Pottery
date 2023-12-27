package com.supermartijn642.pottery.content;

import com.supermartijn642.core.block.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlockEntity extends BaseBlockEntity implements RandomizableContainer, ContainerSingleItem {

    private DecoratedPotBlockEntity.Decorations decorations = DecoratedPotBlockEntity.Decorations.EMPTY;
    private ItemStack items = ItemStack.EMPTY;
    public long wobbleStartedAtTick;
    @Nullable
    public DecoratedPotBlockEntity.WobbleStyle lastWobbleStyle;
    @Nullable
    protected ResourceLocation lootTable;
    protected long lootTableSeed;

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

    @Nullable
    @Override
    public ResourceLocation getLootTable(){
        return this.lootTable;
    }

    @Override
    public void setLootTable(ResourceLocation lootTable){
        this.lootTable = lootTable;
    }

    @Override
    public long getLootTableSeed(){
        return this.lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long lootTableSeed){
        this.lootTableSeed = lootTableSeed;
    }

    @Override
    public ItemStack getTheItem(){
        this.unpackLootTable(null);
        return this.items;
    }

    @Override
    public ItemStack splitTheItem(int count){
        this.unpackLootTable(null);
        ItemStack split = this.items.split(count);
        if(this.items.isEmpty())
            this.items = ItemStack.EMPTY;
        return split;
    }

    @Override
    public void setTheItem(ItemStack stack){
        this.unpackLootTable(null);
        this.items = stack;
    }

    @Override
    public BlockEntity getContainerBlockEntity(){
        return this;
    }

    public void wobble(DecoratedPotBlockEntity.WobbleStyle style){
        if(this.level == null || this.level.isClientSide)
            return;
        this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, style.ordinal());
    }

    @Override
    public boolean triggerEvent(int identifier, int data){
        if(this.level != null && identifier == 1 && data >= 0 && data < DecoratedPotBlockEntity.WobbleStyle.values().length){
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[data];
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
        if(!this.trySaveLootTable(data) && !this.items.isEmpty())
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
        if(!this.tryLoadLootTable(data))
            this.items = data.contains("items", Tag.TAG_COMPOUND) ? ItemStack.of(data.getCompound("items")) : ItemStack.EMPTY;
    }
}
