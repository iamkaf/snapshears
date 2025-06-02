package com.example.examplemod;

import com.example.examplemod.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import static net.minecraft.world.entity.LivingEntity.getSlotForHand;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class CommonClass {

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

        Constants.LOG.info(
                "Hello from Common init on {}! we are currently in a {} environment!",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName()
        );
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.
        if (Services.PLATFORM.isModLoaded("examplemod")) {

            Constants.LOG.info("Hello to examplemod");
        }
    }

    public static InteractionResult onPlayerEntityInteract(Player player, Level level, InteractionHand interactionHand,
            Entity entity) {
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
                // these calls were taken from Sheep.mobInteract()
                sheep.shear((ServerLevel) player.level(), SoundSource.PLAYERS, shears);
                sheep.gameEvent(GameEvent.SHEAR, player);
                shears.hurtAndBreak(1, player, getSlotForHand(interactionHand));
                player.crit(sheep); // Apply critical hit effect
            }
        }

        return InteractionResult.PASS;
    }
}