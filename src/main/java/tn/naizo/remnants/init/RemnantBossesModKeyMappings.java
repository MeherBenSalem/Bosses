
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.network.ThrowShurikenKeyBindMessage;
import tn.naizo.remnants.RemnantBossesMod;

import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class RemnantBossesModKeyMappings {
	public static final KeyMapping THROW_SHURIKEN_KEY_BIND = new KeyMapping("key.remnant_bosses.throw_shuriken_key_bind", GLFW.GLFW_KEY_UNKNOWN, "key.categories.remnant") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				RemnantBossesMod.PACKET_HANDLER.sendToServer(new ThrowShurikenKeyBindMessage(0, 0));
				ThrowShurikenKeyBindMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(THROW_SHURIKEN_KEY_BIND);
	}

	@Mod.EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (Minecraft.getInstance().screen == null) {
				THROW_SHURIKEN_KEY_BIND.consumeClick();
			}
		}
	}
}
