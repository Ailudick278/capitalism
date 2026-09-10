import assert from 'node:assert/strict';
import fs from 'node:fs';
import { recipes } from './industry-chain-audit.mjs';

const readJson = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));

const oreDrops = new Set();
for (const file of fs.globSync('src/main/resources/data/capitalismmod/loot_table/blocks/*ore.json')) {
  const collect = value => {
    if (!value || typeof value !== 'object') return;
    if (value.type === 'minecraft:item' && value.name) oreDrops.add(value.name);
    Object.values(value).forEach(collect);
  };
  collect(readJson(file));
}

// Ingredient reachability, not a claim about machine construction, quantities or profitability.
// Leaves are supplied resources. A recycling loop cannot seed its own missing ingredients.
function reachable(blocked = '') {
  const blockedItems = new Set(Array.isArray(blocked) ? blocked : [blocked]);
  const produced = new Set(recipes.flatMap(r => Object.keys(r.outputs)));
  const available = new Set(recipes.flatMap(r => Object.keys(r.inputs)).filter(i => !produced.has(i)));
  recipes.flatMap(r => Object.keys(r.inputs)).filter(i => i.startsWith('minecraft:')).forEach(i => available.add(i));
  oreDrops.forEach(i => available.add(i));
  blockedItems.forEach(item => available.delete(item));
  let changed;
  do {
    changed = false;
    for (const recipe of recipes) {
      if (!Object.keys(recipe.inputs).every(i => available.has(i))) continue;
      for (const output of Object.keys(recipe.outputs)) {
        if (blockedItems.has(output) || available.has(output)) continue;
        available.add(output);
        changed = true;
      }
    }
  } while (changed);
  return available;
}

const gates = [
  ['alumina', 'aluminum_ingot'], ['copper_cathode', 'copper_foil'],
  ['silicon_ingot', 'silicon_wafer'], ['tested_wafer', 'packaged_chip'],
  ['assembled_pcb', 'circuit_board'], ['final_chip_test', 'circuit_board'],
  ['electronic_etchant', 'circuit_board'], ['cobalt_ingot', 'nmc_cathode'],
  ['manganese_ingot', 'nmc_cathode'], ['phosphoric_acid', 'lfp_cathode'],
  ['reformate', 'gasoline'], ['reformate', 'benzene'], ['reformate', 'p_xylene'],
  ['gas_oil', 'diesel'], ['vacuum_resid', 'asphalt'],
  ['c4_fraction', 'butadiene'], ['butadiene', 'synthetic_rubber'],
  ['ethylbenzene', 'styrene_monomer'], ['acrylonitrile', 'abs_resin'],
  ['bisphenol_a', 'epoxy_resin'], ['epichlorohydrin', 'epoxy_resin'],
  ['ethylene_oxide', 'ethylene_glycol'], ['ethylene_dichloride', 'vinyl_chloride'],
  ['brine', 'caustic_soda'], ['compressed_air', 'nitrogen'],
  ['ammonia', 'nitric_acid'], ['ammonia', 'urea'], ['carbon_dioxide', 'urea'],
  ['formaldehyde', 'phenolic_resin']
];
const baseline = reachable();
for (const [gate, target] of gates) {
  assert(baseline.has(`capitalismmod:${target}`), `Unreachable baseline: ${target}`);
  assert(!reachable(`capitalismmod:${gate}`).has(`capitalismmod:${target}`),
    `${gate} can be bypassed when producing ${target}`);
}

// Both methanol routes remain useful; recycled carbon is an intentional alternative.
assert(reachable('capitalismmod:syngas').has('capitalismmod:methanol'), 'CO2 methanol route is blocked');
assert(reachable('capitalismmod:carbon_dioxide').has('capitalismmod:methanol'), 'Syngas methanol route is blocked');
assert(!reachable(['capitalismmod:syngas', 'capitalismmod:carbon_dioxide']).has('capitalismmod:methanol'),
  'Methanol bypasses both carbon feedstocks');
assert(reachable('capitalismmod:refinery_gas').has('capitalismmod:syngas'), 'Coal gasification cannot start');
assert(reachable('capitalismmod:crude_oil').has('capitalismmod:hydrogen'), 'Non-oil hydrogen cannot start');
assert(reachable('capitalismmod:crude_oil').has('capitalismmod:urea'), 'Non-oil fertilizer chain cannot start');

const newPetrochemicalItems = ['syngas', 'reformate', 'c4_fraction', 'ethylbenzene',
  'acrylonitrile', 'bisphenol_a', 'epichlorohydrin'];
const source = fs.readFileSync('src/main/java/com/ailudick/capitalismmod/data/CapitalismData.java', 'utf8');
const creativeTabs = fs.readFileSync('src/main/java/com/ailudick/capitalismmod/init/ModCreativeTabs.java', 'utf8');
for (const id of newPetrochemicalItems) {
  assert(baseline.has(`capitalismmod:${id}`), `New intermediate cannot be produced: ${id}`);
  assert(source.includes(`new CommodityJson("capitalismmod:${id}",`), `Missing market default: ${id}`);
  assert(creativeTabs.includes(`output.accept(ModItems.${id.toUpperCase()}.get())`), `Missing creative entry: ${id}`);
}

const recipeById = new Map(recipes.map(recipe => [recipe.id, recipe]));
assert.equal(recipes.find(recipe => recipe.industry === 'petrochemical_refining').id, 'atmospheric_distillation',
  'New recipes must not change the existing default refining selection');
assert.deepEqual(recipeById.get('air_compression').inputs, {}, 'Air compression must use ambient air');
assert(!recipeById.get('gas_reforming').outputs['capitalismmod:lpg'], 'Reforming must not produce LPG');
assert(!recipeById.get('ethylene_dichloride_cracking').outputs['capitalismmod:hydrogen'], 'EDC cracking produces HCl, not H2');
assert(!recipeById.get('pvc_pipe_recycling').outputs['capitalismmod:chlorine'], 'Mechanical PVC recycling must not release reusable chlorine');
for (const id of ['packaging_film_recycling', 'plastic_container_recycling'])
  assert.deepEqual(Object.keys(recipeById.get(id).outputs), ['capitalismmod:reclaimed_plastic']);

const registered = fs.readFileSync('src/main/java/com/ailudick/capitalismmod/init/ModItems.java', 'utf8');
const languages = ['en_us', 'zh_cn'].map(lang => readJson(
  `src/main/resources/assets/capitalismmod/lang/${lang}.json`));
const items = new Set(recipes.flatMap(r => [...Object.keys(r.inputs), ...Object.keys(r.outputs)])
  .filter(i => i.startsWith('capitalismmod:')));
const missingTranslations = [];
for (const item of items) {
  const id = item.split(':')[1];
  assert(registered.includes(`register("${id}"`), `Unregistered item: ${id}`);
  const model = `src/main/resources/assets/capitalismmod/models/item/${id}.json`;
  assert(fs.existsSync(model), `Missing model: ${id}`);
  const parsedModel = readJson(model);
  for (const texture of Object.values(parsedModel.textures ?? {})) {
    if (texture.startsWith('capitalismmod:'))
      assert(fs.existsSync(`src/main/resources/assets/capitalismmod/textures/${texture.split(':')[1]}.png`),
        `Missing referenced texture for ${id}: ${texture}`);
  }
  for (const [index, language] of languages.entries()) {
    if (!language[`item.capitalismmod.${id}`] && !language[`block.capitalismmod.${id}`])
      missingTranslations.push(`${['en_us', 'zh_cn'][index]}:${id}`);
  }
}
const missingChinese = missingTranslations.filter(key => key.startsWith('zh_cn:'));
assert.equal(missingChinese.length, 0, `Missing Chinese names: ${missingChinese.join(', ')}`);
for (const id of newPetrochemicalItems)
  assert(languages.every(language => language[`item.capitalismmod.${id}`]), `Missing new bilingual name: ${id}`);
console.log(`PASS: ${gates.length} production gates; ${items.size} registered materials with models and Chinese names.`);
console.log(`Existing English localization backlog: ${missingTranslations.filter(key => key.startsWith('en_us:')).length} material names.`);
