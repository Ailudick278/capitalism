# 锂离子电池产业链 - 2026-09-06

## 本次实现

- 新增锂矿精矿、电池级碳酸锂、石墨负极材料、正极活性材料、电池隔膜和电池电解液。
- 新增电池材料处理设备 `battery_materials` 与电芯生产线 `battery_cell_line`。
- 新增基础路线：
  `石英（矿物代理） → 锂矿精矿 → 碳酸锂 → 正极材料`
  以及
  `煤/润滑剂 → 石墨负极材料`、`聚丙烯粒料 → 电池隔膜`、`碳酸锂/LPG → 电解液`。
- 新增 `lithium_ion_cell` 和 `lithium_ion_battery` 配方，分别产出电芯和电池。
- 新增电池包 `battery_pack`，由电池、电路板、钢板和塑料结构件总装。
- 新增电池回收设备 `battery_recycler`，支持“电池包 → 黑粉 → 锂电材料”的简化回收路线。
- 保留旧的 `battery_cell`、`battery` 配方，作为旧存档和简化生产路线的兼容方案。
- 新增商品价格、创造模式物品和中英文名称。

## 现实依据与抽象边界

美国能源部的锂电池供应链蓝图将上游原料、正负极粉体、隔膜、电解液、电极/电芯制造和电池包列为不同环节；石墨是锂离子电池常用负极材料，隔膜用于隔开正负极并允许离子传导。

本阶段保留 Minecraft 可玩性：没有新增矿石方块和复杂化学计量，而是用石英作为锂矿物来源代理，用现有煤、红石、铁等材料简化正极路线。后续如需要更高拟真度，可以再拆分锂、镍、钴、锰、溶剂、导电剂和粘结剂。

回收路线参考了 EPA 所描述的电池收集、拆解/粉碎、黑粉形成和后续金属回收流程；黑粉在游戏中作为中间物，之后重新生成碳酸锂、石墨负极和正极活性材料。

参考资料：

- [美国能源部：National Blueprint for Lithium Batteries 2021–2030](https://www.energy.gov/sites/default/files/2021-06/FCAB%20National%20Blueprint%20Lithium%20Batteries%200621_0_0.pdf)
- [美国能源部：Grid Energy Storage Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Energy%20Storage%20Supply%20Chain%20Report%20-%20final.pdf)
- [美国能源部：电池隔膜的功能说明](https://www.energy.gov/edf/entek)
- [美国地质调查局：石墨与锂离子电池负极](https://www.usgs.gov/data/grade-and-tonnage-data-disseminated-flake-graphite-deposits)

## 后续方向

- 为电芯增加容量、循环寿命、热失控风险和良率属性。
- 增加电池包、回收和梯次利用，连接电子产品质保与召回。
- 在不破坏旧配方的前提下，逐步增加镍、钴、锰和电解质溶剂的独立供应链。
- 将废旧电池和电子产品的回收来源从“企业自制电池包”扩展到消费端报废物，并加入运输安全与回收企业服务合同。
