# 石化中间材料与 PCB 供应链 - 2026-09-06

## 本次实现

- 石化链新增乙二醇和环氧树脂两个中间材料。
- 乙二醇由乙烯和水进入化学反应器生产，环氧树脂由乙二醇和塑料颗粒进入聚合反应器生产。
- 电子链新增 PCB 基材、铜箔、焊料和贴片元件。
- PCB 基材由玻璃与环氧树脂经层压生产，铜箔由铜锭经轧制生产，焊料由金属合金化生产，贴片元件由红石、铜线和塑料颗粒经 SMT 线生产。
- 新增完整的 PCB 装配配方：PCB 基材 + 铜箔 + 焊料 + 贴片元件 + 封装芯片 → 电路板。
- 保留旧电路板配方，避免破坏已有世界和旧的自定义产业配置。
- 新材料已加入企业商品目录、创造模式物品栏、英文名称和物品模型。

## 现实依据

石化行业通常以乙烯等基础化学品继续生产聚合物和树脂；印刷电路板则以基材、铜箔、焊料和电子元件经过线路加工与装配形成。石化原料与聚合物方向参考 [EIA Hydrocarbon Gas Liquids](https://www.eia.gov/energyexplained/hydrocarbon-gas-liquids/uses-of-hydrocarbon-gas-liquids.php)；电子装配和芯片后段参考 [Semiconductor Industry Association](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)。

## 当前抽象边界

- 乙二醇的真实工业路线还涉及环氧乙烷和催化/水合步骤，当前用化学反应器抽象。
- 焊料现实中通常涉及锡、银、铜等更明确的合金体系，当前暂用铜和铁代表金属合金输入，后续可加入锡资源与无铅焊料等级。
- PCB 的光刻、蚀刻、钻孔、阻焊和 AOI 检测目前仍合并在 `pcb_assembly_line` 中。

## 后续方向

- 增加锡、银、玻纤布、铜箔等级和无铅焊料路线。
- 将 PCB 制造拆分为覆铜板、线路加工、钻孔/阻焊和 SMT 装配。
- 让 PCB 良率、芯片等级和返修率影响电子消费品的质量与售后成本。
