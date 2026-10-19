package com.nightbeam.remnants;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.remnants.entity.anim.RemnantClips;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Animation contract: every clip the Java entities play is listed in {@link RemnantClips}, and each
 * one must exist in its {@code .animation.json} with the same length and loop mode. Entity sources
 * may not name clips outside the table, and each entity needs its geo model, texture and glowmask.
 */
public final class ClipContractRegression {
    private static final Pattern LITERAL_CLIP = Pattern.compile("\"animation\\.[a-z_]+\\.[a-z_]+\"");
    /** LegionEntity's controller plays movementClip (walk/idle), hurt and death for every subclass. */
    private static final Pattern LEGION_BODY = Pattern.compile("extends (LegionEntity|CryptbornEntity|ForgottenSoulEntity|FloatingSoulEntity)\\b");
    private static final Pattern SHEET_OF = Pattern.compile("return RemnantClips\\.([A-Z_]+);");
    private static final List<String> TABLE_ONLY = List.of("HollowSovereignEntity", "SovereignPauldronEntity",
            "SpectralCrescentEntity", "LegionEntity", "HollowSentinelEntity", "SpectralRoyalGuardEntity",
            "HollowArbalistEntity", "FallenStandardBearerEntity", "SoulboundKnightEntity",
            "ForgottenWatcherEntity", "WatcherIllusionEntity", "MemoryBoltEntity", "LostPilgrimEntity", "MemoryWraithEntity",
            "SoulCollectorEntity", "SoulWispEntity");

    public static void main(String[] args) throws Exception {
        Path resources = Path.of(args[0]);
        Path sources = Path.of(args[1]);
        Path assets = resources.resolve("assets/remnant_bosses");
        List<String> failures = new ArrayList<>();
        int clips = 0;
        for (RemnantClips.Sheet sheet : RemnantClips.ALL) {
            Path file = assets.resolve("animations/entity/" + sheet.model() + ".animation.json");
            if (!Files.isRegularFile(file)) {
                failures.add("missing " + file);
                continue;
            }
            JsonObject animations = JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("animations");
            for (RemnantClips.Clip clip : sheet.all()) {
                clips++;
                JsonObject json = animations.getAsJsonObject(clip.id());
                if (json == null) {
                    failures.add(clip.id() + " is played by Java but missing from " + file.getFileName());
                    continue;
                }
                double seconds = json.has("animation_length") ? json.get("animation_length").getAsDouble() : -1;
                long ticks = Math.round(seconds * 20);
                if (Math.abs(seconds * 20 - clip.ticks()) > 0.01 || ticks != clip.ticks()) {
                    failures.add(clip.id() + " is " + seconds + " s (" + ticks + " ticks); Java expects " + clip.ticks());
                }
                RemnantClips.Mode mode = mode(json.get("loop"));
                if (mode != clip.mode()) failures.add(clip.id() + " loop mode is " + mode + "; Java expects " + clip.mode());
            }
            for (String suffix : List.of("geo/entity/" + sheet.model() + ".geo.json", "textures/entities/" + sheet.model() + ".png",
                    "textures/entities/" + sheet.model() + "_glowmask.png")) {
                if (!Files.isRegularFile(assets.resolve(suffix))) failures.add("missing asset " + suffix);
            }
        }
        if (!Files.isRegularFile(assets.resolve("textures/entities/hollow_sentinel_spectral.png"))) {
            failures.add("missing spectral royal guard texture");
        }
        if (!Files.isRegularFile(assets.resolve("textures/entities/forgotten_watcher_illusion.png"))) {
            failures.add("missing watcher illusion texture");
        }
        // Java hides and turns the aegis, and hides plate aegis_0 (the open segment).
        Path watcherGeo = assets.resolve("geo/entity/forgotten_watcher.geo.json");
        if (Files.isRegularFile(watcherGeo)) {
            String geo = Files.readString(watcherGeo);
            for (String bone : List.of("aegis", "aegis_0", "aegis_1", "aegis_2", "aegis_3", "aegis_4", "aegis_5", "core", "eye")) {
                if (!geo.contains("\"name\": \"" + bone + "\"") && !geo.contains("\"name\":\"" + bone + "\"")) {
                    failures.add("forgotten_watcher.geo.json lacks the bone Java drives: " + bone);
                }
            }
        }
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                if (!TABLE_ONLY.contains(name)) continue;
                Matcher literal = LITERAL_CLIP.matcher(Files.readString(file));
                if (literal.find()) failures.add(name + " names " + literal.group() + " directly; use RemnantClips");
            }
        }
        // A Legion-style body that keeps the default movementClip plays walk while moving (the Soul
        // Collector floats and has no walk clip, so it overrides movementClip).
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                Matcher sheetName = SHEET_OF.matcher(text);
                if (!LEGION_BODY.matcher(text).find() || !sheetName.find()) continue;
                RemnantClips.Sheet sheet = (RemnantClips.Sheet) RemnantClips.class.getField(sheetName.group(1)).get(null);
                List<String> needed = new ArrayList<>(List.of("idle", "hurt", "death"));
                if (!text.contains("movementClip(")) needed.add("walk");
                for (String clip : needed) {
                    if (!sheet.has(clip)) failures.add(file.getFileName() + " plays " + sheet.model() + "." + clip
                            + " through the LegionEntity controller, but the sheet has no such clip");
                }
            }
        }
        if (!failures.isEmpty()) throw new AssertionError("Clip contract broken:\n  " + String.join("\n  ", failures));
        System.out.println("Clip contract passed: " + clips + " clips in " + RemnantClips.ALL.size() + " animation files");
    }

    private static RemnantClips.Mode mode(JsonElement loop) {
        if (loop == null || loop.isJsonNull()) return RemnantClips.Mode.ONCE;
        if (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()) {
            return loop.getAsBoolean() ? RemnantClips.Mode.LOOP : RemnantClips.Mode.ONCE;
        }
        String value = loop.getAsString();
        return switch (value) {
            case "true", "loop" -> RemnantClips.Mode.LOOP;
            case "hold_on_last_frame" -> RemnantClips.Mode.HOLD;
            default -> RemnantClips.Mode.ONCE;
        };
    }
}
