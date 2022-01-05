package dev.socketmods.banmallet.network;

import dev.socketmods.banmallet.BanMallet;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Custom network filter hooks for BanMallet.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = BanMallet.MODID)
public class NetworkFilterHook {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            final ServerGamePacketListenerImpl netHandler = player.connection;
            final Connection manager = netHandler.connection;

            if (NetworkHooks.isVanillaConnection(manager)) {
                LOGGER.debug("Injecting custom network hooks into vanilla connection for {}", player);
                // Insert the custom arguments filter to work before Forge's vanilla connection filter
                injectFilter(manager, "forge:vanilla_filter", BanMallet.MODID + ":custom_arguments",
                        new CustomArgumentNetworkFilter());

                final MinecraftServer server = player.getServer();
                if (server != null) {
                    // Re-send command tree, with our filter now in place
                    server.getCommands().sendCommands(player);
                }
            }
        }
    }

    public static void injectFilter(Connection manager, String existingHandlerName, String name, ChannelHandler handler) {
        // Yes, we have to use addAfter so our new filter runs _before_ the existing one
        manager.channel().pipeline().addAfter(existingHandlerName, name, handler);
        LOGGER.debug("Injected handler {} after {} into {}", name, manager, existingHandlerName);
    }
}
