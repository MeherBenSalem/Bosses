package tn.naizo.remnants.item;

import tn.naizo.remnants.init.ModItems;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

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

			public TagKey<Block> getIncorrectBlocksForDrops() {
				return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(ModItems.OLD_SKELETON_BONE.get()));
			}
		}, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 15f;
			}

			public float getAttackDamageBonus() {
				return 8f;
			}

			public TagKey<Block> getIncorrectBlocksForDrops() {
				return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(ModItems.OLD_SKELETON_BONE.get()));
			}
		}, 3, -2f)));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		// Procedure call removed - will be handled by event system
		return retval;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list,
			TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
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
}