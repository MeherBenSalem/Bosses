import path from "node:path";

export const MODRINTH_ID = "coZxDAGe";
export const CURSEFORGE_ID = "1225026";
export const MOD_TITLE = "Remnant Bosses";
export const LOADER_IDS = Object.freeze({ fabric: 7499, forge: 7498, neoforge: 10150 });

// Runtime requirements in each loader's fabric.mod.json / mods.toml.
// Project IDs verified against https://api.modrinth.com/v2/project/{id}.
export const DEPENDENCY_PROJECTS = Object.freeze({
  geckolib: "8BmcQJ2H",
  jauml: "ihvBalM2",
  "fabric-api": "P7dR8mSH",
});

const SUPPORTED_TARGETS = new Set([
  "fabric:1.20.1", "forge:1.20.1", "fabric:1.21.1", "neoforge:1.21.1",
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

export function modrinthDependencies(loader, game) {
  if (!SUPPORTED_TARGETS.has(`${loader}:${game}`)) {
    throw new Error(`Unsupported release target ${loader} ${game}`);
  }
  const mods = ["geckolib", "jauml"];
  if (loader === "fabric") mods.push("fabric-api");
  return mods.map((mod) => ({
    project_id: DEPENDENCY_PROJECTS[mod],
    version_id: null,
    file_name: null,
    dependency_type: "required",
  }));
}

export function parseJar(jar, expectedVersion) {
  const name = path.basename(jar);
  const match = name.match(
    /^remnant_bosses-(fabric|forge|neoforge)-(\d+\.\d+\.\d+)-(\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?)\.jar$/,
  );
  if (!match) throw new Error(`Cannot parse release jar ${name}`);
  const [, loader, game, version] = match;
  modrinthDependencies(loader, game); // Fail closed instead of guessing a target.
  if (expectedVersion && version !== normalizeVersion(expectedVersion)) {
    throw new Error(`Jar version ${version} does not match ${expectedVersion}: ${name}`);
  }
  return { jar, name, loader, game, version };
}

export function modrinthMetadata(release, { version, changelog, projectId = MODRINTH_ID }) {
  version = normalizeVersion(version);
  if (release.version !== version) throw new Error(`Jar version does not match ${version}`);
  return {
    name: `${version} · ${release.loader} · ${release.game}`,
    version_number: `${version}+${release.loader}-${release.game}`,
    changelog,
    dependencies: modrinthDependencies(release.loader, release.game),
    game_versions: [release.game],
    version_type: "release",
    loaders: [release.loader],
    featured: false,
    status: "listed",
    project_id: projectId,
    file_parts: ["file_0"],
    primary_file: "file_0",
  };
}
