package com.lunaticelitemobs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point: adds the in-game config screen (Mods menu → Lunatic Elite Mobs → Config). */
@Mod(value = LunaticEliteMobs.MODID, dist = Dist.CLIENT)
public class LunaticEliteMobsClient {
    public LunaticEliteMobsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
