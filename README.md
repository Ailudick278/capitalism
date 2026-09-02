# Capitalism Mod

## Recent currency asset updates

USD and CNY banknote items use separate front and back textures. Source artwork is kept in `texture/`, while packaged textures are copied to `src/main/resources/assets/capitalismmod/textures/item/`.

The banknote models are thin and centered, with different materials on their two sides. Right-clicking a currency item has no special action and does not consume the item.

After building, copy the JAR to the local test instance:

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-user')
.\gradlew.bat build -x test --no-daemon --console=plain
Copy-Item .\build\libs\capitalismmod-1.0.0.jar `
  "$env:APPDATA\.minecraft\versions\mod test\mods\capitalismmod-1.0.0.jar" -Force
```

一个面向 Minecraft 1.21.1 的 NeoForge 经济模拟模组。模组以“货币—银行—企业—市场—物流”为核心，提供玩家金融、企业经营和商品交易玩法。

## 项目信息

| 项目 | 内容 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| 模组版本 | 1.0.0 |
| Java 包名 | `com.ailudick.capitalismmod` |
| JEI | 可选软依赖 |

金额在内部使用最小货币单位保存，以避免浮点数误差。当前支持 USD、CNY、EUR 和 RUB 四种货币。

## 主要功能

### 货币与汇率

- 支持 USD、CNY、EUR、RUB，以及纸币、硬币、银行卡、信用卡和发票。
- 支持货币兑换和浮动汇率；服务器会定期尝试获取外部汇率，网络不可用时使用内置汇率。
- 汇率和更新时间会同步到客户端。
- 支付时优先使用背包中的实体货币，再使用银行账户余额。

### 银行系统

- 支持借记账户、信用账户、活期存款和定期存款。
- 支持存款、取款、转账、货币兑换和贷款。
- 银行卡使用 19 位卡号，并包含 Luhn 校验位。
- 贷款支持期限、利息和逾期罚息；跨行转账会收取手续费。
- 每个 Minecraft 日自动结算利息，并在服务器重启后补算遗漏的结算日。
- 银行界面显示个人总资产，并统一折算为 USD。

### 公司与产业链

- 通过工商局注册公司并选择行业。
- 行业生产规则由 `config/capitalismmod/industries.json` 驱动。
- 公司生产会消耗仓库中的投入品，并将产出存入仓库。
- 支持公司升级、企业所得税、维护费、公司金库、公司收购和同类公司合并。
- 支持上市公司公开收购要约，以及采矿、农业、制造、能源、交通、建筑和零售等产业链。

### 商品市场与供应链

- 玩家和公司可以发布 B2B 供应报价，采购订单支持预扣款、部分交付和后续补发。
- 商品现货市场使用限价订单簿自动撮合。
- 商品交易采用实物交割，卖方需要先将货物存入仓库。
- 支持仓库存取、采购订单和市场订单同步。

### 物流基础设施

- 跨贸易区域的订单会进入持久化物流运输流程。
- 支持物流中心、转运站和港口三类设施。
- 设施会影响运输时间、容量和货损风险，并可降低对应路线的运输风险。
- 支持运输保险；货物损失时可按申报价值赔付。

### 证券、债券与期货

- 通过证券委员会为公司进行 IPO。
- 股票支持限价单、部分成交、K 线、分红、涨跌停和场外转让。
- 股票价格会受到成交量和公司基本面的影响。
- 内置“测试公司”股票，初始价格为 100 USD，用于测试行情曲线、盘口和买卖流程。
- 支持政府债券的购买、提前赎回和到期兑付。
- 期货支持做多、做空、保证金、逐日盯市、强制平仓和到期现金结算。


### 拍卖、玩家金融与税务

- 拍卖行支持挂牌、竞价、超价退款和到期结算；无人竞价时拍品退回卖方。
- 支持玩家之间的 P2P 借贷、还款、逾期罚息和场外股票转让。
- 商店购买会生成发票，可在税务局申请报销。
- 提供财富排行榜、服务器经济统计和经济新闻播报。

## 方块与设施

| 方块 | 用途 |
|---|---|
| 银行 | 开户、存取款、贷款、转账、兑换和查看流水 |
| 工商局 | 注册公司和领取营业执照 |
| 仓库 | 存取物品，作为公司和市场的交割仓库 |
| 采购台 | 查看供应报价并下单 |
| 物流中心 | 提升区域间运输容量和效率 |
| 转运站 | 提升铁路运输容量和效率 |
| 港口 | 提升海运容量和效率 |
| 商品交易所 | 商品现货交易 |
| 期货交易所 | 商品期货交易 |
| 证券交易所 | 股票交易和行情查看 |
| 债券市场 | 购买、赎回和兑付政府债券 |
| 拍卖行 | 挂拍和竞价 |
| 证券委员会 | 公司 IPO |
| 税务局 | 缴纳企业所得税和报销发票 |

## 常用命令

| 命令 | 用途 |
|---|---|
| `/capitalism help` | 查看帮助 |
| `/balance` | 查看各货币余额 |
| `/pay <玩家> <货币> <金额>` | 向玩家转账 |
| `/exchange <from> <to> <金额>` | 兑换货币 |
| `/fx` | 查看汇率 |
| `/company list` | 查看公司 |
| `/company upgrade <公司>` | 升级公司 |
| `/company withdraw <公司> <货币> <金额>` | 提取公司金库资金 |
| `/company acquire <卖家> <公司> <价格>` | 发起公司收购 |
| `/company takeover list` | 查看收购报价 |
| `/company takeover accept <报价ID>` | 接受收购报价 |
| `/company takeover reject <报价ID>` | 拒绝收购报价 |
| `/company merge <来源公司> <目标公司>` | 合并同类公司 |
| `/company control <股票ID>` | 查看控股股东 |
| `/offer <公司> <商品> <价格>` | 发布 B2B 供应报价 |
| `/logistics` | 查看贸易区域和在途货物 |
| `/logistics insure <货运ID>` | 为在途货物购买保险 |
| `/shares give <玩家> <股票> <数量>` | 场外转让股票 |
| `/lend <玩家> <货币> <金额> <天数> <利率%>` | 发起 P2P 借贷 |
| `/repay <借据ID>` | 偿还借款 |
| `/loans` | 查看借贷记录 |
| `/economylog` | 查看个人经济流水 |
| `/marketorders` | 查看自己的市场订单 |
| `/markettrades` | 查看自己的成交记录 |
| `/ranking` | 查看财富排行榜 |
| `/economystats` | 查看服务器经济统计 |
| `/marketrepair scan` | 管理员扫描异常市场订单 |
| `/marketrepair fix` | 管理员修复可识别的异常订单 |

证券、债券、期货、拍卖和税务相关操作也可以通过对应的游戏界面完成。

## 配置

配置文件位于：

```text
config/capitalismmod-common.toml
```

常用配置项：

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `depositRatePerYear` | `0.05` | 活期存款年利率 |
| `loanRatePerYear` | `0.10` | 贷款年利率 |
| `termDepositRatePerYear` | `0.08` | 定期存款年利率 |
| `creditLimit` | `100000` | 信用额度，使用最小货币单位 |
| `maxDebitAccounts` | `3` | 每名玩家最多持有的借记账户数 |
| `maxCreditAccounts` | `1` | 每名玩家最多持有的信用账户数 |
| `transferFeeRate` | `0.001` | 转账手续费率 |
| `incomeTaxRate` | `0.25` | 企业所得税率 |
| `tradeRegionSize` | `512` | 贸易区域边长 |
| `regionalShippingTicks` | `12000` | 跨区域运输基础时间 |
| `logisticsRiskRate` | `0.03` | 基础货损风险 |
| `logisticsInsuranceRate` | `0.05` | 物流保险费率 |
| `commodityPriceLimit` | `0.10` | 商品涨跌停幅度 |
| `stockPriceLimit` | `0.10` | 股票涨跌停幅度 |
| `futuresMarginRate` | `0.10` | 期货保证金率 |
| `futuresExpiryDays` | `7` | 期货合约期限 |
| `bondFaceValue` | `100` | 政府债券面值 |
| `bondRatePerYear` | `0.05` | 政府债券年利率 |
| `bondMaturityDays` | `30` | 政府债券期限 |

产业链、商店商品、商品列表和抽象股票等数据可通过以下 JSON 文件配置：

```text
config/capitalismmod/*.json
```

## 开发与构建

### Windows

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-user')
.\gradlew.bat build
```

### Linux/macOS

```bash
./gradlew build
```

构建产物位于 `build/libs/`。常用开发任务：

```powershell
.\gradlew.bat runClient
.\gradlew.bat test
```

## 目录结构

```text
src/main/java/com/ailudick/capitalismmod/
├── bank/          银行账户与金融操作
├── company/       公司、产业链与收购
├── currency/      货币与汇率
├── economy/       经济数据与个人资产
├── futures/       期货
├── loan/          玩家借贷
├── market/        商品市场与物流
├── network/       客户端/服务端网络同步
├── shop/          商店报价
├── stock/         股票市场
├── supply/        B2B 供应市场
└── screen/        游戏界面
```

## 免责声明

本模组中的汇率、金融产品和经济数据仅用于游戏内模拟与玩法展示，不构成现实世界的投资、金融或其他专业建议。

## 许可证

本项目基于 NeoForge MDK 模板开发。当前许可证配置为 `All Rights Reserved`，具体授权范围以项目发布者的说明为准。
