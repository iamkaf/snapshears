package com.example.examplemod;


import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static net.minecraft.world.InteractionResult.CONSUME;
import static net.minecraft.world.InteractionResult.SUCCESS;

@Mod(Constants.MOD_ID)
public class ExampleMod {

    public ExampleMod(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
        CommonClass.init();

        NeoForge.EVENT_BUS.register(EventHandlerCommon.class);
    }

    static class EventHandlerCommon {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void event(PlayerInteractEvent.EntityInteract event) {
            InteractionResult result = CommonClass.onPlayerEntityInteract(
                    event.getEntity(),
                    event.getLevel(),
                    event.getHand(),
                    event.getTarget()
            );

            var side = event.getSide();

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
            } else {
                if (result != InteractionResult.PASS) {
                    event.setCanceled(true);
                }
            }
        }
    }
}