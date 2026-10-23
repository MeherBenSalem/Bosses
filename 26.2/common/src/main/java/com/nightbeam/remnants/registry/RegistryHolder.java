package com.nightbeam.remnants.registry;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Supplier;

public final class RegistryHolder<T> {
	/** The entry whose factory is running, so item, block and entity type properties can carry their registry key. */
	private static final ThreadLocal<Identifier> BUILDING = new ThreadLocal<>();

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

	public Identifier id() {
		return Identifier.fromNamespaceAndPath(namespace, path);
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
			Identifier outer = BUILDING.get();
			BUILDING.set(id());
			try {
				value = factory.get();
			} finally {
				BUILDING.set(outer);
			}
		}
		return Objects.requireNonNull(value, () -> "Unbound registry entry: " + id());
	}

	/** The id of the entry being constructed by {@link #get()}; see {@link RegistryKeys}. */
	public static Identifier building() {
		return Objects.requireNonNull(BUILDING.get(), "No registry entry is being constructed");
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
