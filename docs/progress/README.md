# 系统开发进度

本文档是各经济系统日志的总索引。每个系统日志固定分为“已实现功能、存在的问题、后续方向”三部分；实际状态以源代码、测试和游戏运行结果为准。

## 系统日志

| 系统 | 文档 |
|---|---|
| 拍卖 | [auction.md](auction.md) |
| 银行与货币 | [bank-currency.md](bank-currency.md) |
| 银行信用与 NPC 银行 | [bank-credit.md](bank-credit.md) |
| 企业与个人经营 | [business-company.md](business-company.md) |
| 人口与家庭 | [population.md](population.md) |
| 时间与日历 | [calendar.md](calendar.md) |
| 工业与生产 | [industry.md](industry.md) |
| 土地 | [land.md](land.md) |
| 贷款 | [loan.md](loan.md) |
| 市场、供应链与物流 | [market-logistics.md](market-logistics.md) |
| 股票、债券与期货 | [securities.md](securities.md) |
| 税务 | [tax.md](tax.md) |
| 金融风险 | [financial.md](financial.md) |
| 动态经济事件 | [economic-events.md](economic-events.md) |
| 政府与宏观政策 | [government.md](government.md) |
| 界面、网络与基础设施 | [ui-network.md](ui-network.md) |
| 世界地图 | [world-map.md](world-map.md) |

## 阶段路线

1. 基础可靠性：编码、主体与来源 ID、持久化、跨系统结算、恢复和审计。
2. 居民经济闭环：NPC 家庭收入、消费、就业、迁移、银行账户和信用风险。
3. 合同与市场联动：履约、仓储、运输、价格、付款、损失和违约处置。
4. 城市与区域经济：住房、公共设施、维护成本、服务质量和区域差异。
5. 政府与宏观经济：税收、财政、政策利率、债券、流动性和风险传导。
6. 最终可靠性验收：跨账本恢复、离线账户、审计规则和完整回归测试。

## 开发与验收规则

- 每个跨系统操作必须有稳定主体 ID、来源或请求 ID、货币、金额、时间和状态。
- 重复调用不得重复扣款、发放、计税、交付或增加债务。
- 持久化顺序必须允许重启后继续完成，或进入明确的退款、损失、违约或坏账状态。
- 新玩法必须同时提交实现、持久化、测试和对应系统日志。
- `/economy-audit` 用于发现账本、结算、合同、市场、居民、政府和金融数据的不变量异常。
- 经济参数应有现实含义和边界说明，但不直接复制现实国家的完整法律制度。
