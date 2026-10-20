package com.nightbeam.remnants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mixins must reach Minecraft members through the refmap. The Forge 1.20.1 build remaps injector targets,
 * accessors and invokers through the refmap, but leaves @Shadow and @Overwrite members under their Mojang
 * names, so a production Forge game fails with "@Shadow field ... was not located". Use @Accessor, @Invoker
 * or public methods instead.
 */
public final class MixinPolicyRegression {
    private static final Pattern FORBIDDEN = Pattern.compile("^\s*@(Shadow|Overwrite)\b", Pattern.MULTILINE);

    private MixinPolicyRegression() {
    }

    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (String root : args) {
            Path dir = Path.of(root);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    if (!source.contains("@Mixin")) continue;
                    checked++;
                    if (FORBIDDEN.matcher(source).find()) failures.add(dir.relativize(file).toString());
                }
            }
        }
        if (checked == 0) throw new IllegalStateException("no mixin sources found under " + String.join(", ", args));
        if (!failures.isEmpty()) {
            throw new IllegalStateException("@Shadow/@Overwrite in mixins break production Forge 1.20.1 jars; use @Accessor, "
                    + "@Invoker or public methods: " + failures);
        }
        System.out.println("Mixin policy passed: " + checked + " mixins use refmap-mapped members only");
    }
}
