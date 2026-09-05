# 半导体封装测试链 - 2026-09-06

## 本次实现

- 新增封装芯片 `packaged_chip` 中间品。
- 新增芯片封装线 `chip_packaging_line`，消耗硅晶圆、铜线和塑料粒料，产出封装芯片。
- 新增完整可选配方 `circuit_board_with_packaged_chip`，由封装芯片继续装配电路板。
- 封装芯片加入商品目录、创造模式物品栏、模型和中英文名称。
- 旧的“硅晶圆直接装配电路板”配方保留作为兼容路径，完整路径由企业配方选择启用。

## 现实依据

现实半导体后端制造通常包括晶圆切割、裸片贴装、键合、封装和测试。完成测试的封装芯片才会交付给手机、汽车和计算机等下游电子产品厂商。本阶段把这些连续工序抽象为一条封装测试线，避免把每种键合线、引线框架和封装树脂都拆成单独物品，但保留独立的后端企业与中间品。

参考资料：

- [Semiconductor Industry Association - Stage 4: Back-end manufacturing](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)
- [Semiconductor Industry Association - Semiconductor production stages and business models](https://www.semiconductors.org/semiconductor-industry-primer/semiconductor-industry-primer-the-stages-of-production-and-business-models/)
- [U.S. Department of Energy - Semiconductor Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf)

## 当前抽象边界

- 暂不独立模拟晶圆切割、引线框架、键合线、封装树脂和电气测试仪器。
- 封装芯片暂不区分 CPU、存储器、射频芯片和电源管理芯片。
- 旧配方保留用于存档兼容；玩家可以通过 `/company recipes <公司名>` 查看配方，再用 `/company recipe <公司名> <配方ID>` 选择完整链条。

## 后续方向

- 引入封装良率和测试报废，使质量影响单位成本。
- 为手机主板增加不同芯片组合和规格档位。
- 将封装测试服务与跨企业采购、运输和质量验收衔接。
