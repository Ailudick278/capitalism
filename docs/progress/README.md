# 系统开发进度

本文记录各经济系统当前的真实实现状态。每个系统日志固定分为三部分：

1. 已实现功能
2. 存在的问题
3. 后续方向

## 系统日志

| 系统 | 日志 |
|---|---|
| 拍卖 | [auction.md](auction.md) |
| 银行与货币 | [bank-currency.md](bank-currency.md) |
| 企业与个人经营 | [business-company.md](business-company.md) |
| 时间与日历 | [calendar.md](calendar.md) |
| 工业与生产 | [industry.md](industry.md) |
| 土地 | [land.md](land.md) |
| 贷款 | [loan.md](loan.md) |
| 市场、供应链与物流 | [market-logistics.md](market-logistics.md) |
| 股票、债券与期货 | [securities.md](securities.md) |
| 税务 | [tax.md](tax.md) |
| 界面、网络与基础设施 | [ui-network.md](ui-network.md) |
| 世界地图 | [world-map.md](world-map.md) |

## 阅读规则

- 以源代码和测试结果为准，日志只做状态说明。
- “已实现功能”只记录当前可运行的功能。
- “存在的问题”记录会影响真实性、稳定性或扩展性的缺口。
- “后续方向”按依赖关系排序，不代表全部同时开发。

## 阶段路线

- 阶段一：修复编码、统一账本边界、补充跨系统结算测试。
- 阶段二：实现 NPC 人口、家庭收入、消费、就业和迁移。
- 阶段三：统一合同履约、订单、库存、物流和违约处理。
- 阶段四：完善动态价格、城市公共设施和区域经济。
- 阶段五：加入政府政策、宏观经济事件和金融风险传导。

总体框架见 [economic-expansion-framework.md](../design/economic-expansion-framework.md)。
