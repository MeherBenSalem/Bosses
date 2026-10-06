import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { DEPENDENCY_PROJECTS, LOADER_IDS, modrinthDependencies,
  modrinthMetadata, parseJar } from "./release_metadata.mjs";
import { parseArgs, uploadPlatforms } from "./upload_platforms.mjs";

const ROOT = fileURLToPath(new URL("../", import.meta.url));
const TARGETS = [
  ["fabric", "1.20.1"], ["forge", "1.20.1"],
  ["fabric", "1.21.1"], ["neoforge", "1.21.1"],
];
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

test("all four payloads match the loader runtime requirements", () => {
  for (const [loader, game] of TARGETS) {
    const release = parseJar(jarName(loader, game), "2.6.1");
    const body = modrinthMetadata(release, { version: "2.6.1", changelog: "Test" });
    const expected = sourceDependencies(loader, game).map((mod) => DEPENDENCY_PROJECTS[mod]).sort();
    assert.deepEqual(body.dependencies.map((d) => d.project_id).sort(), expected);
    assert.equal(body.dependencies.length, loader === "fabric" ? 3 : 2);
    assert.ok(body.dependencies.every((d) => d.dependency_type === "required" && d.version_id === null && d.file_name === null));
    assert.deepEqual(body.loaders, [loader]);
    assert.deepEqual(body.game_versions, [game]);
    assert.equal(body.version_number, `2.6.1+${loader}-${game}`);
    assert.equal(body.project_id, "coZxDAGe");
    assert.deepEqual(body.file_parts, ["file_0"]);
    assert.equal(body.primary_file, "file_0");
  }
});

test("invalid targets, mismatched versions and missing arguments fail closed", () => {
  for (const name of ["unknown.jar", "remnant_bosses-quilt-1.20.1-2.6.1.jar", "remnant_bosses-forge-1.21.1-2.6.1.jar"]) {
    assert.throws(() => parseJar(name));
  }
  assert.throws(() => parseJar(jarName("fabric", "1.20.1"), "2.6.2"), /does not match/);
  assert.throws(() => modrinthDependencies("fabric", "1.22"), /Unsupported/);
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
  const previews = logs.filter(([label]) => label === "[dry-run] Modrinth");
  assert.equal(previews.length, 4);
  for (const [, name, json] of previews) {
    const release = parseJar(name);
    assert.deepEqual(JSON.parse(json).dependencies, modrinthDependencies(release.loader, release.game));
  }
  assert.equal(logs.filter(([label]) => label === "[dry-run] CurseForge").length, 4);
});

test("CLI dry-run never reads local.env, jar bytes or calls fetch", (t) => {
  const dir = fixture(t);
  const desktop = path.join(dir, "Desktop");
  fs.mkdirSync(desktop);
  fs.writeFileSync(path.join(desktop, "local.env"), "MODRINTH_TOKEN=not-a-token\n");
  const preload = path.join(dir, "forbid-io.mjs");
  fs.writeFileSync(preload, `import fs from 'node:fs';
    const original = fs.readFileSync;
    fs.readFileSync = function(file, ...args) {
      if (String(file).endsWith('local.env') || String(file).endsWith('.jar')) throw new Error('Forbidden read');
      return original.call(this, file, ...args);
    };
    globalThis.fetch = () => { throw new Error('Forbidden network'); };`);
  const env = { ...process.env, USERPROFILE: dir };
  for (const key of ["MODRINTH_TOKEN", "CURSEFORGE_TOKEN", "CURSEFORGE_API_KEY"]) delete env[key];
  const child = spawnSync(process.execPath, ["--import", preload,
    path.join(ROOT, "scripts/upload_platforms.mjs"), "--version", "2.6.1", "--jar-dir", dir, "--require-all-targets", "--dry-run"],
  { encoding: "utf8", env });
  assert.equal(child.status, 0, child.stderr);
  assert.equal((child.stdout.match(/\[dry-run\] Modrinth/g) || []).length, 4);
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

function mockTransport(records, { modrinthFailure = false, modernLookupFails = false } = {}) {
  return async (url, options = {}) => {
    if (url === "https://api.modrinth.com/v2/version") {
      assert.equal(options.method, "POST");
      assert.ok(options.body instanceof FormData);
      assert.deepEqual([...options.body.keys()], ["data", "file_0"]);
      const body = JSON.parse(options.body.get("data"));
      const file = options.body.get("file_0");
      assert.equal(await file.text(), "fixture jar");
      assert.deepEqual(body, modrinthMetadata(parseJar(file.name), {
        version: "2.6.1", changelog: body.changelog, projectId: "coZxDAGe",
      }));
      records.push({ platform: "modrinth", body });
      return new Response(modrinthFailure ? "test rejection" : "{}", { status: modrinthFailure ? 400 : 200 });
    }
    if (url === "https://minecraft.curseforge.com/api/game/versions") {
      return Response.json([
        { id: 1, name: "Client" }, { id: 2, name: "Server" },
        { id: 1201, name: "1.20.1" }, { id: 1211, name: "1.21.1" },
      ]);
    }
    if (url.startsWith("https://api.curseforge.com/v1/minecraft/version/")) {
      const game = url.split("/").at(-1);
      return modernLookupFails ? new Response("unavailable", { status: 503 })
        : Response.json({ data: { gameVersionId: game === "1.20.1" ? 1201 : 1211 } });
    }
    if (url === "https://minecraft.curseforge.com/api/projects/1225026/upload-file") {
      assert.equal(options.method, "POST");
      assert.ok(options.body instanceof FormData);
      const body = JSON.parse(options.body.get("metadata"));
      const file = options.body.get("file");
      const release = parseJar(file.name);
      assert.equal(await file.text(), "fixture jar");
      assert.deepEqual(body.gameVersions, [1, 2, LOADER_IDS[release.loader], release.game === "1.20.1" ? 1201 : 1211]);
      records.push({ platform: "curseforge", body });
      return Response.json({ id: 42 });
    }
    throw new Error(`Unexpected network target ${url}`);
  };
}

const tokens = { MODRINTH_TOKEN: "mock-token", CURSEFORGE_TOKEN: "mock-token", CURSEFORGE_API_KEY: "mock-key" };

test("actual upload FormData sends four separate dependency-correct releases", async (t) => {
  const records = [];
  await uploadPlatforms({ version: "2.6.1", jarDir: fixture(t), requireAllTargets: true }, {
    env: tokens, fetchImpl: mockTransport(records), log: silent,
  });
  assert.equal(records.filter((r) => r.platform === "modrinth").length, 4);
  assert.equal(records.filter((r) => r.platform === "curseforge").length, 4);
  assert.equal(new Set(records.filter((r) => r.platform === "modrinth").map((r) => r.body.version_number)).size, 4);
});

test("CurseForge-only needs no Modrinth token and retains legacy lookup fallback", async (t) => {
  const records = [];
  await uploadPlatforms({ version: "2.6.1", jarDir: fixture(t), curseforgeOnly: true }, {
    env: { CURSEFORGE_TOKEN: "mock-token", CURSEFORGE_API_KEY: "mock-key" },
    fetchImpl: mockTransport(records, { modernLookupFails: true }), log: silent,
  });
  assert.equal(records.filter((r) => r.platform === "modrinth").length, 0);
  assert.equal(records.filter((r) => r.platform === "curseforge").length, 4);
});

test("live-mode missing credentials or API rejection stops further publication", async (t) => {
  const jarDir = fixture(t);
  let called = false;
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir }, {
    env: {}, fetchImpl: () => { called = true; }, log: silent,
  }), /Set MODRINTH_TOKEN/);
  assert.equal(called, false);
  const records = [];
  await assert.rejects(uploadPlatforms({ version: "2.6.1", jarDir }, {
    env: tokens, fetchImpl: mockTransport(records, { modrinthFailure: true }), log: silent,
  }), /Modrinth 400/);
  assert.equal(records.length, 1);
});
