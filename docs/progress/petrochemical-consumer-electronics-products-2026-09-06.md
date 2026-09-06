# 石化与电子消费品扩展 — 2026-09-06

## 本次实现

- 在已有“原油/炼油/石脑油/烯烃/聚合物/塑料粒子”链条上继续接入两个终端消费品：充电宝、智能音箱。
- 充电宝使用电池包、充电接口、电源管理芯片和 ABS 树脂，体现电芯组装、电源管理、外壳注塑和电子装配的分工。
- 智能音箱使用无线通信模组、扬声器模组、麦克风模组、封装芯片和 ABS 树脂，体现芯片、声学器件、无线连接与塑料外壳的整机装配。
- 两个产品复用电子装配线，不新增“一种产品一台专机”的不现实约束；上游仍可由不同企业分别供应零部件。

## 现实依据与简化边界

- 石化链条继续把炼油副产物和裂解/聚合物作为塑料与电子外壳的上游，而不是把塑料直接凭空生成。
- 半导体链条仍按“晶圆制造 → 切割/测试 → 封装 → 进入电路板和消费电子”的顺序抽象。
- 充电宝配方把电池包作为已完成的电池系统，暂不模拟 BMS 固件、热管理和安规认证；智能音箱暂不拆分 Wi‑Fi/蓝牙协议栈与扬声器单体参数。

## 参考资料

- U.S. EIA, Refining crude oil: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- Semiconductor Industry Association, How are semiconductors made?: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/
- U.S. Department of Energy, Battery pack components and assembly: https://www.energy.gov/documents/chapter13-electricpdf

## 后续方向

- 增加消费电子质量/良率、召回、质保和翻新分级，而不是继续只增加终端物品数量。
- 将 ABS、PC、PET 等塑料牌号与注塑、挤出、电子外壳需求关联，保留不同产品对材料的差异。
- 后续若引入智能家电，应先复用现有电池、显示、无线和装配模块，再增加产品专属零件。
