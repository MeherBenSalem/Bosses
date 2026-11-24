import path from "node:path";

// NightBeam publishes to CurseForge only (since 24 Nov 2026); the existing Modrinth project stays up as it is but gets
// no new versions.
export const CURSEFORGE_ID = "1225026";
export const MOD_TITLE = "Remnant Bosses";
export const LOADER_IDS = Object.freeze({ fabric: 7499, forge: 7498, neoforge: 10150 });

// Runtime requirements in each loader's fabric.mod.json / mods.toml: mod id -> CurseForge slug.
export const DEPENDENCY_PROJECTS = Object.freeze({
  geckolib: "geckolib",
  jauml: "jauml",
  "fabric-api": "fabric-api",
});

const SUPPORTED_TARGETS = new Set([
  "fabric:1.20.1", "forge:1.20.1", "fabric:1.21.1", "neoforge:1.21.1",
  "fabric:26.2", "neoforge:26.2", "fabric:26.3", "neoforge:26.3",
]);

export function assertCompleteRelease(releases) {
  const targets = new Set(releases.map((p) => `${p.loader}:${p.game}`));
  const missing = [...SUPPORTED_TARGETS].filter((target) => !targets.has(target));
  if (missing.length) throw new Error(`Missing release targets: ${missing.join(", ")}`);
}

export function normalizeVersion(value) {
  const version = String(value ?? "").replace(/^v/, "");
  if (!/^\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?$/.test(version)) {
    throw new Error("--version must be a release version such as 2.6.1");
  }
  return version;
}

/** Required mod ids for a target (fails closed on an unsupported target). */
export function requiredMods(loader, game) {
  if (!SUPPORTED_TARGETS.has(`${loader}:${game}`)) {
    throw new Error(`Unsupported release target ${loader} ${game}`);
  }
  const mods = ["geckolib", "jauml"];
  if (loader === "fabric") mods.push("fabric-api");
  return mods;
}

export function curseforgeRelations(loader, game) {
  const mods = requiredMods(loader, game);
  return { projects: Object.entries(DEPENDENCY_PROJECTS)
    .filter(([mod]) => mods.includes(mod))
    .map(([, slug]) => ({ slug, type: "requiredDependency" })) };
}

export function parseJar(jar, expectedVersion) {
  const name = path.basename(jar);
  const match = name.match(
    /^remnant_bosses-(fabric|forge|neoforge)-(\d+\.\d+(?:\.\d+)?)-(\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?)\.jar$/,
  );
  if (!match) throw new Error(`Cannot parse release jar ${name}`);
  const [, loader, game, version] = match;
  requiredMods(loader, game); // Fail closed instead of guessing a target.
  if (expectedVersion && version !== normalizeVersion(expectedVersion)) {
    throw new Error(`Jar version ${version} does not match ${expectedVersion}: ${name}`);
  }
  return { jar, name, loader, game, version };
}
