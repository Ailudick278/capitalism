# Capitalism Mod

面向 Minecraft 1.21.1 NeoForge 的经济与社会模拟模组。模组将货币、银行、企业、生产、市场、物流、土地、税务、证券、劳动和人口连接为可持续运行的经济系统。

## 项目环境

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| Java | 21 |

## 当前系统

- 货币与银行：多货币钱包、兑换、转账、存款、贷款、利息和流动性风险。
- 企业与生产：企业注册、资本、库存、配方、原料、质量、工资和经营报告。
- 市场与物流：供需定价、订单、现货交易、仓储、运输、燃料、保险、损失和合同履约。
- 土地与城市：区块权属、租赁、转让、用途、租金、土地税、住房和公共服务。
- 劳动与人口：岗位、技能、雇佣合同、工资、NPC 家庭、消费、就业和区域迁移。
- 政府与宏观经济：税收、财政支出、政策利率、债券、银行流动性和风险传导。
- 金融可靠性：跨系统来源凭证、可重试结算、持久化账本和 `/economy-audit` 审计。

NPC 家庭目前是虚拟居民记录，不是世界中的实体 NPC；NPC 已拥有家庭现金流、消费、就业、迁移和独立银行账户。

## 常用命令

```text
/capitalism help
/balance
/exchange <from> <to> <amount>
/company list
/labor jobs
/labor contracts
/population info [region]
/economy-audit
/economystats
/economicevent list
/government budget [day]
```

完整命令以游戏内 `/capitalism help` 为准。

## 配置与数据

主要配置位于 `config/capitalismmod-common.toml` 和 `config/capitalismmod/*.json`。世界级经济数据使用 NeoForge `SavedData` 持久化。跨系统操作使用稳定的主体 ID、来源 ID、请求 ID、货币、金额、时间和状态，以支持重启恢复和审计。

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

开发按阶段推进：先保证账本、结算、持久化、恢复和测试可靠，再扩展居民、城市、政府和宏观经济玩法。每项功能都应具备可运行闭环、恢复路径、测试和对应文档。

## 免责声明

本模组中的货币、税务、金融和经济数据仅用于游戏内模拟，不构成现实世界的投资、税务、法律或金融建议。
