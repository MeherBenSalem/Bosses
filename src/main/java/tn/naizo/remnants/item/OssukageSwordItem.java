
package tn.naizo.remnants.item;

import tn.naizo.remnants.procedures.OssukageSwordToolInHandTickProcedure;
import tn.naizo.remnants.procedures.OssukageSwordRightclickedProcedure;
import tn.naizo.remnants.procedures.OssukageSwordLivingEntityIsHitWithToolProcedure;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

import java.util.List;

public class OssukageSwordItem extends SwordItem {
	public OssukageSwordItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 15f;
			}

			public float getAttackDamageBonus() {
				return 8f;
			}

			public int getLevel() {
				return 0;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2f, new Item.Properties().fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		OssukageSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		OssukageSwordRightclickedProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
		return ar;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, list, flag);
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_0"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_1"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_2"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_3"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_4"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_5"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_6"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_7"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_8"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_9"));
		list.add(Component.translatable("item.remnant_bosses.ossukage_sword.description_10"));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			OssukageSwordToolInHandTickProcedure.execute(entity);
	}
}
