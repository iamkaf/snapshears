package com.iamkaf.snapshears;

import com.iamkaf.amber.api.config.v1.JsonConfigManager;
import com.iamkaf.amber.api.event.v1.events.common.CommandEvents;
import com.iamkaf.amber.api.event.v1.events.common.PlayerEvents;
import com.iamkaf.amber.api.platform.v1.ModInfo;
import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.snapshears.config.ShearsConfig;
import com.iamkaf.snapshears.platform.Services;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import static net.minecraft.world.entity.LivingEntity.getSlotForHand;

/**
 * Common entry point for the SnapShears mod.
 * Replace the contents with your own implementation.
 */
public class SnapShearsMod {
    private static final JsonConfigManager<ShearsConfig> config =
            new JsonConfigManager<>(Constants.MOD_ID, new ShearsConfig(), null, ShearsConfig.HEADER_COMMENT);

    /**
     * Called during mod initialization for all loaders.
     */
    public static void init() {
        Constants.LOG.info("Initializing {} on {}...", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
        config.loadConfig();

        // Register the event handler for player interactions
        PlayerEvents.ENTITY_INTERACT.register(SnapShearsMod::onPlayerEntityInteract);
        CommandEvents.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {
            commandDispatcher.register(Commands.literal("snapshears").executes(commandContext -> {
                ModInfo modInfo = Platform.getModInfo(Constants.MOD_ID);

                // wat??
                if (modInfo == null) {
                    commandContext.getSource().sendFailure(Component.literal("SnapShears mod info not found!"));
                    return Command.SINGLE_SUCCESS;
                }

                commandContext.getSource()
                        .sendSuccess(
                                () -> Component.literal(modInfo.name()).append(" - Version: " + modInfo.version()),
                                false
                        );
                return Command.SINGLE_SUCCESS;
            }).then(Commands.literal("reload").executes(commandContext -> {
                config.loadConfig();
                commandContext.getSource().sendSuccess(() -> Component.literal("SnapShears config reloaded!"), true);
                return Command.SINGLE_SUCCESS;
            })));
        });
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
        boolean mainHandHasShears = isConfiguredShears(player.getMainHandItem());
        boolean itemInHandIsShears = isConfiguredShears(shears);
        if (!itemInHandIsShears) {
            return InteractionResult.PASS;
        }
        if (interactionHand.equals(InteractionHand.OFF_HAND) && mainHandHasShears) {
            return InteractionResult.PASS;
        }

        // Sheep check
        // Might want to do something different in the future in case we want to support modded sheep.
        if (!(entity instanceof Sheep interactedSheep)) {
            return InteractionResult.PASS;
        }

        // Get a bounding box around the sheep
        AABB box = interactedSheep.getBoundingBox().inflate(player.entityInteractionRange());

        if (interactedSheep.readyForShearing())
            player.crit(interactedSheep); // Apply critical hit effect on the original sheep

        for (Sheep sheep : player.level().getEntitiesOfClass(Sheep.class, box)) {
            if (sheep == interactedSheep) continue;
            // Check if the player can interact with the sheep
            if (player.canInteractWithEntity(sheep, player.entityInteractionRange()) && sheep.readyForShearing()) {
                // these calls were taken from Sheep.mobInteract()
                sheep.shear((ServerLevel) player.level(), SoundSource.PLAYERS, shears);
                sheep.gameEvent(GameEvent.SHEAR, player);
                shears.hurtAndBreak(1, player, getSlotForHand(interactionHand));
                player.crit(sheep); // Apply critical hit effect
            }
        }

        return InteractionResult.PASS;
    }

    private static boolean isConfiguredShears(ItemStack stack) {
        ShearsConfig cfg = config.getConfig();
        if (cfg == null) return false;
        if (stack.isEmpty()) return false;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (String entry : cfg.shears) {
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.parse(entry.substring(1));
                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                if (stack.is(tag)) return true;
            } else if (entry.equals(itemId.toString())) {
                return true;
            }
        }
        return false;
    }
}
