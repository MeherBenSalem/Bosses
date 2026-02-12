package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.ModEntities;
import tn.naizo.remnants.entity.KunaiEntity;
import tn.naizo.remnants.config.JaumlConfigLib;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class ThrowKunaisProcedureProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		// Only force Mobs to look at their target
		if (entity instanceof Mob mob && mob.getTarget() != null) {
			Entity target = mob.getTarget();
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3(target.getX(), target.getY() + 1.5, target.getZ()));
		}

		Entity _shootFrom = entity;
		Level projectileLevel = _shootFrom.level();
		if (!projectileLevel.isClientSide()) {
			Projectile _entityToSpawn = new Object() {
				public Projectile getArrow(Level level, float damage) {
					AbstractArrow entityToSpawn = new KunaiEntity(ModEntities.KUNAI.get(), level);
					entityToSpawn.setBaseDamage(damage);
					entityToSpawn.setSilent(true);
					entityToSpawn.setCritArrow(true);
					return entityToSpawn;
				}
			}.getArrow(projectileLevel,
					(float) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_damage"));

			_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
			_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z,
					2, 0);
			projectileLevel.addFreshEntity(_entityToSpawn);
		}
	}
}
