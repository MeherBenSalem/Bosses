import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";
import test from "node:test";
import { DEPENDENCY_PROJECTS, LOADER_IDS, curseforgeRelations, requiredMods,
  parseJar } from "./release_metadata.mjs";
import { parseArgs, uploadPlatforms } from "./upload_platforms.mjs";

const ROOT = fileURLToPath(new URL("../", import.meta.url));
const TARGETS = [
  ["fabric", "1.20.1"], ["forge", "1.20.1"],
  ["fabric", "1.21.1"], ["neoforge", "1.21.1"],
  ["fabric", "26.2"], ["neoforge", "26.2"],
  ["fabric", "26.3"], ["neoforge", "26.3"],
];
// Mock CurseForge game version ids.
const GAME_IDS = { "1.20.1": 1201, "1.21.1": 1211, "26.2": 2602, "26.3": 2603 };
const jarName = (loader, game) => `remnant_bosses-${loader}-${game}-2.6.1.jar`;
const silent = () => {};

function fixture(t) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "remnant-publisher-"));
  t.after(() => fs.rmSync(dir, { recursive: true, force: true }));
  for (const [loader, game] of TARGETS) fs.writeFileSync(path.join(dir, jarName(loader, game)), "fixture jar");
  return dir;
}

function sourceDependencies(loader, game) {
  if (loader === "fabric") {
    const source = JSON.parse(fs.readFileSync(path.join(ROOT, game, loader, "src/main/resources/fabric.mod.json")));
    return Object.keys(source.depends).filter((mod) => DEPENDENCY_PROJECTS[mod]);
  }
  const file = loader === "forge" ? "mods.toml" : "neoforge.mods.toml";
  const source = fs.readFileSync(path.join(ROOT, game, loader, "src/main/resources/META-INF", file), "utf8");
  return source.split(/\[\[dependencies\./).slice(1).flatMap((block) => {
    const mod = block.match(/modId\s*=\s*"([^"]+)"/)?.[1];
    const required = /mandatory\s*=\s*true|type\s*=\s*"required"/.test(block);
    return required && DEPENDENCY_PROJECTS[mod] ? [mod] : [];
  });
}

test("every target's CurseForge relations match the loader runtime requirements", () => {
  for (const [loader, game] of TARGETS) {
    const release = parseJar(jarName(loader, game), "2.6.1");
    assert.equal(release.loader, loader);
    assert.equal(release.game, game);
    const relations = curseforgeRelations(loader, game);
    const expected = sourceDependencies(loader, game).map((mod) => DEPENDENCY_PROJECTS[mod]).sort();
    assert.deepEqual(relations.projects.map((p) => p.slug).sort(), expected);
    assert.equal(relations.projects.length, loader === "fabric" ? 3 : 2);
    assert.ok(relations.projects.every((p) => p.type === "requiredDependency"));
  }
});

test("invalid targets, mismatched versions and missing arguments fail closed", () => {
  for (const name of ["unknown.jar", "remnant_bosses-quilt-1.20.1-2.6.1.jar", "remnant_bosses-forge-1.21.1-2.6.1.jar"]) {
    assert.throws(() => parseJar(name));
  }
  assert.throws(() => parseJar(jarName("fabric", "1.20.1"), "2.6.2"), /does not match/);
  assert.throws(() => requiredMods("fabric", "1.22"), /Unsupported/);
  assert.throws(() => parseArgs(["node", "script", "--version"]), /Missing value/);
  assert.throws(() => parseArgs(["node", "script", "--version", "2.6.1", "--jar-dir", "--dry-run"]), /Missing value/);
  assert.throws(() => parseArgs(["node", "script", "--version", "bad"]), /release version/);
  assert.throws(() => parseArgs(["node", "script", "--version", "2.6.1", "--unknown"]), /Unknown arg/);
  assert.equal(parseArgs(["node", "script", "--version", "v2.6.1"]).version, "2.6.1");
});

test("dry-run emits complete per-target payloads without credentials or network", async (t) => {
  const jarDir = fixture(t);
  const logs = [];
  let calls = 0;
  await uploadPlatforms({ version: "2.6.1", jarDir, dryRun: true }, {
    env: {}, fetchImpl: async () => { calls++; throw new Error("Network forbidden"); },
    log: (...values) => logs.push(values),
  });
  assert.equal(calls, 0);
  assert.equal(logs.filter(([label]) => String(label).includes("Modrinth")).length, 0);
  assert.equal(logs.filter(([label]) => label === "[dry-run] CurseForge").length, TARGETS.length);
  for (const [, name, json] of logs.filter(([label]) => label === "[dry-run] CurseForge")) {
    const release = parseJar(name);
    const expected = sourceDependencies(release.loader, release.game).sort();
    assert.deepEqual(JSON.parse(json).metadata.relations.projects.map((p) => p.slug).sort(), expected);
  }
});

test("CLI dry-run never reads local.env, jar bytes or calls fetch", (t) => {
  const dir = fixture(t);
  const desktop = path.join(dir, "Desktop");
  fs.mkdirSync(desktop);
  fs.writeFileSync(path.join(desktop, "local.env"), "CURSEFORGE_TOKEN=not-a-token\n");
  const preload = path.join(dir, "forbid-io.mjs");
  fs.writeFileSync(preload, `import fs from 'node:fs';
    const original = fs.readFileSync;
    fs.readFileSync = function(file, ...args) {
      if (String(file).endsWith('local.env') || String(file).endsWith('.jar')) throw new Error('Forbidden read');
      return original.call(this, file, ...args);
    };
    globalThis.fetch = () => { throw new Error('Forbidden network'); };`);
  const env = { ...process.env, USERPROFILE: dir };
  for (const key of ["CURSEFORGE_TOKEN", "CURSEFORGE_API_KEY"]) delete env[key];
  const child = spawnSync(process.execPath, ["--import", pathToFileURL(preload).href,
    path.join(ROOT, "scripts/upload_platforms.mjs"), "--version", "2.6.1", "--jar-dir", dir, "--require-all-targets", "--dry-run"],
  { encoding: "utf8", env });
  assert.equal(child.status, 0, child.stderr);
  assert.equal((child.stdout.match(/\[dry-run\] CurseForge/g) || []).length, TARGETS.length);
  assert.doesNotMatch(child.stdout, /Modrinth/);
  assert.doesNotMatch(child.stdout, /Loaded secrets|not-a-token/);
});

test("full-release and regular-file preflight stops before any upload", async (t) => {
  const jarDir = fixture(t);
  let calls = 0;
  const transport = { env: tokens, fetchImpl: async () => { calls++; }, log: silent };
  const last = path.join(jarDir, jarName("neoforge", "1.21.1"));
  fs.unlinkSync(last);
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir, requireAllTargets: true }, transport), /Missing release targets/);
  assert.equal(calls, 0);
  fs.mkdirSync(last);
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir }, transport), /Not a regular jar file/);
  assert.equal(calls, 0);
});

function mockTransport(records, { curseforgeFailure = false, modernLookupFails = false } = {}) {
  return async (url, options = {}) => {
    if (url === "https://minecraft.curseforge.com/api/game/versions") {
      return Response.json([
        { id: 1, name: "Client" }, { id: 2, name: "Server" },
        ...Object.entries(GAME_IDS).map(([name, id]) => ({ id, name })),
      ]);
    }
    if (url.startsWith("https://api.curseforge.com/v1/minecraft/version/")) {
      const game = url.split("/").at(-1);
      return modernLookupFails ? new Response("unavailable", { status: 503 })
        : Response.json({ data: { gameVersionId: GAME_IDS[game] } });
    }
    if (url === "https://minecraft.curseforge.com/api/projects/1225026/upload-file") {
      assert.equal(options.method, "POST");
      assert.ok(options.body instanceof FormData);
      const body = JSON.parse(options.body.get("metadata"));
      const file = options.body.get("file");
      const release = parseJar(file.name);
      assert.equal(await file.text(), "fixture jar");
      assert.deepEqual(body.gameVersions, [1, 2, LOADER_IDS[release.loader], GAME_IDS[release.game]]);
      assert.deepEqual(body.relations.projects.map((p) => p.slug).sort(), sourceDependencies(release.loader, release.game).sort());
      assert.ok(body.relations.projects.every((p) => p.type === "requiredDependency"));
      records.push({ platform: "curseforge", body });
      return curseforgeFailure ? new Response("test rejection", { status: 400 }) : Response.json({ id: 42 });
    }
    throw new Error(`Unexpected network target ${url}`);
  };
}

const tokens = { CURSEFORGE_TOKEN: "mock-token", CURSEFORGE_API_KEY: "mock-key" };

test("actual upload FormData sends one dependency-correct CurseForge file per jar, never Modrinth", async (t) => {
  const records = [];
  const urls = [];
  const transport = mockTransport(records);
  await uploadPlatforms({ version: "2.6.1", jarDir: fixture(t), requireAllTargets: true }, {
    env: tokens, fetchImpl: (url, options) => (urls.push(url), transport(url, options)), log: silent,
  });
  assert.equal(records.filter((r) => r.platform === "curseforge").length, TARGETS.length);
  assert.equal(new Set(records.map((r) => r.body.displayName)).size, TARGETS.length);
  assert.ok(urls.every((u) => !u.includes("modrinth")));
});

test("CurseForge upload needs no Modrinth token and retains legacy lookup fallback", async (t) => {
  const records = [];
  await uploadPlatforms({ version: "2.6.1", jarDir: fixture(t) }, {
    env: { CURSEFORGE_TOKEN: "mock-token", CURSEFORGE_API_KEY: "mock-key" },
    fetchImpl: mockTransport(records, { modernLookupFails: true }), log: silent,
  });
  assert.equal(records.filter((r) => r.platform === "curseforge").length, TARGETS.length);
});

test("live-mode missing credentials or API rejection stops further publication", async (t) => {
  const jarDir = fixture(t);
  let called = false;
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir }, {
    env: {}, fetchImpl: () => { called = true; }, log: silent,
  }), /Set CURSEFORGE_TOKEN/);
  assert.equal(called, false);
  const records = [];
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir }, {
    env: tokens, fetchImpl: mockTransport(records, { curseforgeFailure: true }), log: silent,
  }), /CurseForge 400/);
  assert.equal(records.length, 1);
});
