package com.example.modtemplate;

import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static net.minecraft.world.InteractionResult.*;

@Mod(Constants.MOD_ID)
public class TemplateNeoForge {
    public TemplateNeoForge(IEventBus eventBus) {
        TemplateMod.init();

        NeoForge.EVENT_BUS.register(EventHandlerCommon.class);
    }

    static class EventHandlerCommon {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void event(PlayerInteractEvent.EntityInteract event) {
            InteractionResult result = TemplateMod.onPlayerEntityInteract(
                    event.getEntity(),
                    event.getLevel(),
                    event.getHand(),
                    event.getTarget()
            );

            var side = event.getSide();

            if (result.equals(PASS)) {
                return;
            }

            // These checks make sure the event handling is equivalent to Fabric's.
            if (side.isClient()) {
                if (result == SUCCESS) {
                    event.setCancellationResult(SUCCESS);
                    event.setCanceled(true);
                } else if (result == CONSUME) {
                    event.setCancellationResult(CONSUME);
                    event.setCanceled(true);
                } else {
                    // If the result is FAIL or any other value, cancel the event
                    event.setCanceled(true);
                }
            }
        }
    }
}