package tn.naizo.remnants.procedures;

import tn.naizo.remnants.entity.RatEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class RatOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double color = 0;
		color = Mth.nextInt(RandomSource.create(), 0, 4);
		if (color == 0) {
			if (entity instanceof RatEntity animatable)
				animatable.setTexture("rat_black");
		} else if (color == 1) {
			if (entity instanceof RatEntity animatable)
				animatable.setTexture("rat_brown");
		} else if (color == 2) {
			if (entity instanceof RatEntity animatable)
				animatable.setTexture("rat_grey");
		} else if (color == 3) {
			if (entity instanceof RatEntity animatable)
				animatable.setTexture("rat_red");
		} else {
			if (entity instanceof RatEntity animatable)
				animatable.setTexture("rat_white");
		}
	}
}
