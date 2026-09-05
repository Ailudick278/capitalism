# 石化与电子消费品扩展（2026-09-06）

## 本次实现

- 在已有“原油 → 精炼 → 石脑油/烯烃 → 聚合物”链条上增加润滑油和沥青副产品。
- 增加聚乙烯粒料、聚丙烯粒料和合成橡胶，保留原有通用塑料粒料配方以兼容旧存档和旧配置。
- 增加调和装置（`blending_unit`），用于把炼厂馏分进一步加工为润滑油。
- 增加电源适配器、电视机、笔记本电脑三类电子消费品。
- 增加 SMT 生产线（`smt_line`），用于电源适配器等电子装配前段；电视机和笔记本电脑继续使用电子装配线。
- 新增商品初始价格、物品注册、英文名称和临时原版纹理映射。

## 现实逻辑依据

- EIA 将炼厂流程概括为分离、转化和处理，并说明原油精炼可得到燃料、石化原料等多类产品；本实现因此把炼厂建模为多产出节点，而不是单一产物机器。
- EIA 说明石脑油等石化原料经蒸汽裂解可生成乙烯、丙烯等基础化学品，这些基础化学品再进入塑料、合成橡胶等聚合物路线。
- EPA 的聚合物生产范围明确覆盖聚乙烯、聚丙烯等基础聚合物，因此第一版将两种主要牌号拆成独立商品；旧的通用塑料粒料仍作为简化路线保留。
- 半导体行业协会将芯片后段描述为切割、贴装、键合、封装和测试；现有晶圆/封装芯片路线继续沿用，新增消费品只消费封装芯片和装配件，不把晶圆直接当作终端电子产品。

## 当前简化边界

- 合成橡胶暂用“丙烯 + 石脑油”的游戏化原料代理现实中的更细分单体/聚合路线；后续可拆出丁二烯、苯乙烯和专用聚合物。
- 润滑油和沥青目前作为炼厂多产出及调和路线的商品节点，尚未加入质量等级、添加剂、粘度或硫含量。
- SMT 线目前是生产能力和机器成本的抽象，不模拟贴片机、回流焊、AOI 等单机设备。
- 电视机和笔记本电脑先作为消费品终端，后续可继续拆分存储器、摄像头、键盘、扬声器、显示驱动和包装物流。

## 后续方向

1. 将不同塑料牌号接入注塑、薄膜、包装、轮胎和家电外壳，形成跨行业需求。
2. 增加石化质量属性和副产品库存核算，避免所有炼厂产物都按同一质量处理。
3. 增加电子产品的质量、良率、返修和质保逻辑，连接现有企业质量台账。
4. 以 JSON 配置迁移方式把新增链条暴露给服务器管理员，而不是只依赖代码默认值。

## 资料

- U.S. Energy Information Administration, “Refining crude oil - the refining process”: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- U.S. Energy Information Administration, “Refining crude oil - inputs and outputs”: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-inputs-and-outputs.php
- U.S. Environmental Protection Agency, “Subpart X – Petrochemical Production”: https://www.epa.gov/ghgreporting/subpart-x-petrochemical-production
- U.S. Environmental Protection Agency, “Polymer Manufacturing Industry”: https://www.epa.gov/stationary-sources-air-pollution/polymer-manufacturing-industry-standards-performance-volatile
- Semiconductor Industry Association, “Stage 4: Back-end Manufacturing”: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/
