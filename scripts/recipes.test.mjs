import assert from 'node:assert/strict';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = fileURLToPath(new URL('../', import.meta.url));
for (const game of ['1.20.1', '1.21.1']) {
  const modern = game === '1.21.1';
  const resources = `${root}/${game}/common/src/main/resources`;
  const recipes = `data/remnant_bosses/${modern ? 'recipe' : 'recipes'}`;
  const loot = `data/remnant_bosses/${modern ? 'loot_table' : 'loot_tables'}/blocks`;
  test(`${game}: crafting resources can be discovered and resolve the registered output IDs`, () => {
    for (const name of ['ancient_altar', 'ancient_pedestal', 'fang_on_a_stick', 'skeleton_skull', 'skeleton_skull_2']) {
      const file = `${resources}/${recipes}/${name}_recipe.json`;
      assert.ok(fs.existsSync(file), `Minecraft cannot discover ${file}`);
      const json = JSON.parse(fs.readFileSync(file));
      const output = name.startsWith('skeleton_skull') ? 'minecraft:skeleton_skull' : `remnant_bosses:${name}`;
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
    const supported = pack.supported_formats ?? { min_inclusive: pack.pack_format, max_inclusive: pack.pack_format };
    for (const format of modern ? [34, 48] : [15]) {
      assert.ok(supported.min_inclusive <= format && supported.max_inclusive >= format, `Pack rejects format ${format}`);
    }
  });
}
