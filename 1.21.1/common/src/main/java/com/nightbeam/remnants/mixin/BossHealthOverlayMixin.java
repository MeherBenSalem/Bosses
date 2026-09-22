package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.client.RemnantBossBars;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Accessor("events") protected abstract Map<UUID,LerpingBossEvent> remnant$getEvents();
    @Invoker("drawBar") protected abstract void remnant$vanillaBar(GuiGraphics gui,int x,int y,BossEvent event);
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void remnant$render(GuiGraphics gui,CallbackInfo ci) {
        if(!RemnantBossBars.enabled() || remnant$getEvents().values().stream().noneMatch(e->RemnantBossBars.theme(e.getName())>=0)) return;
        Minecraft minecraft=Minecraft.getInstance();
        int y=12;
        for(LerpingBossEvent event:remnant$getEvents().values()) {
            int x=gui.guiWidth()/2-91;
            boolean custom=RemnantBossBars.draw(gui,x,y,event);
            if(!custom) remnant$vanillaBar(gui,x,y,event);
            // Long renamed titles are clipped by the normal GUI boundary, never used to identify a boss.
            gui.drawString(minecraft.font,event.getName(),gui.guiWidth()/2-minecraft.font.width(event.getName())/2,y-9,0xFFFFFF);
            y+=custom?36:19;
            if(y+22>=gui.guiHeight()/2) break;
        }
        ci.cancel();
    }
}
