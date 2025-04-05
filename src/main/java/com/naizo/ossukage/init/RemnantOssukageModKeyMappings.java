
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import com.naizo.ossukage.network.ThrowShurikenKeyBindMessage;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class RemnantOssukageModKeyMappings {
	public static final KeyMapping THROW_SHURIKEN_KEY_BIND = new KeyMapping("key.remnant_ossukage.throw_shuriken_key_bind", GLFW.GLFW_KEY_UNKNOWN, "key.categories.remnant") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new ThrowShurikenKeyBindMessage(0, 0));
				ThrowShurikenKeyBindMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(THROW_SHURIKEN_KEY_BIND);
	}

	@EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				THROW_SHURIKEN_KEY_BIND.consumeClick();
			}
		}
	}
}
