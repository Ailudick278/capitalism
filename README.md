# Capitalism Mod

一个面向 Minecraft 1.21.1 的 NeoForge 经济与社会模拟模组。

模组以货币、银行、企业、市场、土地和税务为核心，采用模块化结构，方便后续继续扩展。

## 项目信息

| 项目 | 内容 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| Mod 版本 | 1.0.0 |
| Java 包名 | `com.ailudick.capitalismmod` |

## 主要系统

### 货币与银行

- 支持 USD、CNY、EUR 和 RUB。
- 支持货币兑换、汇率和最小货币单位精确存储。
- 支持借记账户、信用账户、活期存款和定期存款。
- 支持存款、取款、转账、贷款、还款和利息结算。

### 企业与产业链

- 注册企业并选择经营范围。
- 通过产业配置驱动生产、投入品消耗和产出。
- 支持企业升级、企业金库、企业收购和同类企业合并。
- 企业收入、采购成本和升级费用可以进入企业所得税计算。

### 市场与供应链

- B2B 供应市场支持报价、采购订单、预付款和补发订单。
- 商品现货市场支持限价订单和实物交割。
- 支持仓库、采购、市场订单和交易记录。
- 支持贸易区域、物流运输、运输设施和货运保险。

### 证券、债券与期货

- 支持企业 IPO、股票交易和场外股份转让。
- 支持政府债券购买、赎回和到期兑付。
- 支持期货多空交易、保证金、逐日盯市、强制平仓和到期结算。

### 土地与世界地图

- 土地系统支持区块选择、土地持有、租赁、转让和拍卖。
- 权限覆盖支持所有者、租户、信托和授权管理。
- 世界地图是独立的可视化子系统，用于查看已探索区域和选择区块。
- 土地系统通过地图入口使用世界地图，同时保留独立地图入口用于调试。

### 税务系统

税务功能集中在独立的税务系统中，当前包括：

- 土地持有税、企业利润所得税和个人经营所得税。
- 增值税、土地转让税、证券印花税和资本利得税等税种框架。
- 企业和个人经营的收入凭证、采购成本凭证和税务周期汇总。
- 个人经营按“收入 - 合理采购成本”计算应税所得。
- 税务申报、缴税、税单、税务局、税收抵扣和退税流程。
- 税期锁定、管理员更正、补税、抵扣和审计日志。
- 个人经营更正申请支持提交、管理员批准或驳回。

## 常用命令

| 命令 | 用途 |
|---|---|
| `/capitalism help` | 查看模组帮助 |
| `/balance` | 查看货币余额 |
| `/exchange <from> <to> <amount>` | 兑换货币 |
| `/fx` | 查看汇率 |
| `/company list` | 查看企业 |
| `/business info` | 查看个人经营信息 |
| `/business tax` | 查看个人经营税务明细 |
| `/business declare <billId>` | 申报个人经营税单 |
| `/business paytax` | 缴纳个人经营税款 |
| `/taxrule list` | 查看税率规则 |
| `/taxexpenses` | 查看费用凭证 |
| `/taxtransactions` | 查看交易税记录 |
| `/taxcorrection history` | 查看税务更正历史 |

提交个人经营更正申请：

```text
/taxcorrection_request individual <businessId> <periodEnd> <revenue> <expenses> <reason>
```

管理员审核：

```text
/taxcorrection review <申请ID> approve <审核意见>
/taxcorrection review <申请ID> reject <驳回原因>
```

## 配置

主要配置文件：

```text
config/capitalismmod-common.toml
```

产业、商品和其他可扩展数据位于：

```text
config/capitalismmod/*.json
```

世界地图支持配置缩放范围和区块加载半径。加载半径最大值为 24，默认值为 8。

## 构建

Windows：

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-user')
.\gradlew.bat build
```

Linux/macOS：

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

## 目录结构

```text
src/main/java/com/ailudick/capitalismmod/
├── bank/        银行账户与金融操作
├── business/    个人经营系统
├── company/     企业与产业链
├── currency/    货币与汇率
├── land/        土地与区块逻辑
├── market/      商品市场、仓库与物流
├── tax/         统一税务系统
├── stock/       股票市场
├── supply/      B2B 供应市场
├── network/     客户端与服务端同步
└── screen/      游戏界面
```

## 免责声明

本模组中的货币、税务、金融和经济数据仅用于游戏内模拟，不构成现实世界中的投资、税务或金融建议。

## 许可

当前项目基于 NeoForge MDK 开发，许可和授权范围以项目发布者的说明为准。
