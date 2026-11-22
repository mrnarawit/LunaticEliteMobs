package com.lunaticelitemobs;

import com.lunaticelitemobs.config.EliteClientConfig;
import com.lunaticelitemobs.config.EliteConfig;
import com.lunaticelitemobs.elite.ModAttachments;
import com.lunaticelitemobs.network.EliteNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(LunaticEliteMobs.MODID)
public class LunaticEliteMobs {
    public static final String MODID = "lunaticelitemobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LunaticEliteMobs(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(EliteNetwork::register);
        modBus.addListener(EliteConfig::onLoad);
        modBus.addListener(EliteConfig::onReload);
        modBus.addListener(EliteClientConfig::onLoad);
        modBus.addListener(EliteClientConfig::onReload);

        container.registerConfig(ModConfig.Type.COMMON, EliteConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, EliteClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
