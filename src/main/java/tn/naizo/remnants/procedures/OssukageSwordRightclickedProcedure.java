package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.RemnantBossesModEntities;
import tn.naizo.remnants.entity.KunaiEntity;
import tn.naizo.remnants.configuration.MainConfigConfiguration;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class OssukageSwordRightclickedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
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
				}.getArrow(projectileLevel, (float) (double) MainConfigConfiguration.SHURIKEN_DAMAGE.get(), (int) (double) MainConfigConfiguration.SHURIKEN_KNOCKBACK.get(), (byte) ((double) MainConfigConfiguration.SHURIKEN_PIERCE.get()));
				_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
				_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 2, 0);
				projectileLevel.addFreshEntity(_entityToSpawn);
			}
		}
		if (entity instanceof Player _player)
			_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), (int) (double) MainConfigConfiguration.SHURIKEN_TIMER.get());
	}
}
