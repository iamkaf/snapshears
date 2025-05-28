package com.iamkaf.template.registry;

import com.iamkaf.template.Reference;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class Items {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Reference.MOD_ID, Registries.ITEM);

    public static RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register(
            "example_item",
            () -> new Item(new Item.Properties().arch$tab(CreativeModeTabs.TEMPLATE).setId(key("example_item")))
    );

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Reference.resource(name));
    }

    public static void init() {
        ITEMS.register();
    }
}