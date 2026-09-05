# 石化烯烃与聚合物链 - 2026-09-06

## 本次实现

- 新增乙烯 `ethylene` 与丙烯 `propylene` 两种石化中间品。
- 新增蒸汽裂解装置 `steam_cracker`：石脑油 → 乙烯 + 丙烯。
- 新增两条聚合路线：乙烯 → 塑料粒料、丙烯 → 塑料粒料。
- 保留原有“石脑油 + 煤 → 塑料粒料”配方，兼容已经存在的企业配置和存档。
- 乙烯、丙烯加入商品目录，可被采购、库存和市场系统使用。

## 现实依据

炼油过程会产生石脑油等中间馏分；石脑油可以进一步进入蒸汽裂解，生产乙烯、丙烯等基础化工原料。乙烯和丙烯再通过聚合分别对应聚乙烯、聚丙烯等树脂，树脂通常以粒料形式交给下游注塑、挤出和装配企业。本阶段将这些工序抽象成可替代的生产配方，不模拟真实工厂的完整催化剂、温压、分离塔和副产物平衡。

参考资料：

- [U.S. Energy Information Administration：Refining crude oil](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)
- [U.S. EPA：Plastics molding and forming](https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines)

## 后续方向

- 将乙烯、丙烯的裂解收率和燃料副产物纳入批次结算。
- 进一步增加柴油、汽油、液化石油气等炼厂产品，但要先设计联产库存与需求，避免单一配方无限产出。
- 将不同塑料树脂类型接入注塑件规格、质量和回收流程。
