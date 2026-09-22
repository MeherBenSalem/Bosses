package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.RemnantBossBars;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge's native hook also works in source-set launches without jar manifests. */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeBossBarEvents {
    private ForgeBossBarEvents() {}

    @SubscribeEvent
    public static void renderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!RemnantBossBars.draw(event.getGuiGraphics(), event.getX(), event.getY(), event.getBossEvent())) return;
        var font = Minecraft.getInstance().font;
        var name = event.getBossEvent().getName();
        event.getGuiGraphics().drawString(font, name, event.getX() + 91 - font.width(name) / 2, event.getY() - 9, 0xFFFFFF);
        event.setIncrement(36);
        event.setCanceled(true);
    }
}
