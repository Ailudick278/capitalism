# 石化与电子消费品共用中间件 - 2026-09-06

## 本次实现

- 沿用现有石化上游：原油、炼厂馏分、乙烯/丙烯、ABS/通用塑料粒料，为电子外壳和包装提供材料。
- 新增键盘模组、存储模组、无线通信模组三个可交易的电子中间品。
- 键盘模组由 ABS 树脂、被动元件和铜线在电子装配线上生产。
- 存储模组由封装芯片、PCB 和被动元件在 SMT 线上生产。
- 无线通信模组由封装芯片、被动元件和铜线在 SMT 线上生产。
- 笔记本电脑现在需要键盘模组和存储模组；无线路由器现在需要无线通信模组。
- 三种模组复用已有设备和生产能力，没有为每种消费品创建专用机器。

## 现实依据

- 石化工业通过炼化、裂解和聚合提供塑料树脂等基础材料；ABS 常用于电子产品外壳。
- 电子消费品通常拆分为 PCB、封装芯片、存储、无线连接、输入部件和外壳，再由 EMS/整机装配完成。
- SMT 适合生产高密度电子模块；注塑/成型适合塑料外壳和键盘结构件。

## 后续方向

- 为显示屏拆分玻璃基板、背光和显示驱动，而不是把显示屏作为单一中间品。
- 增加电子产品质量等级、良率、返修和售后备件消耗。
- 将塑料、铜和电子废料回收分流，避免回收结果全部变成同一种通用材料。

## 参考资料

- U.S. EIA, Refining crude oil: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- Semiconductor Industry Association, How are semiconductors made: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/
- Intel, Silicon die to chip package: https://www.intel.com/content/www/us/en/newsroom/tech101/manufacturing/how-silicon-die-become-chip-packages.html
