import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const sourcePath = path.join(root, 'src/main/java/com/ailudick/capitalismmod/data/CapitalismData.java');
const source = fs.readFileSync(sourcePath, 'utf8');

function balanced(text, start) {
  let depth = 0, quote = false, escape = false;
  for (let i = start; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (escape) escape = false;
      else if (c === '\\') escape = true;
      else if (c === '"') quote = false;
      continue;
    }
    if (c === '"') quote = true;
    else if (c === '(') depth++;
    else if (c === ')' && --depth === 0) return text.slice(start + 1, i);
  }
  throw new Error(`Unbalanced expression at ${start}`);
}

function splitArgs(text) {
  const result = [];
  let start = 0, depth = 0, quote = false, escape = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (escape) escape = false;
      else if (c === '\\') escape = true;
      else if (c === '"') quote = false;
    } else if (c === '"') quote = true;
    else if (c === '(') depth++;
    else if (c === ')') depth--;
    else if (c === ',' && depth === 0) {
      result.push(text.slice(start, i).trim());
      start = i + 1;
    }
  }
  result.push(text.slice(start).trim());
  return result;
}

function stringValue(text) {
  const match = text.match(/^"((?:\\.|[^"\\])*)"/s);
  return match ? match[1] : null;
}

function mapValue(text) {
  const result = {};
  for (const match of text.matchAll(/"([^"\\]+)"\s*,\s*(-?\d+)/g)) result[match[1]] = Number(match[2]);
  return result;
}

const recipes = [];
const marker = /new RecipeJson\s*\(/g;
let match;
while ((match = marker.exec(source))) {
  const args = splitArgs(balanced(source, match.index + match[0].length - 1));
  if (args.length < 5) continue;
  recipes.push({
    industry: [...source.slice(0, match.index).matchAll(/withRecipes\(new IndustryJson\("([^"]+)"/g)].at(-1)?.[1] ?? 'unknown',
    id: stringValue(args[0]) ?? 'unknown',
    inputs: mapValue(args[1]),
    outputs: mapValue(args[2]),
    income: Number(args[3]) || 0,
    machine: stringValue(args[4]) ?? 'unknown',
    workers: Number(args[5]) || 0,
    energyCost: Number(args[6]) || 0,
    maintenanceCost: Number(args[7]) || 0
  });
}

const producerMap = new Map();
function materialClass(recipe) {
  const key = `${recipe.id} ${Object.keys(recipe.outputs).join(' ')}`.toLowerCase();
  if (/fertilizer|herbicide|fungicide|pesticide/.test(key)) return 'agrochemical';
  if (/tablet|pharmaceutical|aspirin|medical/.test(key)) return 'pharmaceutical';
  if (/rubber|tire|resin|pellet|polymer/.test(key)) return 'polymer_material';
  if (/paper|textile|fiber|ink|coating/.test(key)) return 'consumer_material';
  if (/water|disinfect|chemical|acid|chlorine|oxygen|nitrogen/.test(key)) return 'chemical_intermediate';
  return 'industrial_intermediate';
}
function qualityTier(recipe) {
  const key = `${recipe.id} ${Object.keys(recipe.outputs).join(' ')}`.toLowerCase();
  if (/electronic|semiconductor|sterile|high_purity/.test(key)) return 'high_purity';
  if (/medical|food_grade|pharmaceutical/.test(key)) return 'regulated';
  return 'industrial';
}
function pollutionScore(recipe) {
  const key = `${recipe.id} ${recipe.machine}`.toLowerCase();
  if (/capture|recycl|purif/.test(key)) return 1;
  if (/pesticide|chlor|solvent|pharmaceutical|acid/.test(key)) return 5;
  if (/ferment|water/.test(key)) return 2;
  return 3;
}
function hazardous(recipe) {
  const key = `${recipe.id} ${Object.keys(recipe.inputs).join(' ')} ${Object.keys(recipe.outputs).join(' ')}`.toLowerCase();
  return /acid|chlor|peroxide|pesticide|herbicide|fungicide|solvent|hydrogen/.test(key);
}
const classStats = Object.create(null);
const qualityStats = Object.create(null);
let pollutionTotal = 0;
let hazardousCount = 0;
for (const recipe of recipes) {
  const cls = materialClass(recipe);
  const quality = qualityTier(recipe);
  classStats[cls] = (classStats[cls] ?? 0) + 1;
  qualityStats[quality] = (qualityStats[quality] ?? 0) + 1;
  pollutionTotal += pollutionScore(recipe);
  if (hazardous(recipe)) hazardousCount++;
}
for (const recipe of recipes) for (const item of Object.keys(recipe.outputs)) {
  if (!producerMap.has(item)) producerMap.set(item, []);
  producerMap.get(item).push(recipe.id);
}

const missing = [];
const selfLoops = [];
const edges = new Set();
for (const recipe of recipes) {
  for (const item of Object.keys(recipe.inputs)) {
    if (!producerMap.has(item)) missing.push({ recipe: recipe.id, item });
    for (const output of Object.keys(recipe.outputs)) edges.add(`${item}|${output}`);
    if (item in recipe.outputs) selfLoops.push({ recipe: recipe.id, item });
  }
}

const graph = new Map();
for (const edge of edges) {
  const [from, to] = edge.split('|');
  if (!graph.has(from)) graph.set(from, new Set());
  graph.get(from).add(to);
}
const cycles = [];
const visiting = new Set(), visited = new Set(), stack = [];
function visit(node) {
  if (visiting.has(node)) {
    const index = stack.indexOf(node);
    cycles.push([...stack.slice(index), node]);
    return;
  }
  if (visited.has(node)) return;
  visiting.add(node); stack.push(node);
  for (const next of graph.get(node) ?? []) visit(next);
  stack.pop(); visiting.delete(node); visited.add(node);
}
for (const node of graph.keys()) visit(node);

const machineSource = fs.readFileSync(path.join(root, 'src/main/java/com/ailudick/capitalismmod/company/MachineType.java'), 'utf8');
const blockSource = fs.readFileSync(path.join(root, 'src/main/java/com/ailudick/capitalismmod/init/ModBlocks.java'), 'utf8');
const resourceFiles = [
  ...fs.globSync(path.join(root, 'src/main/resources/data/**/*.json')),
  ...fs.globSync(path.join(root, 'src/main/resources/data/**/**/*.json'))
];
const resourceText = resourceFiles.map(file => fs.readFileSync(file, 'utf8')).join('\n');
const resourceItems = new Set([
  ...[...resourceText.matchAll(/"name"\s*:\s*"([a-z0-9_]+:[a-z0-9_]+)"/g)].map(m => m[1]),
  ...[...resourceText.matchAll(/"result"\s*:\s*\{[^}]*"id"\s*:\s*"([a-z0-9_]+:[a-z0-9_]+)"/g)].map(m => m[1])
]);
for (const match of blockSource.matchAll(/register\("([a-z0-9_]+)"/g)) resourceItems.add(`capitalismmod:${match[1]}`);
const machines = new Set([...machineSource.matchAll(/\("([a-z0-9_]+)"\s*,/g)].map(m => m[1]));
const unknownMachines = recipes.filter(r => r.machine !== 'none' && !machines.has(r.machine));
const duplicateIds = [...new Set(recipes.map(r => r.id).filter((id, i, all) => all.indexOf(id) !== i))];
const machineMaterials = new Map();
for (const match of machineSource.matchAll(/\("([a-z0-9_]+)"\s*,\s*[^,]+,\s*[^,]+,\s*Map\.of\(([^)]*)\)\)/gs)) {
  const materialEntries = [...match[2].matchAll(/"([^"\\]+)"\s*,\s*(\d+)/g)]
    .map(m => [m[1], Number(m[2])]);
  machineMaterials.set(match[1], Object.fromEntries(materialEntries));
}
const machineMaterialGaps = [];
for (const recipe of recipes) {
  const materials = machineMaterials.get(recipe.machine) ?? {};
  for (const item of Object.keys(materials)) {
    if (!producerMap.has(item) && !resourceItems.has(item) && item.startsWith('capitalismmod:')) machineMaterialGaps.push({ machine: recipe.machine, item });
  }
}
const zeroEnergyRecipes = recipes.filter(r => Object.keys(r.outputs).length > 0 && r.machine !== 'none' && r.energyCost === 0);
const internalMissing = missing.filter(x => x.item.startsWith('capitalismmod:'));
const unresolvedInternalMissing = internalMissing.filter(x => !resourceItems.has(x.item));

const esc = s => s.replaceAll('|', '/');
const mermaidLines = ['flowchart LR'];
const shownEdges = [...edges].slice(0, 180);
for (const edge of shownEdges) {
  const [from, to] = edge.split('|');
  const a = esc(from).replaceAll(':', '_');
  const b = esc(to).replaceAll(':', '_');
  mermaidLines.push(`  ${a}["${from}"] --> ${b}["${to}"]`);
}
if (edges.size > shownEdges.length) mermaidLines.push(`  note["仅展示前 ${shownEdges.length} 条边，共 ${edges.size} 条"]`);

const lines = [
  `- Material class distribution: ${JSON.stringify(classStats)}`,
  `- Quality tier distribution: ${JSON.stringify(qualityStats)}`,
  `- Hazardous process recipes: ${hazardousCount}; pollution score total: ${pollutionTotal}`,
  '# 工业产业链数据审计（默认基线）', '',
  `审计来源：\`${path.relative(root, sourcePath).replaceAll('\\', '/') }\``,
  '说明：这是对 `CapitalismData.defaultIndustries()` 中 Java 默认配方的静态审计；运行时 `config/capitalismmod/industries.json` 可能覆盖或补充结果。', '',
  '## 总览', '',
  `- 配方数：${recipes.length}`,
  `- 产出物料数：${producerMap.size}`,
  `- 输入→输出关系数：${edges.size}`,
  `- 未发现配方生产者的输入：${missing.length} 条（其中 capitalismmod: 内部物料 ${internalMissing.length} 条）`,
  `- 扣除方块掉落等资源来源后仍未解释的内部物料：${unresolvedInternalMissing.length} 条`,
  `- 自循环配方：${selfLoops.length} 条`,
  `- 检测到的循环路径：${cycles.length} 条`,
  `- 未识别机器类型：${unknownMachines.length} 条`,
  `- 设备材料中未找到配方或资源来源的物料：${new Set(machineMaterialGaps.map(x => x.item)).size} 种`,
  `- 有产出但能源成本为 0 的机器配方：${zeroEnergyRecipes.length} 条`,
  `- 重复配方 ID：${duplicateIds.length} 个`, '',
  '## 重点问题', ''
];
if (missing.length) {
  lines.push('### 无配方生产者输入（前 80 条）', '', '这些输入可能来自原版资源、矿石掉落或其他数据包，因此不自动判定为断链。', '');
  for (const item of missing.slice(0, 80)) lines.push(`- ${item.recipe} 需要 ${item.item}，但默认配方没有产出它。`);
  lines.push('');
}
if (cycles.length) {
  lines.push('### 循环路径（前 30 条）', '');
  for (const cycle of cycles.slice(0, 30)) lines.push(`- ${cycle.join(' → ')}`);
  lines.push('');
}
if (selfLoops.length) {
  lines.push('### 自循环', '');
  for (const item of selfLoops) lines.push(`- 配方 ${item.recipe} 同时消耗并产出 ${item.item}`);
  lines.push('');
}
if (unknownMachines.length) {
  lines.push('### 未识别机器', '');
  for (const item of unknownMachines) lines.push(`- 配方 ${item.id} 使用 ${item.machine}`);
  lines.push('');
}
if (machineMaterialGaps.length) {
  lines.push('### 设备材料来源缺口', '');
  for (const item of [...new Map(machineMaterialGaps.map(x => [`${x.machine}|${x.item}`, x])).values()].slice(0, 80)) lines.push(`- 机器 ${item.machine} 需要 ${item.item}，但未找到生产配方或资源来源。`);
  lines.push('');
}
if (zeroEnergyRecipes.length) {
  lines.push('### 能源成本为 0 的生产配方', '', '这不一定是错误，但需要确认是否代表无需能源的工艺，还是遗漏了能源参数。', '');
  for (const item of zeroEnergyRecipes.slice(0, 80)) lines.push(`- 配方 ${item.id} 使用 ${item.machine}，能源成本为 0。`);
  lines.push('');
}
lines.push('## 可视化', '', '下面的 Mermaid 图展示默认配方形成的物料流关系；同一物料的多个生产者会汇入同一个节点。', '', '```mermaid', ...mermaidLines, '```', '');
lines.push('## 审计结论', '',
  '- `minecraft:` 开头的未生产物料通常是外部基础资源，不等同于错误。',
  '- `capitalismmod:` 开头且没有生产者的物料是优先核查对象，可能代表断链、遗漏配方或依赖外部数据包。',
  '- 循环路径需要区分“合法回收/再制造闭环”和“无法自然启动的死循环”，不能一律视为错误。',
  '- 当前能源字段以每周期货币化成本表示；代码尚未声明统一的电力/燃料载体，因此能源可达性只能作为后续审计项。',
  '- 下一步应将同一套审计逻辑接入运行时配置，并增加设备、能源、产能和配方可达性检查。');

const industryMatches = [...source.matchAll(/new IndustryJson\("([^"]+)"/g)];
const recipesById = new Map(recipes.map(recipe => [recipe.id, recipe]));
lines.push('## 当前产业链清单', '', '以下清单按默认行业定义整理；输入中的 minecraft: 物料通常来自原版资源，capitalismmod: 物料通常由其他工业配方或矿石掉落提供。', '');
for (let i = 0; i < industryMatches.length; i++) {
  const start = industryMatches[i].index;
  const end = i + 1 < industryMatches.length ? industryMatches[i + 1].index : source.length;
  const block = source.slice(start, end);
  const ids = [...block.matchAll(/new RecipeJson\("([^"]+)"/g)].map(m => m[1]);
  const recipeIds = ids.length ? ids : [industryMatches[i][1]];
  lines.push(`### ${industryMatches[i][1]}`, '');
  for (const id of recipeIds) {
    const recipe = recipesById.get(id);
    if (!recipe) { lines.push(`- ${id}（默认配方，未能从源码提取详细字段）`); continue; }
    const input = Object.entries(recipe.inputs).map(([item, quantity]) => `${item} ×${quantity}`).join(' + ') || '无输入';
    const output = Object.entries(recipe.outputs).map(([item, quantity]) => `${item} ×${quantity}`).join(' + ') || `服务收入 ${recipe.income}`;
    lines.push(`- ${recipe.id}：${input} → ${output}；设备 ${recipe.machine}；工人 ${recipe.workers}；能源 ${recipe.energyCost}；维护 ${recipe.maintenanceCost}`);
  }
  lines.push('');
}

const out = path.join(root, 'docs/progress/industry-chain-audit.md');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, lines.join('\n'), 'utf8');
console.log(JSON.stringify({ recipes: recipes.length, products: producerMap.size, edges: edges.size, missing: missing.length, internalMissing: internalMissing.length, cycles: cycles.length, selfLoops: selfLoops.length, unknownMachines: unknownMachines.length, report: path.relative(root, out) }, null, 2));

export { recipes };
