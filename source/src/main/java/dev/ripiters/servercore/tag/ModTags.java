package dev.ripiters.servercore.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> UNBUNDLABLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("servercore", "unbundlable"));
    }
}