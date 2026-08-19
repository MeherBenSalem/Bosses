package com.nightbeam.remnants.forge.platform;

import com.nightbeam.remnants.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class ForgePlatformHelper implements IPlatformHelper {
	@Override
	public String getPlatformName() {
		return "Forge";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLEnvironment.production;
	}
}
