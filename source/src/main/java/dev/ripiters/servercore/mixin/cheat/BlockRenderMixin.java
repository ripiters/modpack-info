package dev.ripiters.servercore.mixin.cheat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Block.class)
public abstract class BlockRenderMixin {

    @Unique
    private static Set<Block> serverCore$groundBlocks;

    // Lazy initialization to prevent NPE during early Bootstrap class loading fixme: LOAD / Add tag
    @Unique
    private static Set<Block> serverCore$getGroundBlocks() {
        if (serverCore$groundBlocks == null) {
            serverCore$groundBlocks = Set.of(
                    Blocks.STONE,
                    Blocks.DEEPSLATE,
                    Blocks.GRANITE,
                    Blocks.DIORITE,
                    Blocks.ANDESITE,
                    Blocks.TUFF,
                    Blocks.NETHERRACK,
                    Blocks.END_STONE,
                    Blocks.DIRT,
                    Blocks.GRASS_BLOCK,
                    Blocks.PODZOL,
                    Blocks.MYCELIUM,
                    Blocks.COARSE_DIRT,
                    Blocks.ROOTED_DIRT,
                    Blocks.MUD,
                    Blocks.CLAY,
                    Blocks.SAND,
                    Blocks.RED_SAND,
                    Blocks.GRAVEL,
                    Blocks.BASALT,
                    Blocks.BLACKSTONE,
                    Blocks.SMOOTH_BASALT
            );
        }
        return serverCore$groundBlocks;
    }

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, remap = false)
    private static void forceOcclusionForAntiXray(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState neighborState = level.getBlockState(pos);

        // Cull face if it touches internal terrain blocks
        if (serverCore$getGroundBlocks().contains(neighborState.getBlock())) {
            cir.setReturnValue(false);
        }
    }
}