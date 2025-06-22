package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.RemnantBossesModEntities;
import tn.naizo.remnants.entity.OssukageEntity;
import tn.naizo.remnants.entity.KunaiEntity;
import tn.naizo.jauml.JaumlConfigLib;

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
		double particleRadius = 0;
		double particleAmount = 0;
		entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.5),
				((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
		{
			Entity _shootFrom = entity;
			Level projectileLevel = _shootFrom.level();
			if (!projectileLevel.isClientSide()) {
				Projectile _entityToSpawn = new Object() {
					public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
						AbstractArrow entityToSpawn = new KunaiEntity(RemnantBossesModEntities.KUNAI.get(), level);
						entityToSpawn.setBaseDamage(damage);
						entityToSpawn.setKnockback(knockback);
						entityToSpawn.setSilent(true);
						entityToSpawn.setPierceLevel(piercing);
						entityToSpawn.setCritArrow(true);
						return entityToSpawn;
					}
				}.getArrow(projectileLevel, (float) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_damage"), (int) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_knockback"),
						(byte) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_pierce"));
				_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
				_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 2, 0);
				projectileLevel.addFreshEntity(_entityToSpawn);
			}
		}
		if (entity instanceof OssukageEntity) {
			((OssukageEntity) entity).setAnimation("throw");
		}
	}
}
