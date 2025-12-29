package com.supermartijn642.pottery.content;

import com.supermartijn642.core.block.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlockEntity extends BaseBlockEntity implements RandomizableContainer, ContainerSingleItem.BlockContainerSingleItem {

    private PotDecorations decorations = PotDecorations.EMPTY;
    private ItemStack items = ItemStack.EMPTY;
    public long wobbleStartedAtTick;
    @Nullable
    public DecoratedPotBlockEntity.WobbleStyle lastWobbleStyle;
    @Nullable
    protected ResourceKey<LootTable> lootTable;
    protected long lootTableSeed;

    public PotBlockEntity(PotType type, BlockPos pos, BlockState state){
        super(type.getBlockEntityType(), pos, state);
    }

    public ItemStack itemFromDecorations(){
        ItemStack stack = new ItemStack(this.getBlockState().getBlock());
        stack.applyComponents(this.collectComponents());
        return stack;
    }

    public PotDecorations getDecorations(){
        return this.decorations;
    }

    public void updateDecorations(PotDecorations decorations){
        this.decorations = decorations;
        this.dataChanged();
    }

    public Direction getFacing(){
        return this.getBlockState().getValue(PotBlock.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public ResourceKey<LootTable> getLootTable(){
        return this.lootTable;
    }

    @Override
    public void setLootTable(ResourceKey<LootTable> lootTable){
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
        if(this.level == null || this.level.isClientSide())
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
    protected void writeData(ValueOutput output){
        if(this.decorations != PotDecorations.EMPTY)
            output.store("sherds", PotDecorations.CODEC, this.decorations);
        if(!this.trySaveLootTable(output) && !this.items.isEmpty())
            output.store("items", ItemStack.CODEC, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);
        if(this.decorations != PotDecorations.EMPTY)
            output.store("sherds", PotDecorations.CODEC, this.decorations);
    }

    @Override
    protected void readData(ValueInput input){
        this.decorations = input.read("sherds", PotDecorations.CODEC).orElse(PotDecorations.EMPTY);
        if(!this.tryLoadLootTable(input))
            this.items = input.read("items", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder){
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.POT_DECORATIONS, this.decorations);
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.items)));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components){
        super.applyImplicitComponents(components);
        this.decorations = components.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY);
        this.items = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output){
        super.removeComponentsFromTag(output);
        output.discard("sherds");
        output.discard("item");
    }
}
