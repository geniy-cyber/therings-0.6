package org.stringers.therings.tab;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.stringers.therings.Therings;
import org.stringers.therings.items.modItems;
import org.stringers.therings.blocks.modBlocks;

public class creativeRingsTab extends CreativeModeTab {

    protected creativeRingsTab(Builder builder) {
        super(builder);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Therings.MODID);

    public static final RegistryObject<CreativeModeTab> TOKYO_TAB = CREATIVE_MODE_TABS.register("rings_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(modItems.LUMERITE_INGOT.get()))
                    .title(Component.translatable("creative_tab.rings"))
                    .displayItems((pParameters, pOutput) -> {
                        //Items
                        pOutput.accept(modBlocks.LUMERITE_ORE.get());
                        pOutput.accept(modItems.LUMERITE_INGOT.get());
                        pOutput.accept(modItems.CONCENTRATED_LUMERITE_INGOT.get());
                        pOutput.accept( modItems.RAW_LUMERITE.get());

                    })
                    .build());
}
