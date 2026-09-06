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
| 人口与家庭 | [population.md](population.md) |
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

## 阶段路线（当前状态）

- 阶段一：已完成——修复文档编码、统一资金边界，并建立跨系统幂等结算测试。
- 阶段二：已完成基础闭环——NPC 家庭收入、消费、就业、年龄队列和带成本迁移已接入持久化日结。
- 阶段三：已完成基础闭环——合同索引、货运交付、库存、付款、损失和违约已可重试。
- 阶段四：已完成基础闭环——市场需求、区域设施、住房拥挤租金和公共服务已接入经济结算。
- 阶段五：已完成基础闭环——税收、财政、政策利率、债券偿付、银行流动性和金融风险传导已接入；后续进入精细化阶段。
- 阶段六：进行中——已完成债券发行批次恢复、银行到账恢复、逾期率/存贷比/挤兑压力触发、连续三日危机恢复、全局日结阶段日志、持久化银行权益账本、手续费收入、逾期损失准备、坏账核销、政府资本补充收据、公开收购、债券兑付和分红托管阶段日志；完整金融托管状态机仍待收敛。

阶段验收以源码、测试和运行日志为准；“已完成基础闭环”不代表现实细节已经完整。

总体框架见 [economic-expansion-framework.md](../design/economic-expansion-framework.md)。
