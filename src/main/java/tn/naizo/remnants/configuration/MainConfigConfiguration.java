package tn.naizo.remnants.configuration;

import net.minecraftforge.common.ForgeConfigSpec;

import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

public class MainConfigConfiguration {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<String> PORTAL_ACTIVATION_ITEM;
	public static final ForgeConfigSpec.ConfigValue<Double> DASH_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> DASH_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_DAMAGE;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_KNOCKBACK;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_PIERCE;
	public static final ForgeConfigSpec.ConfigValue<Double> SPEED_AMPLIFIER;
	public static final ForgeConfigSpec.ConfigValue<Double> LIFE_STEAL_POWER;
	public static final ForgeConfigSpec.ConfigValue<Double> MAX_HEALTH;
	public static final ForgeConfigSpec.ConfigValue<Double> ATTACK_DAMAGE_P1;
	public static final ForgeConfigSpec.ConfigValue<Double> MOVEMENT_SPEED_P1;
	public static final ForgeConfigSpec.ConfigValue<Double> HP_THRESHOLD;
	public static final ForgeConfigSpec.ConfigValue<Double> TRANSFORM_DELAY;
	public static final ForgeConfigSpec.ConfigValue<Double> SKELETONS_ON_TRANSFORM;
	public static final ForgeConfigSpec.ConfigValue<Double> SKELETONS_ON_DASH;
	public static final ForgeConfigSpec.ConfigValue<Double> SPECIAL_ATTACK_CHANCE;
	public static final ForgeConfigSpec.ConfigValue<Double> INVISIBILITY_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> ATTACK_DAMAGE_P2;
	public static final ForgeConfigSpec.ConfigValue<Double> MOVEMENT_SPEED_P2;
	static {
		BUILDER.push("General Combat Settings");
		PORTAL_ACTIVATION_ITEM = BUILDER.define("portal_activation_item", BuiltInRegistries.ITEM.getKey(Items.NETHER_STAR).toString());
		DASH_TIMER = BUILDER.define("dash_timer", (double) 100);
		DASH_DISTANCE = BUILDER.define("dash_distance", (double) 2);
		SHURIKEN_TIMER = BUILDER.define("shuriken_timer", (double) 50);
		SHURIKEN_DAMAGE = BUILDER.define("shuriken_damage", (double) 50);
		SHURIKEN_KNOCKBACK = BUILDER.define("shuriken_knockback", (double) 1);
		SHURIKEN_PIERCE = BUILDER.define("shuriken_pierce", (double) 0);
		SPEED_AMPLIFIER = BUILDER.define("speed_amplifier", (double) 1);
		LIFE_STEAL_POWER = BUILDER.define("life_steal_power", (double) 20);
		BUILDER.pop();
		BUILDER.push("Phase 1 Settings");
		MAX_HEALTH = BUILDER.define("max_health", (double) 800);
		ATTACK_DAMAGE_P1 = BUILDER.define("attack_damage", (double) 7);
		MOVEMENT_SPEED_P1 = BUILDER.define("movement_speed", (double) 0.25);
		BUILDER.pop();
		BUILDER.push("Phase 2 Settings");
		HP_THRESHOLD = BUILDER.define("hp_threshold", (double) 50);
		TRANSFORM_DELAY = BUILDER.define("transform_delay", (double) 40);
		SKELETONS_ON_TRANSFORM = BUILDER.define("skeletons_on_transform", (double) 5);
		SKELETONS_ON_DASH = BUILDER.define("skeletons_on_dash", (double) 2);
		SPECIAL_ATTACK_CHANCE = BUILDER.define("special_attack_chance", (double) 20);
		INVISIBILITY_TIMER = BUILDER.define("invisibility_timer", (double) 200);
		ATTACK_DAMAGE_P2 = BUILDER.define("attack_damage", (double) 9);
		MOVEMENT_SPEED_P2 = BUILDER.define("movement_speed", (double) 0.3);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}
