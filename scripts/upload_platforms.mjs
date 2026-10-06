/**
 * Upload build/release-jars to Modrinth + CurseForge.
 * Tokens: environment or Desktop local.env (live uploads only)
 *
 *   node --test scripts/upload_platforms.test.mjs
 *   node scripts/upload_platforms.mjs --version 2.6.1 --jar-dir releases --dry-run
 * Omit --dry-run only when a new release upload is authorized.
 * This publisher does not repair metadata on existing releases.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { CURSEFORGE_ID, LOADER_IDS, MOD_TITLE, MODRINTH_ID,
  assertCompleteRelease, modrinthMetadata, normalizeVersion, parseJar } from "./release_metadata.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..");
const DIST = path.join(ROOT, "build", "release-jars");

async function loadEnv() {
  const candidates = [
    path.join(process.env.USERPROFILE || "", "NightBeam-Knowledge-Base", "secrets", "local.env"),
    path.join(process.env.USERPROFILE || "", "Desktop", "local.env"),
    "C:\\Users\\Meher\\Desktop\\local.env",
  ];
  for (const envPath of candidates) {
    if (!fs.existsSync(envPath)) continue;
    const text = fs.readFileSync(envPath, "utf8");
    for (const line of text.split(/\r?\n/)) {
      const m = line.match(/^([^#=]+)=(.*)$/);
      if (!m) continue;
      const k = m[1].trim();
      const v = m[2].trim();
      if (!process.env[k]) process.env[k] = v;
    }
    console.log("Loaded secrets from", envPath);
    return;
  }
}

export function parseArgs(argv) {
  const out = { curseforgeOnly: false, dryRun: false, jarDir: DIST };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (["--version", "--changelog-file", "--jar-dir"].includes(a)) {
      const value = argv[++i];
      if (!value || value.startsWith("--")) throw new Error(`Missing value for ${a}`);
      if (a === "--version") out.version = value;
      else if (a === "--changelog-file") out.changelogFile = value;
      else out.jarDir = path.resolve(value);
    } else if (a === "--curseforge-only") out.curseforgeOnly = true;
    else if (a === "--require-all-targets") out.requireAllTargets = true;
    else if (a === "--dry-run") out.dryRun = true;
    else throw new Error(`Unknown arg ${a}`);
  }
  out.version = normalizeVersion(out.version);
  return out;
}

export async function uploadPlatforms(args, {
  env = process.env, fetchImpl = globalThis.fetch, log = console.log, warn = console.warn,
} = {}) {
  const version = normalizeVersion(args.version);
  const dist = args.jarDir || DIST;
  const modrinthId = env.MODRINTH_ID || MODRINTH_ID;
  const curseforgeId = env.CURSEFORGE_ID || CURSEFORGE_ID;

  const changelog =
    args.changelogFile
      ? fs.readFileSync(args.changelogFile, "utf8")
      : `## ${MOD_TITLE} ${version}\n\nSee GitHub release notes.`;

  const jars = fs
    .readdirSync(dist)
    .filter((f) => f.startsWith("remnant_bosses-") && f.endsWith(`-${version}.jar`))
    .map((f) => path.join(dist, f))
    .sort();
  if (!jars.length) throw new Error(`No remnant_bosses-*-${version}.jar in ${dist}`);

  const parsed = jars.map((jar) => parseJar(jar, version));
  const targets = parsed.map((p) => `${p.loader}:${p.game}`);
  if (new Set(targets).size !== targets.length) throw new Error("Duplicate release target");
  if (args.requireAllTargets) assertCompleteRelease(parsed);
  for (const p of parsed) {
    if (!fs.statSync(p.jar).isFile()) throw new Error(`Not a regular jar file: ${p.name}`);
    fs.accessSync(p.jar, fs.constants.R_OK);
  }
  const payloads = parsed.map((p) => modrinthMetadata(p, {
    version, changelog, projectId: modrinthId,
  }));

  // A preview must not load secrets, require tokens, read jar bytes or call either API.
  if (args.dryRun) {
    for (let i = 0; i < parsed.length; i++) {
      const p = parsed[i];
      if (!args.curseforgeOnly) log("[dry-run] Modrinth", p.name, JSON.stringify(payloads[i]));
      log("[dry-run] CurseForge", p.name, JSON.stringify({
        project_id: curseforgeId,
        metadata: { changelog, changelogType: "markdown", displayName: p.name, releaseType: "release" },
        gameVersionLookup: { minecraft: p.game, environments: ["Client", "Server"], loaderId: LOADER_IDS[p.loader] },
      }));
    }
    return { releases: parsed.length, dryRun: true };
  }

  const { MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY } = env;
  if ((!args.curseforgeOnly && !MODRINTH_TOKEN) || !CURSEFORGE_TOKEN || !CURSEFORGE_API_KEY) {
    throw new Error(args.curseforgeOnly
      ? "Set CURSEFORGE_TOKEN, CURSEFORGE_API_KEY"
      : "Set MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY");
  }

  log(`Uploading ${MOD_TITLE} ${version} (${jars.length} jars) → Modrinth ${modrinthId}, CF ${curseforgeId}`);

  if (!args.curseforgeOnly) {
    for (const p of parsed) {
      const body = modrinthMetadata(p, { version, changelog, projectId: modrinthId });
      const form = new FormData();
      form.append("data", JSON.stringify(body));
      form.append("file_0", new Blob([fs.readFileSync(p.jar)]), p.name);
      const mrRes = await fetchImpl("https://api.modrinth.com/v2/version", {
        method: "POST",
        headers: { Authorization: MODRINTH_TOKEN },
        body: form,
      });
      const mrText = await mrRes.text();
      if (!mrRes.ok) throw new Error(`Modrinth ${mrRes.status} ${p.name} ${mrText.slice(0, 500)}`);
      log("Modrinth OK", p.name, body.version_number);
    }
  }

  const legacyRes = await fetchImpl("https://minecraft.curseforge.com/api/game/versions", {
    headers: { "X-Api-Token": CURSEFORGE_TOKEN },
  });
  if (!legacyRes.ok) throw new Error(`CurseForge legacy versions API ${legacyRes.status}`);
  const legacyFlat = await legacyRes.json();
  const findLegacy = (want, preferType) => {
    const hits = legacyFlat.filter((v) => v.name === want);
    if (preferType != null) {
      const hit = hits.find((v) => v.gameVersionTypeID === preferType);
      if (!hit) throw new Error(`No CF legacy version for ${want} (type ${preferType})`);
      return hit.id;
    }
    if (!hits[0]) throw new Error(`No CF legacy version for ${want}`);
    return hits[0].id;
  };
  const clientId = findLegacy("Client");
  const serverId = findLegacy("Server");

  const resolveGameId = async (game) => {
    const verRes = await fetchImpl(
      `https://api.curseforge.com/v1/minecraft/version/${encodeURIComponent(game)}`,
      { headers: { "x-api-key": CURSEFORGE_API_KEY, Accept: "application/json" } },
    );
    if (verRes.ok) {
      const verJson = await verRes.json();
      const gvId = verJson.data?.gameVersionId;
      if (gvId) {
        log("CF game", game, "-> gameVersionId", gvId);
        return gvId;
      }
    }
    const slug = game.replace(/\./g, "-");
    const candidates = legacyFlat.filter((v) => v.name === game || v.slug === slug);
    if (!candidates[0]) throw new Error(`No CF game version id for ${game}`);
    log("CF game", game, "-> legacy id", candidates[0].id);
    return candidates[0].id;
  };

  for (const p of parsed) {
    const gameId = await resolveGameId(p.game);
    const loaderId = LOADER_IDS[p.loader];
    if (!loaderId) throw new Error(`Unknown loader for ${p.name}`);
    const meta = {
      changelog,
      changelogType: "markdown",
      displayName: p.name,
      gameVersions: [clientId, serverId, loaderId, gameId],
      releaseType: "release",
    };
    const cfForm = new FormData();
    cfForm.append("metadata", JSON.stringify(meta));
    cfForm.append("file", new Blob([fs.readFileSync(p.jar)]), p.name);
    let cfText = "";
    for (let attempt = 1; attempt <= 4; attempt++) {
      const cfRes = await fetchImpl(
        `https://minecraft.curseforge.com/api/projects/${curseforgeId}/upload-file`,
        { method: "POST", headers: { "X-Api-Token": CURSEFORGE_TOKEN }, body: cfForm },
      );
      cfText = await cfRes.text();
      if (cfRes.ok) {
        log("CurseForge OK", p.name, cfText.slice(0, 120));
        break;
      }
      if ((cfRes.status === 503 || cfRes.status === 429) && attempt < 4) {
        warn("CurseForge", cfRes.status, "for", p.name, "- retry", attempt);
        await new Promise((r) => setTimeout(r, 15000 * attempt));
        continue;
      }
      throw new Error(`CurseForge ${cfRes.status} ${p.name} ${cfText.slice(0, 500)}`);
    }
  }
}

export async function main(argv = process.argv) {
  const args = parseArgs(argv);
  if (!args.dryRun) await loadEnv();
  return uploadPlatforms(args);
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  main().catch((e) => {
    console.error(e.message || e);
    process.exitCode = 1;
  });
}
