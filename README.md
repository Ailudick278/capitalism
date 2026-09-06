# Capitalism Mod

一个面向 Minecraft 1.21.1 NeoForge 的经济与社会模拟模组。

模组围绕货币、银行、企业、产业链、市场、物流、土地、税务和金融市场展开，使用模块化结构实现可持久化、可扩展的游戏经济系统。

## 项目信息

| 项目 | 内容 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Mod ID | `capitalismmod` |
| Mod 版本 | 1.0.0 |
| Java 包名 | `com.ailudick.capitalismmod` |
| Java 版本 | 21 |

## 主要功能

### 货币与银行

- 支持 USD、CNY、EUR、RUB 等货币。
- 支持钱包、现金支付、找零、货币兑换和汇率查询。
- 支持借记账户、信用账户、活期存款、定期存款、转账、贷款和利息结算。

### 企业与产业链

- 支持企业注册、经营范围、注册资本、企业金库和经营场所。
- 支持配置驱动的生产配方、原料消耗、产品产出和生产批次。
- 支持库存成本、质量筛查、放行、拒收、返工、工资、折旧和经营报表基础。
- 支持石化、电子、半导体、电池、PCB、木材和家具等产业链。

### 市场与物流

- 支持 B2B 供应市场、报价、采购订单、预付款和补发订单。
- 支持商品现货市场、限价单、撮合、实物交割和交易记录。
- 支持仓库、贸易区域、运输方式、物流节点、燃料、保险、损失和赔付。

### 土地与世界地图

- 支持区块认领、释放、租赁、转让、拍卖、土地用途和权限管理。
- 支持租金、保证金、欠租、土地税、土地估值和产权历史。
- 世界地图支持已探索区块、土地覆盖层、资源、企业地点和物流节点展示。

### 税务与金融市场

- 支持企业所得税、个人经营所得税、土地税、增值税、印花税和资本利得税等税种框架。
- 支持税单、申报、缴税、欠税、退税、抵扣、税务更正和审计记录。
- 支持企业 IPO、股票、债券、期货、保证金、每日盯市和到期结算。

## 常用命令

```text
/capitalism help                 查看模组帮助
/balance                         查看货币余额
/exchange <from> <to> <amount>   兑换货币
/fx                              查看汇率
/company list                    查看企业
/business info                  查看个人经营信息
/business tax                   查看个人经营税务
/taxrule list                   查看税率规则
/taxexpenses                    查看费用凭证
/taxtransactions                查看交易税记录
/taxcorrection history          查看税务更正历史
```

## 配置与数据

主要配置文件：

```text
config/capitalismmod-common.toml
```

产业、商品和其他可扩展数据位于：

```text
config/capitalismmod/*.json
```

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

构建产物位于 `build/libs/`。构建任务会将生成的模组 JAR 复制到本地 Minecraft 测试实例；如需修改目标目录，可使用 `-PmodTestModsDir=<path>`。

运行测试：

```powershell
.\gradlew.bat test
```

## 目录结构

```text
src/main/java/com/ailudick/capitalismmod/
├─ bank/       银行账户与金融操作
├─ business/   个人经营系统
├─ company/    企业与生产系统
├─ currency/   货币与汇率
├─ land/       土地与区块逻辑
├─ market/     商品市场、仓库与物流
├─ tax/        统一税务系统
├─ stock/      股票市场
├─ supply/     B2B 供应市场
├─ loan/       贷款系统
├─ bond/       债券系统
├─ futures/    期货系统
├─ network/    客户端与服务端同步
└─ screen/     游戏界面
```

开发进度见 [`docs/progress/`](docs/progress/README.md)。

## 免责声明

本模组中的货币、税务、金融和经济数据仅用于游戏内模拟，不构成现实世界中的投资、税务、法律或金融建议。

## 许可

项目基于 NeoForge MDK 开发。具体许可和授权范围以项目发布者的说明为准。
