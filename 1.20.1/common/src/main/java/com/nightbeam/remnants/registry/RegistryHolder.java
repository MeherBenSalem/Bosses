package com.nightbeam.remnants.registry;

import com.nightbeam.remnants.Constants;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

public final class RegistryHolder<T> {
	private final String namespace;
	private final String path;
	private T value;

	public RegistryHolder(String path) {
		this(Constants.MOD_ID, path);
	}

	public RegistryHolder(String path, Supplier<T> factory) {
		this(Constants.MOD_ID, path);
		this.value = factory.get();
	}

	private RegistryHolder(String namespace, String path) {
		this.namespace = namespace;
		this.path = path;
	}

	public static <T> RegistryHolder<T> entity(String path) {
		return new RegistryHolder<>(Constants.ENTITY_NAMESPACE, path);
	}

	public ResourceLocation id() {
		return new ResourceLocation(namespace, path);
	}

	public String namespace() {
		return namespace;
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
