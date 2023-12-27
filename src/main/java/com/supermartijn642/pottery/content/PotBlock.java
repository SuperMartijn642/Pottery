package com.supermartijn642.pottery.content;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlock;
import com.supermartijn642.core.block.BlockProperties;
import com.supermartijn642.core.block.EntityHoldingBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlock extends BaseBlock implements EntityHoldingBlock, SimpleWaterloggedBlock {

    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CRACKED = BlockStateProperties.CRACKED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final PotType type;
    private final PotColor color;

    public PotBlock(PotType type, PotColor color){
        //noinspection deprecation
        super(false, BlockProperties.create().mapColor(MapColor.NONE).destroyTime(0).explosionResistance(0).noOcclusion().toUnderlying().pushReaction(PushReaction.DESTROY));
        this.type = type;
        this.color = color;

        this.registerDefaultState(this.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(CRACKED, false).setValue(WATERLOGGED, false));
    }

    public PotType getType(){
        return this.type;
    }

    public PotColor getColor(){
        return this.color;
    }

    @Override
    protected InteractionFeedback interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, Direction hitSide, Vec3 hitLocation){
        // Coloring
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof DyeItem){
            PotColor color = PotColor.colorForDye(((DyeItem)stack.getItem()).getDyeColor());
            if(color == null || color == this.color)
                return InteractionFeedback.CONSUME;

            if(!level.isClientSide){
                BlockEntity entity = level.getBlockEntity(pos);
                if(!(entity instanceof PotBlockEntity))
                    return InteractionFeedback.CONSUME;
                DecoratedPotBlockEntity.Decorations decorations = ((PotBlockEntity)entity).getDecorations();
                if(this.type == PotType.DEFAULT && color == PotColor.BLANK){
                    BlockState newState = Blocks.DECORATED_POT.defaultBlockState()
                        .setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING))
                        .setValue(CRACKED, state.getValue(CRACKED))
                        .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                    level.setBlock(pos, newState, Block.UPDATE_ALL);
                    entity = level.getBlockEntity(pos);
                    if(entity instanceof DecoratedPotBlockEntity)
                        ((DecoratedPotBlockEntity)entity).setFromItem(DecoratedPotBlockEntity.createDecoratedPotItem(decorations));
                }else{
                    BlockState newState = this.type.getBlock(color).defaultBlockState()
                        .setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING))
                        .setValue(CRACKED, state.getValue(CRACKED))
                        .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                    level.setBlock(pos, newState, Block.UPDATE_ALL);
                    entity = level.getBlockEntity(pos);
                    if(entity instanceof PotBlockEntity)
                        ((PotBlockEntity)entity).updateDecorations(decorations);
                }

                if(!player.isCreative()){
                    stack = stack.copy();
                    stack.shrink(1);
                    player.setItemInHand(hand, stack);
                }
            }
            return InteractionFeedback.SUCCESS;
        }

        // Changing sherds
        if(hitSide.getAxis().isHorizontal()){
            if(stack.is(ItemTags.DECORATED_POT_INGREDIENTS) && level.getBlockEntity(pos) instanceof PotBlockEntity entity){
                DecoratedPotBlockEntity.Decorations decorations = entity.getDecorations();
                Item oldItem = DecorationUtils.getDecorationItem(decorations, state.getValue(HORIZONTAL_FACING), hitSide);
                if(stack.is(oldItem))
                    return InteractionFeedback.CONSUME;

                if(!level.isClientSide){
                    // Update the decorations
                    DecoratedPotBlockEntity.Decorations newDecorations = DecorationUtils.setDecorationItem(decorations, state.getValue(HORIZONTAL_FACING), hitSide, stack.getItem());
                    entity.updateDecorations(newDecorations);
                    if(!player.isCreative()){
                        // Decrease the held stack size by 1
                        stack = stack.copy();
                        stack.shrink(1);
                        player.setItemInHand(hand, stack);
                        // Re-add the previous item
                        player.getInventory().placeItemBackInInventory(oldItem.getDefaultInstance());
                    }
                }
                return InteractionFeedback.SUCCESS;
            }
        }

        // Storing items
        if(level.getBlockEntity(pos) instanceof PotBlockEntity entity){
            ItemStack stored = entity.getTheItem();
            if(!stack.isEmpty() && (stored.isEmpty() || (ItemStack.isSameItemSameTags(stack, stored) && stored.getCount() < stored.getMaxStackSize()))){
                entity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                float fillPercentage;
                if(stored.isEmpty())
                    stored = player.isCreative() ? stack.copyWithCount(1) : stack.split(1);
                else
                    stored.grow(1);
                entity.setTheItem(stored);
                fillPercentage = (float)stored.getCount() / stored.getMaxStackSize();
                level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0f, 0.7f + 0.5f * fillPercentage);
                if(level instanceof ServerLevel)
                    ((ServerLevel)level).sendParticles(ParticleTypes.DUST_PLUME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 7, 0, 0, 0, 0);
                entity.setChanged();
            }else{
                level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1, 1);
                entity.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
            }
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionFeedback.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        return this.defaultBlockState()
            .setValue(HORIZONTAL_FACING, context.getHorizontalDirection())
            .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean bl){
        Containers.dropContentsOnDestroy(oldState, newState, level, pos);
        super.onRemove(oldState, level, pos, newState, bl);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos){
        if(state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, side, neighborState, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
        if(level.getBlockEntity(pos) instanceof PotBlockEntity entity)
            entity.decorationsFromItem(stack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(HORIZONTAL_FACING, CRACKED, WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context){
        //noinspection deprecation
        return this.type.getShape().getUnderlying();
    }

    @Override
    public BlockEntity createNewBlockEntity(BlockPos pos, BlockState state){
        return new PotBlockEntity(this.type, pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder){
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PotBlockEntity entity)
            builder.withDynamicDrop(DecoratedPotBlock.SHERDS_DYNAMIC_DROP_ID, consumer -> {
                entity.getDecorations().sorted().map(Item::getDefaultInstance).forEach(consumer);
                for(int i = 0; i < this.type.getExtraBricks(); i++)
                    consumer.accept(Items.BRICK.getDefaultInstance());
            });
        return super.getDrops(state, builder);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player){
        ItemStack stack = player.getMainHandItem();
        if(stack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasSilkTouch(stack)){
            state = state.setValue(CRACKED, true);
            level.setBlock(pos, state, 4);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public FluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public SoundType getSoundType(BlockState state){
        return state.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
    }

    @Override
    protected void appendItemInformation(ItemStack stack, @Nullable BlockGetter level, Consumer<Component> info, boolean advanced){
        super.appendItemInformation(stack, level, info, advanced);
        DecoratedPotBlockEntity.Decorations decorations = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(stack));
        if(decorations != DecoratedPotBlockEntity.Decorations.EMPTY){
            info.accept(CommonComponents.EMPTY);
            info.accept(TextComponents.string("Patterns:").color(ChatFormatting.GRAY).get());
            info.accept(TextComponents.string(" Front: ").color(ChatFormatting.DARK_GRAY).append(decorations.front().getName(ItemStack.EMPTY).plainCopy().withStyle(decorations.front() == Items.BRICK ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Left: ").color(ChatFormatting.DARK_GRAY).append(decorations.left().getName(ItemStack.EMPTY).plainCopy().withStyle(decorations.left() == Items.BRICK ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Right: ").color(ChatFormatting.DARK_GRAY).append(decorations.right().getName(ItemStack.EMPTY).plainCopy().withStyle(decorations.right() == Items.BRICK ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Back: ").color(ChatFormatting.DARK_GRAY).append(decorations.back().getName(ItemStack.EMPTY).plainCopy().withStyle(decorations.back() == Items.BRICK ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state){
        if(level.getBlockEntity(pos) instanceof PotBlockEntity entity)
            return entity.itemFromDecorations();
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult blockHitResult, Projectile projectile){
        BlockPos pos = blockHitResult.getBlockPos();
        if(!level.isClientSide && projectile.mayInteract(level, pos) && projectile.mayBreak(level)){
            level.setBlock(pos, state.setValue(CRACKED, true), 4);
            level.destroyBlock(pos, true, projectile);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState){
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blocstatesState, Level level, BlockPos pos){
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState){
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int identifier, int data){
        BlockEntity entity;
        return super.triggerEvent(state, level, pos, identifier, data)
            || ((entity = level.getBlockEntity(pos)) != null && entity.triggerEvent(identifier, data));
    }
}
