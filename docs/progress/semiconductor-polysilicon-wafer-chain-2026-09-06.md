# 多晶硅与硅晶圆上游链 - 2026-09-06

## 本次实现

- 新增高纯多晶硅中间品 `polysilicon`。
- 新增硅材料精炼设备 `silicon_refiner`，以石英和煤为输入生产多晶硅。
- 新增晶体生长炉 `crystal_growth_furnace`，以多晶硅生产硅晶圆。
- 新增商品目录、创造模式物品栏、模型和中英文名称。
- 既有直接“石英 → 硅晶圆”的配方保留为兼容配方；新增企业可选择更完整的 `silicon_wafer_from_polysilicon` 链条，避免破坏旧世界的自定义产业配置。

## 现实依据

半导体级硅不是把普通石英直接切成晶圆。现实流程通常先从含硅原料获得冶金级硅，再通过进一步提纯得到高纯多晶硅；多晶硅随后经过晶体生长形成单晶硅锭，再切片、研磨和清洗成为硅晶圆。本阶段将其中最关键的“多晶硅”和“晶体生长/晶圆”拆开，同时仍把复杂化学提纯、掺杂和抛光细节抽象掉。

参考资料：

- [U.S. Department of Energy - Solar Photovoltaic Manufacturing Basics](https://www.energy.gov/cmei/systems/solar-photovoltaic-manufacturing-basics)
- [U.S. Department of Energy - Semiconductor Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf)
- [Semiconductor Industry Association - Polysilicon and wafer production background](https://www.semiconductors.org/wp-content/uploads/2025/08/Semiconductor-Industry-Association-SIA-Comments-Polysilicon-Section-232-Investigation.pdf)

## 当前抽象边界

- 暂不区分太阳能级与半导体级多晶硅。
- 暂不模拟氯硅烷、蒸馏、掺杂、光刻、刻蚀、CMP 等化学与晶圆制造工序。
- 旧的直接晶圆配方保留用于兼容已存在的自定义配置；完整产业链通过新配方显式提供。

## 后续方向

- 增加晶圆良率、洁净室投入和封装测试环节。
- 将半导体设备供应、备件和折旧接入企业财务报表。
- 让高质量芯片或电子产品规格影响售价、退货和售后维修。
