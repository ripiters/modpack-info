package dev.ripiters.servercore.mixin;

import com.mojang.logging.LogUtils;
import dev.ripiters.servercore.config.CommonConfig;
import dev.ripiters.servercore.config.ServerCoreConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(Structure.class)
public abstract class StructureMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true, remap = false)
    private void ivp$filterFlatTerrain(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        CommonConfig config = ServerCoreConfig.COMMON;

        if (!config.ivpEnabled.get() || cir.getReturnValue().isEmpty()) {
            return;
        }

        Structure structure = (Structure) (Object) this;
        ResourceLocation structureId = context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);

        if (structureId == null || !shouldFilter(structureId, config)) {
            return;
        }

        Structure.GenerationStub stub = cir.getReturnValue().get();
        int centerX = stub.position().getX();
        int centerZ = stub.position().getZ();

        if (!isFlatEnough(context, context.chunkGenerator(), centerX, centerZ, config)) {
            if (config.ivpLogFilteredStructures.get()) {
                LOGGER.debug("Filtered structure {} at {}, {} because sampled terrain variation exceeded {} blocks.", structureId, centerX, centerZ, config.ivpMaxTerrainVariation.get());
            }

            cir.setReturnValue(Optional.empty());
        }
    }

    private static boolean shouldFilter(ResourceLocation structureId, CommonConfig config) {
        List<CommonConfig.GlobPattern> excludePatterns = config.getIvpExcludePatterns();
        for (CommonConfig.GlobPattern excluded : excludePatterns) {
            if (excluded.matches(structureId)) {
                return false;
            }
        }

        List<CommonConfig.GlobPattern> includePatterns = config.getIvpIncludePatterns();
        for (CommonConfig.GlobPattern included : includePatterns) {
            if (included.matches(structureId)) {
                return true;
            }
        }

        if (!config.ivpAutoDetectModdedVillages.get() || structureId.getNamespace().equals("minecraft")) {
            return false;
        }

        String path = structureId.getPath().toLowerCase(java.util.Locale.ROOT);

        return path.contains("village") || path.contains("town") || path.contains("settlement") || path.contains("hamlet");
    }

    private static boolean isFlatEnough(Structure.GenerationContext context, ChunkGenerator generator, int centerX, int centerZ, CommonConfig config) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        int radius = config.ivpSampleRadius.get();
        int step = config.ivpSampleStep.get();
        int maxVariation = config.ivpMaxTerrainVariation.get();

        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int height = generator.getFirstOccupiedHeight(centerX + dx, centerZ + dz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

                min = Math.min(min, height);
                max = Math.max(max, height);

                if (max - min > maxVariation) {
                    return false;
                }
            }
        }

        int[] edge = {-radius, 0, radius};

        for (int dx : edge) {
            for (int dz : edge) {
                int height = generator.getFirstOccupiedHeight(centerX + dx, centerZ + dz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

                min = Math.min(min, height);
                max = Math.max(max, height);

                if (max - min > maxVariation) {
                    return false;
                }
            }
        }

        return true;
    }
}