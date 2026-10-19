package com.nightbeam.remnants.registry;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

public final class RegistryHolder<T> {
	private final String namespace;
	private final String path;
	private final Supplier<T> factory;
	private T value;
	private Holder<T> holder;

	public RegistryHolder(String path) {
		this(Constants.MOD_ID, path, null);
	}

	public RegistryHolder(String path, Supplier<T> factory) {
		this(Constants.MOD_ID, path, factory);
	}

	private RegistryHolder(String namespace, String path, Supplier<T> factory) {
		this.namespace = namespace;
		this.path = path;
		this.factory = factory;
	}

	public static <T> RegistryHolder<T> entity(String path) {
		return new RegistryHolder<>(Constants.ENTITY_NAMESPACE, path, null);
	}

	public static <T> RegistryHolder<T> entity(String path, Supplier<T> factory) {
		return new RegistryHolder<>(Constants.ENTITY_NAMESPACE, path, factory);
	}

	public ResourceLocation id() {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}

	public String namespace() {
		return namespace;
	}

	public String path() {
		return path;
	}

	public T get() {
		if (value == null && holder != null && holder.isBound()) {
			value = holder.value();
		}
		if (value == null && factory != null) {
			value = factory.get();
		}
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

	/**
	 * Registries whose consumers take a {@link Holder} (1.21 armor materials) bind the registry's own
	 * holder here: Fabric's {@code Registry.registerForHolder} result or a NeoForge {@code DeferredHolder},
	 * which may still be unbound when items are constructed.
	 */
	public void bindHolder(Holder<T> holder) {
		this.holder = holder;
	}

	public Holder<T> holder() {
		return Objects.requireNonNull(holder, () -> "No registry holder bound for " + id());
	}
}
