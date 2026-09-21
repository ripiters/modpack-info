package dev.ripiters.servercore.mixin.copperagebackport;

import com.github.smallinger.copperagebackport.block.CopperLanternBlock;
import net.mehvahdjukaar.amendments.common.entity.FallingLanternEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.smallinger.copperagebackport.block.CopperLanternBlock")
public abstract class CopperLanternFallingMixin {

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true, remap = false)
    private void triggerFallingLantern(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        Direction connectedDirection = state.getValue(CopperLanternBlock.HANGING) ? Direction.DOWN : Direction.UP;
        if (connectedDirection.getOpposite() == direction && !state.canSurvive(level, pos)) {
            if (FallingLanternEntity.maybeFall(false, state, pos, level)) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }
}