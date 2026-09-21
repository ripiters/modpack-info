package dev.ripiters.servercore.handlers.minecraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber
public class CreativeTabHandler {

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {

            Item badNugget = getItem("minecraft", "copper_nugget");
            if (badNugget != Items.AIR) {
                event.remove(
                        new ItemStack(badNugget),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                );
            }

            Item createNugget = getItem("create", "copper_nugget");
            if (createNugget != Items.AIR) {
                event.insertAfter(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(createNugget),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                );
            }
        }
    }

    private static Item getItem(String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.get(id);
        }
        return Items.AIR;
    }
}