package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.RitualInfo;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.Arrays;
import java.util.List;

/** Real registry fixtures catch invalid config falling through to the default air entry. */
public final class RitualRegression {
    public static void main(String[] args) { run(); }

    public static void run() {
        RitualValueRegression.run();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        List<String> offerings = List.of("minecraft:amethyst_block", "minecraft:crying_obsidian",
                "minecraft:end_stone", "minecraft:sculk");
        RitualInfo valid = RitualInfo.fromValues("minecraft:echo_shard", offerings);
        require(valid.valid(), "Effective vanilla ritual must remain valid");
        require(valid.offerings().get(0).equals("minecraft:amethyst_block"), "East offering order changed");
        require(valid.offerings().get(3).equals("minecraft:sculk"), "North offering order changed");
        require(!RitualInfo.fromValues("unknown:activation", offerings).valid(), "Unknown item became air");
        require(!RitualInfo.fromValues("bad::id", offerings).valid(), "Malformed item accepted");
        require(!RitualInfo.fromValues("echo_shard", offerings).valid(), "Unqualified activation cannot match gameplay");
        require(!RitualInfo.fromValues(null, offerings).valid(), "Missing activation accepted");
        require(!RitualInfo.fromValues("minecraft:echo_shard", List.of("unknown:block", "minecraft:stone",
                "minecraft:stone", "minecraft:stone")).valid(), "Unknown block became air");
        require(!RitualInfo.fromValues("minecraft:echo_shard", Arrays.asList(null, "minecraft:stone",
                "minecraft:stone", "minecraft:stone")).valid(), "Null offering accepted");
        require(!RitualInfo.fromValues("minecraft:echo_shard", List.of("minecraft:air", "minecraft:stone",
                "minecraft:stone", "minecraft:stone")).valid(), "Empty pedestal cannot be an offering");
        require(!RitualInfo.fromValues("x".repeat(257), offerings).valid(), "Oversized config accepted");
        System.out.println("RitualRegression: configured IDs, missing/malformed/unknown values and direction order pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
