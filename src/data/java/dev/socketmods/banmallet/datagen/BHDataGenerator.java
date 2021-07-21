package dev.socketmods.banmallet.datagen;

import dev.socketmods.banmallet.BanMallet;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = BanMallet.MODID)
public class BHDataGenerator {
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();

        if (event.includeClient()) {
            generator.addProvider(new EnglishLanguageProvider(generator));
        }
    }
}
