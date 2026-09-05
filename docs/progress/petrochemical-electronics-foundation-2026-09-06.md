# 石化与电子消费品产业基础 - 2026-09-06

## 本次实现

### 石化链

- 新增设备：油井 `oil_well`、炼油厂 `oil_refinery`、聚合反应器 `polymer_reactor`。
- 新增物品：原油 `crude_oil`、石脑油 `naphtha`、塑料粒料 `plastic_pellets`。
- 基础链条：油井产出原油 → 炼油厂分离出石脑油 → 聚合反应器生产塑料粒料。
- 塑料粒料继续供应注塑机，用于生产风扇叶片和后续塑料外壳。

### 电子消费品链

- 新增设备：半导体制造设备 `semiconductor_fab`、电子装配线 `electronics_assembly`。
- 新增物品：硅晶圆 `silicon_wafer`、电路板 `circuit_board`、电池 `battery`、智能手机 `smartphone`。
- 基础链条：石英/碳还原材料 → 硅晶圆 → 电路板；电池与电路板、镜片、塑料粒料 → 智能手机。
- 电子装配设备与通用装配线分开，体现电子产品需要专用装配与测试能力。

## 现实依据

EIA 说明炼厂通过分离、转化、处理把原油变成燃料和化工原料，石脑油等石化原料可进入裂解与塑料生产；EPA 资料说明塑料粒料经加热、均化并注入模具形成塑料件；美国能源部的半导体供应链报告将半导体视为消费电子的重要上游环节。

参考资料：

- https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- https://www.eia.gov/tools/faqs/faq.php?id=34
- https://www.epa.gov/sites/default/files/2017-06/documents/pv29_scope_06-22-17.pdf
- https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf

## 当前抽象边界

- 油井目前是企业设备，不区分油田储量、井位、钻井周期和伴生气；后续可接入地图资源分布。
- 石脑油与聚合物是简化的中间品，没有展开乙烯、丙烯、聚合催化剂和副产物。
- 半导体制造用石英和碳材料表达硅材料来源，没有展开高纯硅、光刻、掺杂、封装和良率。
- 这些简化只用于保留产业方向和经营决策，不代表现实配方比例。

## 后续方向

- 在世界地图上生成油田区块，要求油井建设在可采资源范围内，并设置储量衰减。
- 增加炼油副产品、燃料和化工原料的联产关系，避免炼厂只产出单一石脑油。
- 增加芯片良率、设备折旧、研发投入、产品质量与售后维修，形成电子企业的差异化经营。
