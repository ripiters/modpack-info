package dev.ripiters.servercore.handlers.minecraft;

import dev.ripiters.servercore.ServerCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;

public class RecipeRemoveHandler {

    private static final List<ResourceLocation> TARGET_RECIPES = List.of(
            ResourceLocation.fromNamespaceAndPath("createfood", "crafting/shaped/dumpling_wrappers_from_shaped"),
            ResourceLocation.fromNamespaceAndPath("createfood", "minecraft/crafting/dumpling_wrappers_from_crafting"),
            ResourceLocation.fromNamespaceAndPath("createfood", "create/splashing/dumpling_wrappers_from_splashing")
    );

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener((preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> preparationBarrier.wait(null).thenAcceptAsync(ignored -> {
            RecipeManager recipeManager = event.getServerResources().getRecipeManager();

            List<RecipeHolder<?>> recipes = new ArrayList<>(recipeManager.getRecipes());

            boolean removed = recipes.removeIf(holder -> {
                if (TARGET_RECIPES.contains(holder.id())) {
                    ServerCore.LOGGER.info("Successfully removed target recipe: {}", holder.id());
                    return true;
                }
                return false;
            });

            if (removed) {
                recipeManager.replaceRecipes(recipes);
            } else {
                ServerCore.LOGGER.warn("No target recipes were found to remove!");
            }
        }, gameExecutor));
    }
}