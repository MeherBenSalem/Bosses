package com.nightbeam.remnants.neoforge.platform;

import com.nightbeam.remnants.platform.services.IPlatformHelper;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {
	@Override
	public String getPlatformName() {
		return "NeoForge";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLEnvironment.production;
	}
}
