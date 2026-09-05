# 炼油燃料联产品 - 2026-09-06

## 本次实现

- 新增柴油 `diesel`、汽油 `gasoline` 和液化石油气 `lpg`。
- 炼油配方现在以原油为输入，同时产出石脑油、燃料油、柴油、汽油和液化石油气。
- 三种产品加入商品目录，可进入企业库存、供应报价和商品市场。
- 旧世界和自定义配置仍保留原有配方；新的默认配置使用完整联产配方。

## 现实依据

现实炼厂并非只产出单一燃料。原油蒸馏和后续转化会产生汽油组分、柴油等馏分以及液化炼厂气；石脑油则继续作为石化原料。本阶段先用一个联产配方表达共同生产关系，尚未模拟不同原油品质、复杂设备组合、实际收率平衡和产品调和。

参考资料：

- [EIA：Refining crude oil—inputs and outputs](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-inputs-and-outputs.php)
- [EIA：The refining process](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)

## 后续方向

- 让柴油、汽油和液化石油气分别进入运输、发电、工业和居民能源需求。
- 按炼厂设备和原油类型调整联产比例，并处理副产品库存积压。
- 增加燃料质量规格、调和与储罐容量，避免燃料只作为静态商品存在。
