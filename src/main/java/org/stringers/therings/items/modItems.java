package org.stringers.therings.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stringers.therings.Therings;


public class modItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Therings.MODID);

    public static final RegistryObject<Item> LUMERITE_INGOT = ITEMS.register("lumerite_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CONCENTRATED_LUMERITE_INGOT = ITEMS.register("concentrated_lumerite_ingot",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> RAW_LUMERITE = ITEMS.register("raw_lumerite",
            () -> new Item(new Item.Properties().stacksTo(64)));

}
