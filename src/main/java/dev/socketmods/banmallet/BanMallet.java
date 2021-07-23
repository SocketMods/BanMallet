package dev.socketmods.banmallet;

import com.mojang.brigadier.CommandDispatcher;
import dev.socketmods.banmallet.commands.OpQueryCommand;
import dev.socketmods.banmallet.commands.arguments.DurationArgumentType;
import dev.socketmods.banmallet.commands.replacements.BHBanCommand;
import dev.socketmods.banmallet.commands.replacements.BHDeOpCommand;
import dev.socketmods.banmallet.commands.replacements.BHOpCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dev.socketmods.banmallet.CommandHelper.replaceAndRegister;

@Mod(BanMallet.MODID)
public class BanMallet {
    public static final String MODID = "banmallet";
    public static final Logger LOGGER = LogManager.getLogger(BanMallet.class);

    public static BanMalletConfig CONFIG;
    static ForgeConfigSpec CONFIG_SPEC;

    public BanMallet() {
        final Pair<BanMalletConfig, ForgeConfigSpec> config = new ForgeConfigSpec.Builder()
                .configure(BanMalletConfig::new);
        CONFIG = config.getLeft();
        CONFIG_SPEC = config.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);

        CommandHelper.init();
        ArgumentTypes.register(MODID + ":time_duration", DurationArgumentType.class,
                new ArgumentSerializer<>(DurationArgumentType::duration));

        // LOW priority means that anyone who edits the commands before us is forcefully removed
        // Possibly consider HIGH instead?
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false,
                RegisterCommandsEvent.class, this::onRegisterCommands);
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (CONFIG.forceDisable.get()) {
            LOGGER.warn("BanMallet is force-disabled, not hooking in");
            return;
        }
        final MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        if (currentServer != null && !currentServer.isDedicatedServer() && !CONFIG.integratedServer.get()) {
            LOGGER.info("On integrated server and not configured to be enabled here, not hooking in");
            return;
        }

        final CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // includeDedicated == true
        if (event.getEnvironment() != Commands.EnvironmentType.INTEGRATED) {
            replaceAndRegister(dispatcher, BHOpCommand.getNode());
            replaceAndRegister(dispatcher, BHDeOpCommand.getNode());
            OpQueryCommand.register(dispatcher);

            replaceAndRegister(dispatcher, BHBanCommand.getNode());
        }
    }
}
