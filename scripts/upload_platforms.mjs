/**
 * Upload build/release-jars to Modrinth + CurseForge.
 * Tokens: Desktop local.env (MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY)
 *
 *   node scripts/upload_platforms.mjs --version 2.5.1
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..");
const DIST = path.join(ROOT, "build", "release-jars");

const MODRINTH_ID = "coZxDAGe";
const CURSEFORGE_ID = "1225026";
const MOD_TITLE = "Remnant Bosses";

async function loadEnv() {
  const candidates = [
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

function parseArgs(argv) {
  const out = { curseforgeOnly: false, dryRun: false };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--version") out.version = argv[++i];
    else if (a === "--changelog-file") out.changelogFile = argv[++i];
    else if (a === "--curseforge-only") out.curseforgeOnly = true;
    else if (a === "--dry-run") out.dryRun = true;
    else throw new Error(`Unknown arg ${a}`);
  }
  if (!out.version) throw new Error("--version required");
  return out;
}

function parseJar(jar) {
  const n = path.basename(jar);
  let loader = "fabric";
  if (/neoforge/i.test(n)) loader = "neoforge";
  else if (/forge/i.test(n)) loader = "forge";
  else if (/fabric/i.test(n)) loader = "fabric";
  const m = n.match(
    /^remnant_bosses-(?:fabric|forge|neoforge)-(.+)-(\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?)\.jar$/i,
  );
  const game = m ? m[1] : null;
  return { jar, name: n, loader, game };
}

async function main() {
  await loadEnv();
  const args = parseArgs(process.argv);
  const version = args.version.replace(/^v/, "");

  const changelog =
    args.changelogFile && fs.existsSync(args.changelogFile)
      ? fs.readFileSync(args.changelogFile, "utf8")
      : `## ${MOD_TITLE} ${version}\n\nSee GitHub release notes.`;

  const jars = fs
    .readdirSync(DIST)
    .filter((f) => f.startsWith("remnant_bosses-") && f.endsWith(`-${version}.jar`))
    .map((f) => path.join(DIST, f))
    .sort();
  if (!jars.length) throw new Error(`No remnant_bosses-*-${version}.jar in ${DIST}`);

  const parsed = jars.map(parseJar);
  for (const p of parsed) {
    if (!p.game) throw new Error(`Cannot parse MC version from ${p.name}`);
  }

  const { MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY } = process.env;
  if (!MODRINTH_TOKEN || !CURSEFORGE_TOKEN || !CURSEFORGE_API_KEY) {
    throw new Error("Set MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY");
  }

  console.log(
    `Uploading ${MOD_TITLE} ${version} (${jars.length} jars) → Modrinth ${MODRINTH_ID}, CF ${CURSEFORGE_ID}`,
  );

  if (!args.curseforgeOnly) {
    for (const p of parsed) {
      const versionNumber = `${version}+${p.loader}-${p.game}`;
      const body = {
        name: `${version} · ${p.loader} · ${p.game}`,
        version_number: versionNumber,
        changelog,
        dependencies: [],
        game_versions: [p.game],
        version_type: "release",
        loaders: [p.loader],
        featured: false,
        status: "listed",
        project_id: MODRINTH_ID,
        file_parts: ["file_0"],
        primary_file: "file_0",
      };
      if (args.dryRun) {
        console.log("[dry-run] Modrinth", p.name, versionNumber);
        continue;
      }
      const form = new FormData();
      form.append("data", JSON.stringify(body));
      form.append("file_0", new Blob([fs.readFileSync(p.jar)]), p.name);
      const mrRes = await fetch("https://api.modrinth.com/v2/version", {
        method: "POST",
        headers: { Authorization: MODRINTH_TOKEN },
        body: form,
      });
      const mrText = await mrRes.text();
      if (!mrRes.ok) throw new Error(`Modrinth ${mrRes.status} ${p.name} ${mrText.slice(0, 500)}`);
      console.log("Modrinth OK", p.name, versionNumber);
    }
  }

  const LOADER_IDS = { fabric: 7499, forge: 7498, neoforge: 10150 };
  const legacyRes = await fetch("https://minecraft.curseforge.com/api/game/versions", {
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
    const verRes = await fetch(
      `https://api.curseforge.com/v1/minecraft/version/${encodeURIComponent(game)}`,
      { headers: { "x-api-key": CURSEFORGE_API_KEY, Accept: "application/json" } },
    );
    if (verRes.ok) {
      const verJson = await verRes.json();
      const gvId = verJson.data?.gameVersionId;
      if (gvId) {
        console.log("CF game", game, "-> gameVersionId", gvId);
        return gvId;
      }
    }
    const slug = game.replace(/\./g, "-");
    const candidates = legacyFlat.filter((v) => v.name === game || v.slug === slug);
    if (!candidates[0]) throw new Error(`No CF game version id for ${game}`);
    console.log("CF game", game, "-> legacy id", candidates[0].id);
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
    if (args.dryRun) {
      console.log("[dry-run] CurseForge", p.name, meta);
      continue;
    }
    const cfForm = new FormData();
    cfForm.append("metadata", JSON.stringify(meta));
    cfForm.append("file", new Blob([fs.readFileSync(p.jar)]), p.name);
    let cfText = "";
    for (let attempt = 1; attempt <= 4; attempt++) {
      const cfRes = await fetch(
        `https://minecraft.curseforge.com/api/projects/${CURSEFORGE_ID}/upload-file`,
        { method: "POST", headers: { "X-Api-Token": CURSEFORGE_TOKEN }, body: cfForm },
      );
      cfText = await cfRes.text();
      if (cfRes.ok) {
        console.log("CurseForge OK", p.name, cfText.slice(0, 120));
        break;
      }
      if ((cfRes.status === 503 || cfRes.status === 429) && attempt < 4) {
        console.warn("CurseForge", cfRes.status, "for", p.name, "- retry", attempt);
        await new Promise((r) => setTimeout(r, 15000 * attempt));
        continue;
      }
      throw new Error(`CurseForge ${cfRes.status} ${p.name} ${cfText.slice(0, 500)}`);
    }
  }
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
