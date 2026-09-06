# Capitalism Mod

面向 Minecraft 1.21.1 NeoForge 的经济与社会模拟模组。模组把货币、银行、企业、生产、市场、物流、土地、税务、证券、劳动和人口连接成可持续运行的经济系统。

## 项目环境

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| Java | 21 |

## 当前功能

- 货币与银行：多货币钱包、兑换、转账、存款、贷款、利息和流动性压力。
- 企业与生产：企业注册、资本、金库、配方、原料、库存、质量、工资和经营报表。
- 市场与物流：供需定价、供应订单、现货交易、仓储、运输、燃料、保险、损失和合同履约。
- 土地与城市：区块权属、租赁、转让、用途、租金、土地税、住房和公共服务。
- 劳动与人口：岗位、技能、雇佣合同、工资、NPC 家庭、基本消费、就业和区域迁移。
- 金融市场：股票、债券、期货、企业贷款、分红、公开收购和金融风险指标。
- 政府与税务：税收、财政支出、政策利率、债券、银行流动性与宏观风险传导。

## 常用命令

```text
/capitalism help
/balance
/exchange <from> <to> <amount>
/company list
/company operations <company>
/company logistics contracts <company>
/labor profile
/labor jobs
/labor contracts
/population info [region]
/city housing terminate <household>
/economy-audit
/economicevent priceShock <eventId> <itemId> <shockBps> <days>
/economicevent logisticsShock <eventId> <origin> <destination> <capacityBps> <days>
/economicevent laborShock <eventId> <region> <demandBps> <days>
/economicevent list
/government openMarket buyBond <holdingId>
/government policy openMarket <true|false>
```

完整命令以游戏内 `/capitalism help` 为准。

## 配置与数据

主要配置位于 `config/capitalismmod-common.toml` 和 `config/capitalismmod/*.json`。世界级经济数据使用 NeoForge `SavedData` 持久化；资金、库存、税务、合同和结算记录尽量保留稳定来源 ID、状态和时间。

## 构建与测试

Windows：

```powershell
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

开发按阶段推进：先保证账本、结算、持久化和测试可靠，再扩展居民、城市、政府和宏观经济玩法。每项功能都应有可运行闭环、恢复路径、测试和文档。

## 免责声明

本模组中的货币、税务、金融和经济数据仅用于游戏内模拟，不构成现实世界的投资、税务、法律或金融建议。
