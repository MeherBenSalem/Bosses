package com.nightbeam.remnants.registry;

import com.nightbeam.remnants.Constants;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

public final class RegistryHolder<T> {
	private final String path;
	private T value;

	public RegistryHolder(String path) {
		this.path = path;
	}

	public RegistryHolder(String path, Supplier<T> factory) {
		this.path = path;
		this.value = factory.get();
	}

	public ResourceLocation id() {
		return new ResourceLocation(Constants.MOD_ID, path);
	}

	public String path() {
		return path;
	}

	public T get() {
		return Objects.requireNonNull(value, () -> "Unbound registry entry: " + id());
	}

	public T getOrNull() {
		return value;
	}

	public void bind(T value) {
		this.value = value;
	}

	public boolean isBound() {
		return value != null;
	}
}
