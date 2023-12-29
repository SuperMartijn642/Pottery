package com.supermartijn642.pottery.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Consumer;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotEventHandlers {

    public static void registerListeners(){
        NeoForge.EVENT_BUS.addListener((Consumer<PlayerInteractEvent.RightClickBlock>)event -> {
            InteractionResult result = onInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if(result != InteractionResult.PASS){
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
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
        return InteractionResult.PASS;
    }
}
