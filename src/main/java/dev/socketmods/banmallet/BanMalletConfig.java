package dev.socketmods.banmallet;

import net.minecraftforge.common.ForgeConfigSpec;

public class BanMalletConfig {
    public final ForgeConfigSpec.BooleanValue forceDisable;
    public final ForgeConfigSpec.BooleanValue integratedServer;

    public BanMalletConfig(ForgeConfigSpec.Builder builder) {
        forceDisable = builder.comment(
                "Force disable BanMallet",
                "This allows server administrators to temporary disable BanMallet, for debugging or otherwise."
        ).define("disable", false);

        integratedServer = builder.comment(
                "Force enable on integrated servers",
                "By default, BanMallet only operates on the dedicate server. Setting this to `true` overrides that, ",
                "and forces BanMallet to work on the integrated server as well."
        ).define("force_integrated", false);
    }
}
