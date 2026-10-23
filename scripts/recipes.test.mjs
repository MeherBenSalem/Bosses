import assert from 'node:assert/strict';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = fileURLToPath(new URL('../', import.meta.url));
const crafting = [
  ['ancient_altar_recipe', 'remnant_bosses:ancient_altar'],
  ['ancient_pedestal_recipe', 'remnant_bosses:ancient_pedestal'],
  ['skeleton_skull_recipe', 'minecraft:skeleton_skull'],
  ...['boss_journal', 'eclipse_shard', 'eclipse_waygate', 'sovereigns_seal',
    'regalia_cuirass', 'regalia_greaves', 'regalia_sabatons', 'crypt_key',
    'memory_lens', 'archivist_hood', 'archivist_robe', 'archivist_leggings', 'archivist_boots']
    .map((name) => [name, `remnant_bosses:${name}`]),
];
// Resource and data pack formats each game version must accept.
const packFormats = { '1.20.1': [15], '1.21.1': [34, 48], '26.2': [88, 107], '26.3': [97, 121] };
for (const game of Object.keys(packFormats)) {
  const modern = game !== '1.20.1';
  const resources = `${root}/${game}/common/src/main/resources`;
  const recipes = `data/remnant_bosses/${modern ? 'recipe' : 'recipes'}`;
  const loot = `data/remnant_bosses/${modern ? 'loot_table' : 'loot_tables'}/blocks`;
  test(`${game}: crafting resources can be discovered and resolve the registered output IDs`, () => {
    for (const [name, output] of crafting) {
      const file = `${resources}/${recipes}/${name}.json`;
      assert.ok(fs.existsSync(file), `Minecraft cannot discover ${file}`);
      const json = JSON.parse(fs.readFileSync(file));
      assert.equal(json.result[modern ? 'id' : 'item'], output);
      assert.equal(json.result.count, 1);
    }
  });
  test(`${game}: both ritual blocks have discoverable self-drop tables`, () => {
    for (const name of ['ancient_altar', 'ancient_pedestal']) {
      const file = `${resources}/${loot}/${name}.json`;
      assert.ok(fs.existsSync(file), `Minecraft cannot discover ${file}`);
      const json = JSON.parse(fs.readFileSync(file));
      assert.equal(json.type, 'minecraft:block');
      assert.equal(json.pools[0].entries[0].name, `remnant_bosses:${name}`);
      assert.equal(json.pools[0].rolls, 1);
    }
  });
  test(`${game}: pack metadata accepts the client resources and server data format`, () => {
    const pack = JSON.parse(fs.readFileSync(`${resources}/pack.mcmeta`)).pack;
    const supported = 'min_format' in pack ? { min_inclusive: pack.min_format, max_inclusive: pack.max_format }
      : pack.supported_formats ?? { min_inclusive: pack.pack_format, max_inclusive: pack.pack_format };
    for (const format of packFormats[game]) {
      assert.ok(supported.min_inclusive <= format && supported.max_inclusive >= format, `Pack rejects format ${format}`);
    }
  });
}
