package com.iamkaf.snapshears;

import com.iamkaf.amber.api.commands.v1.SimpleCommands;
import com.iamkaf.amber.api.core.v2.AmberInitializer;
import com.iamkaf.amber.api.event.v1.events.common.CommandEvents;
import com.iamkaf.amber.api.event.v1.events.common.PlayerEvents;
import com.iamkaf.snapshears.config.ShearsConfig;
import com.iamkaf.snapshears.platform.Services;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Common entry point for the SnapShears mod.
 * Replace the contents with your own implementation.
 */
public class SnapShearsMod {
    /**
     * Called during mod initialization for all loaders.
     */
    public static void init() {
        Constants.LOG.info("Initializing {} on {}...", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
        AmberInitializer.initialize(Constants.MOD_ID);

        ShearsConfig.init();

        // Register the event handler for player interactions
        PlayerEvents.ENTITY_INTERACT.register(SnapShearsMod::onPlayerEntityInteract);
        CommandEvents.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {
            commandDispatcher.register(SimpleCommands.createBaseCommand(Constants.MOD_ID)
                    .then(Commands.literal("reload").executes(commandContext -> {
                        ShearsConfig.HANDLE.reload();
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

        int radius = ShearsConfig.RADIUS.get();
        double radiusSquared = radius * radius;
        AABB box = interactedSheep.getBoundingBox().inflate(radius);

        if (interactedSheep.readyForShearing())
            player.crit(interactedSheep); // Apply critical hit effect on the original sheep

        for (Sheep sheep : player.level().getEntitiesOfClass(Sheep.class, box)) {
            if (sheep == interactedSheep) continue;
            if (ShearsConfig.ONLY_SAME_COLOR.get() && sheep.getColor() != interactedSheep.getColor()) {
                continue;
            }
            if (interactedSheep.distanceToSqr(sheep) <= radiusSquared && sheep.readyForShearing()) {
                // these calls were taken from Sheep.mobInteract()
                sheep.shear((ServerLevel) player.level(), SoundSource.PLAYERS, shears);
                dropBonusWool(sheep);
                sheep.gameEvent(GameEvent.SHEAR, player);
                EquipmentSlot slot = interactionHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                shears.hurtAndBreak(1, player, slot);
                player.crit(sheep); // Apply critical hit effect
            }
        }

        return InteractionResult.PASS;
    }

    private static boolean isConfiguredShears(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (String entry : ShearsConfig.SHEARS.get()) {
            if (entry.startsWith("#")) {
                Identifier tagId = Identifier.parse(entry.substring(1));
                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                if (stack.is(tag)) return true;
            } else if (entry.equals(itemId.toString())) {
                return true;
            }
        }
        return false;
    }

    private static void dropBonusWool(Sheep sheep) {
        int bonusWool = ShearsConfig.BONUS_WOOL.get();
        if (bonusWool <= 0) {
            return;
        }

        Item woolItem = switch (sheep.getColor()) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };

        sheep.spawnAtLocation((ServerLevel) sheep.level(), new ItemStack(woolItem, bonusWool), 1.0F);
    }
}
