package com.iamkaf.snapshears;

import com.iamkaf.amber.api.core.AmberMod;
import com.iamkaf.amber.api.event.v1.events.common.PlayerEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class ModCommon extends AmberMod {
    public ModCommon() {
        super(Reference.MOD_ID);
        Reference.LOGGER.info(Reference.INITIALIZING_MESSAGE);

        registerInteractionEvent();
    }

    private void registerInteractionEvent() {
        Reference.LOGGER.info("Registering player interaction event for SnapShears");
        PlayerEvents.ENTITY_INTERACT.register((player, level, interactionHand, entity) -> {
            if (level.isClientSide()) return InteractionResult.PASS;

            ItemStack shears = player.getItemInHand(interactionHand);
            boolean playerIsFalling = player.fallDistance > 0.0F;

            if (!playerIsFalling) {
                return InteractionResult.PASS;
            }

            // Makes sure the event is handled by only 1 shear, prioritizing the main hand.
            // In the case where both hands have shears, the main hand will handle the event.
            // In the case where the off-hand has shears, it will handle the event only if the main hand does not have
            // shears.
            // This allows the player to use shears in either hand, but only one hand will handle the event at a time.
            boolean mainHandHasShears = player.getMainHandItem().is(Items.SHEARS);
            boolean itemInHandIsShears = shears.is(Items.SHEARS);
            if (!itemInHandIsShears) {
                return InteractionResult.PASS;
            }
            if (interactionHand.equals(InteractionHand.OFF_HAND) && mainHandHasShears) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof Sheep interactedSheep)) {
                return InteractionResult.PASS;
            }

            // Get a bounding box around the sheep
            AABB box = interactedSheep.getBoundingBox().inflate(player.entityInteractionRange());

            for (Sheep sheep : player.level().getEntitiesOfClass(Sheep.class, box)) {
                // Check if the player can interact with the sheep
                if (player.canInteractWithEntity(sheep, player.entityInteractionRange())) {
                    sheep.mobInteract(player, interactionHand);
                    player.crit(sheep); // Apply critical hit effect
                }
            }

            return InteractionResult.PASS;
        });
    }
}
