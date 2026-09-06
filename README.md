# Capitalism Mod

面向 Minecraft 1.21.1 NeoForge 的经济与社会模拟模组。

模组围绕货币、银行、企业、生产、市场、物流、土地、税务、证券、劳动力和合同展开，目标是让玩家在一个能够持续运行的经济系统中经营企业、参与交易并承受现实中的经营风险。

## 项目信息

| 项目 | 内容 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| Java | 21 |

## 当前系统

- 货币与银行：多货币钱包、兑换、转账、存款、贷款和利息结算。
- 企业与生产：企业注册、金库、生产配方、原料、库存、质量、工资和经营报表。
- 市场与物流：供应订单、现货交易、仓库、运输、燃料、保险、损失和结算。
- 土地与税务：区块、租赁、转让、用途、租金、土地税和税务记录。
- 金融市场：股票、债券、期货、企业贷款和风险基础。
- 劳动力与合同：岗位发布、技能匹配、雇佣、工资、欠薪，以及货运合同的履约状态。

## 常用命令

```text
/capitalism help                 查看模组帮助
/balance                         查看货币余额
/exchange <from> <to> <amount>   兑换货币
/company list                    查看企业
/company operations <company>   查看企业经营指标
/company logistics contracts <company>  查看货运合同
/labor profile                   查看劳动档案
/labor skill <skill> <value>     更新玩家技能
/labor post <company> <role> <dailyWageMinor> 发布岗位
/labor jobs                      查看开放岗位
/labor hire <offerId> <worker>   录用玩家
/labor contracts                 查看雇佣合同
/labor end <employmentId>        结束雇佣
```

## 配置与数据

主要配置位于：

```text
config/capitalismmod-common.toml
config/capitalismmod/*.json
```

世界经济数据使用 NeoForge `SavedData` 持久化。涉及资金、库存、税务和合同的操作都会尽量保留来源、状态和结算记录。

## 构建与测试

Windows：

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-user')
.\gradlew.bat build
.\gradlew.bat test
```

Linux/macOS：

```bash
./gradlew build
./gradlew test
```

构建产物位于 `build/libs/`。

## 开发文档

- [系统开发进度](docs/progress/README.md)
- [经济扩展总框架](docs/design/economic-expansion-framework.md)

开发采用分阶段策略：先保证账本、结算、持久化和测试可靠，再扩展 NPC 人口、消费、城市、政府和宏观经济事件。

## 免责声明

本模组中的货币、税务、金融和经济数据仅用于游戏内模拟，不构成现实世界中的投资、税务、法律或金融建议。
