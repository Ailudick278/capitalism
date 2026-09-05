# 半导体后段封装链 - 2026-09-06

## 本次实现

- 新增硅裸片、测试裸片、引线框架和封装树脂四个中间商品。
- 晶圆经 `wafer_dicing_saw` 切割为硅裸片。
- 硅裸片经 `chip_testing_station` 进行测试，形成可封装的测试裸片。
- 铜箔与钢片经 `stamping_press` 制成引线框架。
- 环氧树脂与塑料颗粒经 `molding_compound_unit` 制成封装树脂。
- 测试裸片、引线框架、铜线和封装树脂经 `chip_packaging_line` 制成封装芯片。
- 保留原有晶圆直接封装配方，兼容旧存档和旧自定义配方。

## 现实依据

半导体后段通常包含晶圆切割、芯片装配、键合、封装和测试；引线框架、键合线和封装材料共同构成封装结构。本阶段参考 [Semiconductor Industry Association 后段制造流程](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)。

## 当前抽象边界

- 暂未拆分晶圆良率、缺陷等级、键合方式和电气测试项目。
- 引线框架暂用铜箔与钢片代表铜合金/金属冲压过程。
- 设备名称表达独立工序，但仍沿用现有企业配方系统，不额外建立工厂图布局。

## 后续方向

- 让裸片测试结果影响封装芯片等级和电子产品良率。
- 增加封装类型、键合线材料和芯片测试设备维护成本。
