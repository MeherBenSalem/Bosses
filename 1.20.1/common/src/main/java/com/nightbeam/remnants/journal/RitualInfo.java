package com.nightbeam.remnants.journal;

import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.config.WardenConfig;
import com.nightbeam.remnants.config.WatcherConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Server-effective ritual values; offerings are ordered east, west, south, north. */
public record RitualInfo(String activationItem, List<String> offerings, boolean valid) {
    public RitualInfo {
        offerings = List.copyOf(offerings);
    }

    public static RitualInfo resolve(BossHunt boss) {
        return resolve(boss.key() + "_summon");
    }

    public static RitualInfo resolve(String summonFile) {
        RitualInfo info = fromValues(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "portal_activation_item"),
                java.util.Arrays.asList(
                        JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_one_activation_block"),
                        JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_two_activation_block"),
                        JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_three_activation_block"),
                        JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_four_activation_block")));
        if (summonFile.equals("hollow_sovereign_summon") && SovereignConfig.value("summoning_enabled", 0, 1) == 0) {
            return new RitualInfo(info.activationItem, info.offerings, false);
        }
        if (summonFile.equals(WardenConfig.SUMMON) && WardenConfig.value("summoning_enabled", 0, 1) == 0) {
            return new RitualInfo(info.activationItem, info.offerings, false);
        }
        if (summonFile.equals(WatcherConfig.SUMMON) && WatcherConfig.value("summoning_enabled", 0, 1) == 0) {
            return new RitualInfo(info.activationItem, info.offerings, false);
        }
        return info;
    }

    public static RitualInfo fromValues(String activation, List<String> offerings) {
        if (offerings.size() != 4) throw new IllegalArgumentException("A ritual requires four offerings");
        String item = RitualValues.boundedId(activation);
        List<String> blocks = offerings.stream().map(RitualValues::boundedId).toList();
        ResourceLocation activationId = ResourceLocation.tryParse(item);
        boolean valid = activationId != null && activationId.toString().equals(item)
                && BuiltInRegistries.ITEM.containsKey(activationId)
                && BuiltInRegistries.ITEM.get(activationId) != Items.AIR;
        for (String block : blocks) {
            ResourceLocation id = ResourceLocation.tryParse(block);
            valid &= id != null && BuiltInRegistries.BLOCK.containsKey(id)
                    && BuiltInRegistries.BLOCK.get(id) != Blocks.AIR;
        }
        return new RitualInfo(item, blocks, valid);
    }

    public boolean matchesTops(Level level, BlockPos altar) {
        if (!valid) return false;
        BlockPos[] positions = {altar.offset(3, 1, 0), altar.offset(-3, 1, 0),
                altar.offset(0, 1, 3), altar.offset(0, 1, -3)};
        for (int i = 0; i < 4; i++) {
            if (!level.getBlockState(positions[i]).is(BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(offerings.get(i))))) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesActivation(ItemStack held) {
        return valid && BuiltInRegistries.ITEM.getKey(held.getItem()).toString().equals(activationItem);
    }
}
