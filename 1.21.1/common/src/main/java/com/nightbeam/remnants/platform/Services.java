package com.nightbeam.remnants.platform;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import com.nightbeam.remnants.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {
	public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
	public static INetworkHelper NETWORK;

	private Services() {
	}

	public static <T> T load(Class<T> clazz) {
		final T loadedService = ServiceLoader.load(clazz)
				.findFirst()
				.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}
}
