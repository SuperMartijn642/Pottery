package com.supermartijn642.pottery.content;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlock;
import com.supermartijn642.core.block.BlockProperties;
import com.supermartijn642.core.block.EntityHoldingBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotBlock extends BaseBlock implements EntityHoldingBlock, SimpleWaterloggedBlock {

    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
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

            if(!level.isClientSide()){
                BlockEntity entity = level.getBlockEntity(pos);
                if(!(entity instanceof PotBlockEntity))
                    return InteractionFeedback.CONSUME;
                PotDecorations decorations = ((PotBlockEntity)entity).getDecorations();
                if(this.type == PotType.DEFAULT && color == PotColor.BLANK){
                    BlockState newState = Blocks.DECORATED_POT.defaultBlockState()
                        .setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING))
                        .setValue(CRACKED, state.getValue(CRACKED))
                        .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                    level.setBlock(pos, newState, Block.UPDATE_ALL);
                    entity = level.getBlockEntity(pos);
                    if(entity instanceof DecoratedPotBlockEntity){
                        ((DecoratedPotBlockEntity)entity).decorations = decorations;
                        entity.setChanged();
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                    }
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
                PotDecorations decorations = entity.getDecorations();
                Optional<Item> oldItem = DecorationUtils.getDecorationItem(decorations, state.getValue(HORIZONTAL_FACING), hitSide);
                if(stack.is(oldItem.orElse(Items.BRICK)))
                    return InteractionFeedback.CONSUME;

                if(!level.isClientSide()){
                    // Update the decorations
                    PotDecorations newDecorations = DecorationUtils.setDecorationItem(decorations, state.getValue(HORIZONTAL_FACING), hitSide, Optional.of(stack.getItem()));
                    entity.updateDecorations(newDecorations);
                    if(!player.isCreative()){
                        // Decrease the held stack size by 1
                        stack = stack.copy();
                        stack.shrink(1);
                        player.setItemInHand(hand, stack);
                        // Re-add the previous item
                        player.getInventory().placeItemBackInInventory(oldItem.orElse(Items.BRICK).getDefaultInstance());
                    }
                }
                return InteractionFeedback.SUCCESS;
            }
        }

        // Storing items
        if(level.getBlockEntity(pos) instanceof PotBlockEntity entity){
            ItemStack stored = entity.getTheItem();
            if(!stack.isEmpty() && (stored.isEmpty() || (ItemStack.isSameItemSameComponents(stack, stored) && stored.getCount() < stored.getMaxStackSize()))){
                entity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                float fillPercentage;
                if(stored.isEmpty())
                    stored = player.isCreative() ? stack.copyWithCount(1) : stack.split(1);
                else
                    stored.grow(player.isCreative() ? 1 : stack.split(1).getCount());
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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction side, BlockPos neighborPos, BlockState neighborState, RandomSource random){
        if(state.getValue(WATERLOGGED))
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, tickAccess, pos, side, neighborPos, neighborState, random);
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
                entity.getDecorations().ordered().stream().map(Item::getDefaultInstance).forEach(consumer);
                for(int i = 0; i < this.type.getExtraBricks(); i++)
                    consumer.accept(Items.BRICK.getDefaultInstance());
            });
        return super.getDrops(state, builder);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player){
        ItemStack stack = player.getMainHandItem();
        if(stack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasTag(stack, EnchantmentTags.PREVENTS_DECORATED_POT_SHATTERING)){
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
    public void appendItemInformation(ItemStack stack, Consumer<Component> info, boolean advanced){
        super.appendItemInformation(stack, info, advanced);
        PotDecorations decorations = stack.get(DataComponents.POT_DECORATIONS);
        if(decorations != null && !decorations.equals(PotDecorations.EMPTY)){
            info.accept(CommonComponents.EMPTY);
            info.accept(TextComponents.string("Patterns:").color(ChatFormatting.GRAY).get());
            info.accept(TextComponents.string(" Front: ").color(ChatFormatting.DARK_GRAY).append(decorations.front().orElse(Items.BRICK).getName().plainCopy().withStyle(decorations.front().isEmpty() ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Left: ").color(ChatFormatting.DARK_GRAY).append(decorations.left().orElse(Items.BRICK).getName().plainCopy().withStyle(decorations.left().isEmpty() ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Right: ").color(ChatFormatting.DARK_GRAY).append(decorations.right().orElse(Items.BRICK).getName().plainCopy().withStyle(decorations.right().isEmpty() ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
            info.accept(TextComponents.string(" Back: ").color(ChatFormatting.DARK_GRAY).append(decorations.back().orElse(Items.BRICK).getName().plainCopy().withStyle(decorations.back().isEmpty() ? ChatFormatting.GRAY : ChatFormatting.GOLD)).get());
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData){
        if(level.getBlockEntity(pos) instanceof PotBlockEntity entity)
            return entity.itemFromDecorations();
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult blockHitResult, Projectile projectile){
        BlockPos pos = blockHitResult.getBlockPos();
        if(level instanceof ServerLevel && projectile.mayInteract((ServerLevel)level, pos) && projectile.mayBreak((ServerLevel)level)){
            level.setBlock(pos, state.setValue(CRACKED, true), 4);
            level.destroyBlock(pos, true, projectile);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state){
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side){
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public RenderShape getRenderShape(BlockState state){
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int identifier, int data){
        BlockEntity entity;
        return super.triggerEvent(state, level, pos, identifier, data)
            || ((entity = level.getBlockEntity(pos)) != null && entity.triggerEvent(identifier, data));
    }
}
