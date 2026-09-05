# 系统开发进度日志

本目录记录 Capitalism Mod 各个主要系统的开发状态。每个系统单独维护一个日志，内容分为：

- 当前已经实现的功能
- 当前边界与暂未覆盖的内容
- 后续开发方向
- 依赖关系与测试备注

更新时间以 `YYYY-MM-DD` 记录。完成某个系统的代码修改后，应同步更新对应日志；如果修改影响多个系统，也要更新相关日志。

## 日志索引

| 系统 | 日志 |
|---|---|
| 土地系统 | [land.md](land.md) |
| 世界地图 | [world-map.md](world-map.md) |
| 税务系统 | [tax.md](tax.md) |
| 企业与个人经营 | [business-company.md](business-company.md) |
| 市场、供应链与物流 | [market-logistics.md](market-logistics.md) |
| 银行与货币 | [bank-currency.md](bank-currency.md) |
| 股票、债券与期货 | [securities.md](securities.md) |
| 贷款 | [loan.md](loan.md) |
| 拍卖 | [auction.md](auction.md) |
| 万年历与时间 | [calendar.md](calendar.md) |
| 界面、网络与基础设施 | [ui-network.md](ui-network.md) |

## 更新规则

1. 新增功能后，在对应日志的“已实现功能”中补充条目。
2. 计划中的内容放入“后续开发方向”，按优先级排序。
3. 发现明确缺陷时，记录到“当前边界与待处理问题”，修复后移除或标记为已解决。
4. 不把一次性的测试现象当作长期规划；需要保留的测试结论才写入日志。
