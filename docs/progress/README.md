# Capitalism Mod 系统开发进度

本目录记录各经济系统当前的实现状态。每个主日志统一分为三个部分：

1. 已实现功能
2. 存在的问题
3. 后续方向

这些日志用于说明当前代码边界和下一阶段开发计划，不替代源码、测试或正式的版本变更记录。

## 系统日志

| 系统 | 主日志 |
|---|---|
| 拍卖 | [auction.md](auction.md) |
| 银行与货币 | [bank-currency.md](bank-currency.md) |
| 企业与个人经营 | [business-company.md](business-company.md) |
| 万年历与时间 | [calendar.md](calendar.md) |
| 石化、电子与制造产业链 | [industry.md](industry.md) |
| 土地 | [land.md](land.md) |
| 贷款 | [loan.md](loan.md) |
| 市场、供应链与物流 | [market-logistics.md](market-logistics.md) |
| 股票、债券与期货 | [securities.md](securities.md) |
| 税务 | [tax.md](tax.md) |
| 界面、网络与基础设施 | [ui-network.md](ui-network.md) |
| 世界地图 | [world-map.md](world-map.md) |

## 阅读方式

- 需要了解某个系统当前能做什么时，阅读“已实现功能”。
- 需要了解尚未完成或存在风险的部分时，阅读“存在的问题”。
- 需要安排下一阶段开发时，阅读“后续方向”。

日志内容应随着实际代码和测试状态更新，避免记录与当前实现不一致的功能。

新玩法的总体架构、系统依赖和现实依据见
[`docs/design/economic-expansion-framework.md`](../design/economic-expansion-framework.md)。
