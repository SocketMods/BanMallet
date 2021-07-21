package dev.socketmods.banmallet.datagen;

import dev.socketmods.banmallet.BanMallet;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class EnglishLanguageProvider extends LanguageProvider {
    public EnglishLanguageProvider(DataGenerator gen) {
        super(gen, BanMallet.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.add("commands.op.success", "Made %s a server operator with permission level %s");
        this.add("commands.banmallet.op.insufficient_permission",
                "Insufficient permission to promote users to server operator with permission level %s");

        this.add("commands.banmallet.deop.insufficient_permission",
                "Cannot make %s no longer a server operator due to insufficient permission");

        this.add("commands.banmallet.opquery.not_op", "%s is not a server operator");
        this.add("commands.banmallet.opquery.op", "%s is a server operator with permission level %s");
    }
}
