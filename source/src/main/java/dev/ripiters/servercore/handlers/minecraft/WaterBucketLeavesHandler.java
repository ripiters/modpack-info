package dev.ripiters.servercore.handlers.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class WaterBucketLeavesHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (player.isShiftKeyDown()) {
            ItemStack stack = event.getItemStack();
            if (stack.is(Items.WATER_BUCKET)) {
                Level level = event.getLevel();
                BlockPos pos = event.getPos();
                BlockState state = level.getBlockState(pos);

                if (state.getBlock() instanceof LeavesBlock) {
                    Direction face = event.getFace();
                    if (face != null) {
                        BlockPos targetPos = pos.relative(face);
                        BlockState targetState = level.getBlockState(targetPos);

                        if (targetState.canBeReplaced()) {
                            if (!level.isClientSide) {
                                level.setBlock(targetPos, Blocks.WATER.defaultBlockState(), 3);

                                level.playSound(null, targetPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

                                if (!player.getAbilities().instabuild) {
                                    event.getItemStack().shrink(1);
                                    if (event.getItemStack().isEmpty()) {
                                        player.setItemInHand(event.getHand(), new ItemStack(Items.BUCKET));
                                    } else {
                                        if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                                            player.drop(new ItemStack(Items.BUCKET), false);
                                        }
                                    }
                                }
                            }

                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                        }
                    }
                }
            }
        }
    }
}