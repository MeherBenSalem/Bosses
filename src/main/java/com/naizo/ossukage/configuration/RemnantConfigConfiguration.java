package com.naizo.ossukage.configuration;

import net.minecraftforge.common.ForgeConfigSpec;

import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

public class RemnantConfigConfiguration {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<String> KEY;
	public static final ForgeConfigSpec.ConfigValue<Double> DASH_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> DASH_DISTANCE;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_DAMAGE;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_KNOCKBACK;
	public static final ForgeConfigSpec.ConfigValue<Double> SHURIKEN_PIERCE;
	public static final ForgeConfigSpec.ConfigValue<Double> SPEED_AMPLIFIER;
	public static final ForgeConfigSpec.ConfigValue<Double> LIFE_STEAL_POWER;
	public static final ForgeConfigSpec.ConfigValue<Double> MAX_HEALTH_PHASE_1;
	public static final ForgeConfigSpec.ConfigValue<Double> ATTACK_DAMAGE_PHASE_1;
	public static final ForgeConfigSpec.ConfigValue<Double> MOVEMENT_SPEED_PHASE_1;
	public static final ForgeConfigSpec.ConfigValue<Double> HP_THRESHOLD;
	public static final ForgeConfigSpec.ConfigValue<Double> TRANSFORM_DELAY;
	public static final ForgeConfigSpec.ConfigValue<Double> SKELETONS_ON_TRANSFORM;
	public static final ForgeConfigSpec.ConfigValue<Double> SKELETONS_ON_DASH;
	public static final ForgeConfigSpec.ConfigValue<Double> SPECIAL_ATTACK_CHANCE;
	public static final ForgeConfigSpec.ConfigValue<Double> INVISIBILITY_TIMER;
	public static final ForgeConfigSpec.ConfigValue<Double> ATTACK_DAMAGE_PHASE_2;
	public static final ForgeConfigSpec.ConfigValue<Double> MOVEMENT_SPEED_PHASE_2;
	static {
		BUILDER.push("General Combat Settings");
		KEY = BUILDER.comment("the item used to activate the portal").define("ancient_ruin_block_activation_key", BuiltInRegistries.ITEM.getKey(Items.NETHER_STAR).toString());
		DASH_TIMER = BUILDER.comment("he will use the dash ability every X amount of Ticks").define("dash_timer", (double) 100);
		DASH_DISTANCE = BUILDER.comment("the dash distance amplifier").define("dash_distance", (double) 2);
		SHURIKEN_TIMER = BUILDER.comment("he will throw a shuriken every X amount of Ticks").define("shuriken_timer", (double) 50);
		SHURIKEN_DAMAGE = BUILDER.comment("the damage of the ranged attacks").define("shuriken_damage", (double) 5);
		SHURIKEN_KNOCKBACK = BUILDER.comment("the knockback of the ranged attacks").define("shuriken_knockback", (double) 1);
		SHURIKEN_PIERCE = BUILDER.comment("the pierce damage of the ranged attacks").define("shuriken_pierce", (double) 0);
		SPEED_AMPLIFIER = BUILDER.comment("speed boost level when holding the sword").define("speed_amplifier", (double) 1);
		LIFE_STEAL_POWER = BUILDER.comment("the percentage of life steal you will get when killing a enemy").define("life_steal_power", (double) 20);
		BUILDER.pop();
		BUILDER.push("Phase 1 Settings");
		MAX_HEALTH_PHASE_1 = BUILDER.comment("the max health on second phase").define("max_health", (double) 800);
		ATTACK_DAMAGE_PHASE_1 = BUILDER.comment("the attack damage on second phase").define("attack_damage", (double) 7);
		MOVEMENT_SPEED_PHASE_1 = BUILDER.comment("the movement on second phase").define("movement_speed", (double) 0.25);
		BUILDER.pop();
		BUILDER.push("Phase 2 Settings");
		HP_THRESHOLD = BUILDER.comment("the percentage of the hp when he will transform [1-100]").define("hp_threshold", (double) 50);
		TRANSFORM_DELAY = BUILDER.comment("the delay before he transforms into second phase").define("transform_delay", (double) 40);
		SKELETONS_ON_TRANSFORM = BUILDER.comment("number of skeletons he will spawn when he transforms").define("skeletons_on_transform", (double) 5);
		SKELETONS_ON_DASH = BUILDER.comment("number of skeletons he will spawn when he dashes on second phase").define("skeletons_on_dash", (double) 2);
		SPECIAL_ATTACK_CHANCE = BUILDER.comment("the chance to apply a special attack each dash on second phase").define("special_attack_chance", (double) 20);
		INVISIBILITY_TIMER = BUILDER.comment("the number in ticks how much he will stay invisible when he transforms").define("invisibility_timer", (double) 200);
		ATTACK_DAMAGE_PHASE_2 = BUILDER.comment("the attack damage on second phase").define("attack_damage", (double) 9);
		MOVEMENT_SPEED_PHASE_2 = BUILDER.comment("the movement on second phase").define("movement_speed", (double) 0.3);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}
