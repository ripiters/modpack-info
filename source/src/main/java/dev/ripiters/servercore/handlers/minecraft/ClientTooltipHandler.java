package dev.ripiters.servercore.handlers.minecraft;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientTooltipHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (id != null && "copperagebackport".equals(id.getNamespace())) {
            List<Component> tooltips = event.getToolTip();

            tooltips.removeIf(component -> {
                String text = component.getString().toLowerCase().trim();
                return text.contains("copper age backport");
            });

            boolean alreadyHasMinecraft = tooltips.stream()
                    .anyMatch(c -> c.getString().trim().equalsIgnoreCase("minecraft"));

            if (!alreadyHasMinecraft) {
                tooltips.add(Component.literal("Minecraft").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (id != null && "copperagebackport".equals(id.getNamespace())) {
            List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

            elements.removeIf(either ->
                    either.left().map(text -> text.getString().toLowerCase().contains("copper age backport")).orElse(false)
            );
        }
    }
}