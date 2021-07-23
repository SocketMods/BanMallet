package dev.socketmods.banmallet.network;

import dev.socketmods.banmallet.BanMallet;
import io.netty.channel.ChannelHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkHooks;
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
        final PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            final ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            final ServerPlayNetHandler netHandler = serverPlayer.connection;
            final NetworkManager manager = netHandler.connection;

            if (NetworkHooks.isVanillaConnection(manager)) {
                LOGGER.debug("Injecting custom network hooks into vanilla connection for {}", serverPlayer);
                // Insert the custom arguments filter to work before Forge's vanilla connection filter
                injectFilter(manager, "forge:vanilla_filter", BanMallet.MODID + ":custom_arguments",
                        new CustomArgumentNetworkFilter());

                final MinecraftServer server = serverPlayer.getServer();
                if (server != null) {
                    // Re-send command tree, with our filter now in place
                    server.getCommands().sendCommands(serverPlayer);
                }
            }
        }
    }

    public static void injectFilter(NetworkManager manager, String existingHandlerName, String name, ChannelHandler handler) {
        // Yes, we have to use addAfter so our new filter runs _before_ the existing one
        manager.channel().pipeline().addAfter(existingHandlerName, name, handler);
        LOGGER.debug("Injected handler {} after {} into {}", name, manager, existingHandlerName);
    }
}
