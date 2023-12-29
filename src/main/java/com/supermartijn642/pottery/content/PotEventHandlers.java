package com.supermartijn642.pottery.content;

import com.supermartijn642.pottery.extensions.PotteryDecoratedPotBlockEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotEventHandlers {

    public static void registerListeners(){
        UseBlockCallback.EVENT.register(PotEventHandlers::onInteract);
    }

    private static InteractionResult onInteract(Player player, Level level, InteractionHand hand, BlockHitResult hitResult){
        if(player.isSpectator())
            return InteractionResult.PASS;

        // Cauldron
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if(state.is(Blocks.WATER_CAULDRON)){
            ItemStack stack = player.getItemInHand(hand);
            if(stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof PotBlock block){
                if(block.getColor() == PotColor.BLANK)
                    return InteractionResult.CONSUME;

                if(!level.isClientSide){
                    ItemStack newStack = new ItemStack(block.getType().getItem(PotColor.BLANK), stack.getCount());
                    if(stack.hasTag())
                        newStack.setTag(stack.getOrCreateTag().copy());
                    player.setItemInHand(hand, newStack);
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Vanilla decorated pot
        if(!state.is(Blocks.DECORATED_POT))
            return InteractionResult.PASS;

        // Coloring
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof DyeItem){
            PotColor color = PotColor.colorForDye(((DyeItem)stack.getItem()).getDyeColor());
            if(color == null || color == PotColor.BLANK)
                return InteractionResult.CONSUME;

            if(!level.isClientSide){
                BlockEntity entity = level.getBlockEntity(pos);
                if(!(entity instanceof DecoratedPotBlockEntity))
                    return InteractionResult.CONSUME;
                DecoratedPotBlockEntity.Decorations decorations = ((DecoratedPotBlockEntity)entity).getDecorations();
                BlockState newState = PotType.DEFAULT.getBlock(color).defaultBlockState()
                    .setValue(PotBlock.HORIZONTAL_FACING, state.getValue(PotBlock.HORIZONTAL_FACING))
                    .setValue(PotBlock.CRACKED, state.getValue(PotBlock.CRACKED))
                    .setValue(PotBlock.WATERLOGGED, state.getValue(PotBlock.WATERLOGGED));
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                entity = level.getBlockEntity(pos);
                if(entity instanceof PotBlockEntity)
                    ((PotBlockEntity)entity).updateDecorations(decorations);

                if(!player.isCreative()){
                    stack = stack.copy();
                    stack.shrink(1);
                    player.setItemInHand(hand, stack);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Changing sherds
        Direction hitSide = hitResult.getDirection();
        if(hitSide.getAxis().isHorizontal()){
            if(stack.is(ItemTags.DECORATED_POT_INGREDIENTS) && level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity entity){
                DecoratedPotBlockEntity.Decorations decorations = entity.getDecorations();
                Item oldItem = DecorationUtils.getDecorationItem(decorations, state.getValue(PotBlock.HORIZONTAL_FACING), hitSide);
                if(stack.is(oldItem))
                    return InteractionResult.CONSUME;

                if(!level.isClientSide){
                    // Update the decorations
                    DecoratedPotBlockEntity.Decorations newDecorations = DecorationUtils.setDecorationItem(decorations, state.getValue(PotBlock.HORIZONTAL_FACING), hitSide, stack.getItem());
                    entity.setFromItem(DecoratedPotBlockEntity.createDecoratedPotItem(newDecorations));
                    entity.setChanged();
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                    if(!player.isCreative()){
                        // Decrease the held stack size by 1
                        stack = stack.copy();
                        stack.shrink(1);
                        player.setItemInHand(hand, stack);
                        // Re-add the previous item
                        player.getInventory().placeItemBackInInventory(oldItem.getDefaultInstance());
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Storing items
        if(level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity entity){
            ItemStack stored = ((Container)entity).getItem(0);
            if(!stack.isEmpty() && (stored.isEmpty() || (ItemStack.isSameItemSameTags(stack, stored) && stored.getCount() < stored.getMaxStackSize()))){
                ((PotteryDecoratedPotBlockEntity)entity).potteryWobble(PotBlockEntity.WobbleStyle.POSITIVE);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                float fillPercentage;
                if(stored.isEmpty())
                    stored = player.isCreative() ? stack.copyWithCount(1) : stack.split(1);
                else
                    stored.grow(stack.split(1).getCount());
                ((Container)entity).setItem(0, stored);
                fillPercentage = (float)stored.getCount() / stored.getMaxStackSize();
                level.playSound(null, pos, SoundEvents.DECORATED_POT_STEP, SoundSource.BLOCKS, 1.0f, 0.7f + 0.5f * fillPercentage);
                if(level instanceof ServerLevel)
                    ((ServerLevel)level).sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 7, 0, 0, 0, 0);
                entity.setChanged();
            }else{
                level.playSound(null, pos, SoundEvents.WAXED_SIGN_INTERACT_FAIL, SoundSource.BLOCKS, 1, 1);
                ((PotteryDecoratedPotBlockEntity)entity).potteryWobble(PotBlockEntity.WobbleStyle.NEGATIVE);
            }
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }
}
