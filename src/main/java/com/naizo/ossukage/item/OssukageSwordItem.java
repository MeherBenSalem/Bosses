
package com.naizo.ossukage.item;

import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;

import java.util.List;

import com.naizo.ossukage.procedures.OssukageSwordToolInHandTickProcedure;
import com.naizo.ossukage.procedures.OssukageSwordLivingEntityIsHitWithToolProcedure;
import com.naizo.ossukage.init.RemnantOssukageModItems;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class OssukageSwordItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 0;
		}

		@Override
		public float getSpeed() {
			return 15f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 0;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 2;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public OssukageSwordItem() {
		super(TOOL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(TOOL_TIER, 11f, -2f)).fireResistant());
	}

	@SubscribeEvent
	public static void handleToolDamage(ModifyDefaultComponentsEvent event) {
		event.modify(RemnantOssukageModItems.OSSUKAGE_SWORD.get(), builder -> builder.remove(DataComponents.MAX_DAMAGE));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		OssukageSwordLivingEntityIsHitWithToolProcedure.execute(entity);
		return retval;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_0"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_1"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_2"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_3"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_4"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_5"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_6"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_7"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_8"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_9"));
		list.add(Component.translatable("item.remnant_ossukage.ossukage_sword.description_10"));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			OssukageSwordToolInHandTickProcedure.execute(entity);
	}
}
