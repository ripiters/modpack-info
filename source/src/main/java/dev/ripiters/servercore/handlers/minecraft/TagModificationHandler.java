package dev.ripiters.servercore.handlers.minecraft;

import dev.ripiters.servercore.ServerCore;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagModificationHandler {

    private static final TagKey<Item> FOODS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "foods")
    );

    private static final TagKey<Item> DOUGH_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "foods/dough")
    );

    private static final TagKey<Item> DOUGH_WHEAT_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "foods/dough/wheat")
    );

    private static final TagKey<Item> WHEAT_DOUGH_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "wheat_dough")
    );

    private static final TagKey<Item> MC_EGGS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "eggs")
    );

    private static final TagKey<Item> C_EGGS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "eggs")
    );

    private static final ResourceKey<Item> CREATE_DOUGH_KEY = ResourceKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create", "dough")
    );

    private static final ResourceKey<Item> BLUE_EGG_KEY = ResourceKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "blue_egg")
    );

    private static final ResourceKey<Item> BROWN_EGG_KEY = ResourceKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "brown_egg")
    );

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            Optional<Registry<Item>> registryOpt = event.getRegistryAccess().registry(Registries.ITEM);

            registryOpt.ifPresent(registry -> {
                // Dough tag removals
                removeEntryFromTag(registry, FOODS_TAG, CREATE_DOUGH_KEY);
                removeEntryFromTag(registry, DOUGH_TAG, CREATE_DOUGH_KEY);
                removeEntryFromTag(registry, DOUGH_WHEAT_TAG, CREATE_DOUGH_KEY);
                removeEntryFromTag(registry, WHEAT_DOUGH_TAG, CREATE_DOUGH_KEY);

                // Eggs tag removals
                removeEntryFromTag(registry, MC_EGGS_TAG, BLUE_EGG_KEY);
                removeEntryFromTag(registry, MC_EGGS_TAG, BROWN_EGG_KEY);
                removeEntryFromTag(registry, C_EGGS_TAG, BLUE_EGG_KEY);
                removeEntryFromTag(registry, C_EGGS_TAG, BROWN_EGG_KEY);
            });
        }
    }

    private static void removeEntryFromTag(Registry<Item> registry, TagKey<Item> tagKey, ResourceKey<Item> itemToRemove) {
        registry.getTag(tagKey).ifPresent(namedTag -> {
            try {
                Field contentsField = namedTag.getClass().getDeclaredField("contents");
                contentsField.setAccessible(true);

                @SuppressWarnings("unchecked")
                List<Holder<Item>> currentHolders = (List<Holder<Item>>) contentsField.get(namedTag);

                List<Holder<Item>> modifiableList = new ArrayList<>(currentHolders);
                boolean removed = modifiableList.removeIf(holder -> holder.is(itemToRemove));

                if (removed) {
                    contentsField.set(namedTag, List.copyOf(modifiableList));
                    ServerCore.LOGGER.info("Successfully removed {} from {} tag.", itemToRemove.location(), tagKey.location());
                }
            } catch (Exception e) {
                ServerCore.LOGGER.error("Failed to modify tag {}", tagKey.location(), e);
            }
        });
    }
}