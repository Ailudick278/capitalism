# 石化基础油与润滑油调和链 - 2026-09-06

## 本次实现

- 炼油配方现在产出基础油 `base_oil`，不再直接产出润滑油。
- 调和装置使用基础油和燃料油生产润滑油，形成“原油 → 炼油馏分/基础油 → 调和润滑油”的连续链条。
- 保留原有 `lubricant` 商品和物品 ID，旧存档库存不会被删除；只改变新生产批次的来源。
- 为基础油补充商品初始价格、创造模式入口、名称和物品模型。

## 现实逻辑依据

炼厂先通过分离、转化和处理得到不同馏分与中间油品；润滑油通常还需要基础油与添加剂进行调和，不能把所有润滑油都视为炼油塔直接产物。本阶段用燃料油作为添加剂/调和组分的游戏化代理，后续可以继续拆出黏度等级和添加剂。

参考资料：

- U.S. Energy Information Administration, Refining crude oil - the refining process: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- U.S. Energy Information Administration, U.S. refinery and blender net production: https://www.eia.gov/dnav/pet/pet_pnp_refp_dc_nus_mbbl_a.htm

## 兼容与边界

- 没有删除旧物品，也没有修改已有库存；旧的润滑油仍可交易和作为生产投入。
- 没有改变企业配方选择、设备维护、税务或贷款规则。
- 当前仍未模拟基础油黏度、添加剂配方、硫含量和质量等级。
