# Capitalism Mod - business-company progress log

Consolidated on: 2026-09-06

## Current system log

# 企业与个人经营系统开发进度

最后更新：2026-09-06

## 已实现功能

- 支持企业注册、企业类型、产业范围和行业配置。
- 支持产业配置驱动的生产投入、产出和企业经济数据。
- 支持企业注册资本、企业金库、收购、同类企业合并和集团/联合体管理；企业规模由经营指标体现，不设简单等级。
- `/company credit <名称>` 提供只读授信报告，公开现金流、债务、偿债覆盖率和剩余债务空间。
- 支持 IPO、企业股权和企业相关的经济事件基础结构。
- 支持个人经营注册、经营范围、订单、经营账本和经营数据持久化。
- 支持个人经营收入、采购成本和经营所得税数据接入税务系统。
- 支持行业目录和可扩展的经营范围目录。
- 企业供应市场收入和业主提款现在写入独立持久化企业账本，可通过 `/company ledger <名称>` 查询。
- 企业通过供应市场完成销售收款时，会同步生成企业所得税收入凭证，避免企业收入只增加金库而未进入税务系统。
- 已建立可持久化的企业生产调度器：按周期检查原料、原子化消耗投入、生成成品并自动尝试履行待补发订单；生产失败时企业停产，不会积累可被一次性刷出的无限产能。
- 生产系统设计依据和现实约束已记录在 `industry-production-research-2026-09-05.md`，后续将按劳动合同、机器维护和完整存货成本推进。
- 已建立生产批次质量筛查、库存冻结、人工放行、拒收报废和返工流程；放行批次才会进入市场供给和待交付订单履约。
- `/company quality` 显示质量台账、仓库总量、冻结量和可用量；返工费用比例与质量提升值可配置。
- 已增加石化基础油调和链，以及手机、电视、笔记本、电源适配器和无线路由器等电子消费品链。

## 当前边界与待处理问题

- 自动生产第一阶段已启用；人工、设备维护、产能上限、质量良率和订单优先级仍需继续深化。
- 企业治理、股东权利、破产清算和集团内部交易尚未完整展开。
- 个人经营与企业之间的差异主要体现在数据结构和税务入口，经营体验仍可进一步区分。

## 后续开发方向

1. 完善企业生命周期：设立、经营、融资、停业、破产和清算。
2. 建立企业会计周期和资产负债、利润、现金流三类报表。
3. 将企业生产与供应链、仓储、物流和市场订单完整串联。
4. 完善企业股权、收购、合并和集团内部交易规则。
5. 增加个人经营的许可证、雇工、成本扣除和风险等级。

## Consolidated historical entries

### company-balance-sheet-2026-09-05.md

# 企业资产负债表工资负债（2026-09-05）

企业财务报表现在将持久化欠薪纳入 `payrollLiabilities` 和总负债。`/company statement <名称>` 会同时显示税款、贷款和工资负债，避免欠薪企业被误判为无负债。企业税款负债直接读取统一税务账簿，并保留旧 `taxOwed` 镜像作为存档兼容兜底；工资负债仍按清算优先顺序处理。

### company-cash-flow-lending-2026-09-05.md

# Company cash-flow lending

## Implemented

- Company loan underwriting now reads the existing company ledger for the latest 90 game days.
- Operating cash flow is separated from financing and investment flows such as loan proceeds, capital contributions, dividends, owner withdrawals and equipment purchases.
- Companies with recorded operating activity must have positive cash flow and keep total debt within three times the recent operating cash flow.
- Companies without operating history retain the existing registered-capital rule, so newly created companies are not permanently unable to obtain startup financing.
- No new persistent fields were added; old worlds use the same ledger and loan saves.

## Design basis

This is a game-scale approximation of cash-flow underwriting and debt-service coverage. Real lenders assess expected cash flow, repayment capacity, loan structure and collateral rather than relying on registered capital alone.

## Next direction

- Add scheduled amortization and a debt-service ratio that considers each loan's term and interest.
- Add collateral and default recovery only after the company asset valuation is reliable.

### company-cogs-source-idempotency-2026-09-06.md

# 企业销货成本来源幂等（2026-09-06）

## 本次完成

- 销货成本会计流水增加 `inventory_cogs:<sourceId>` 来源标记。
- 重试确认同一批库存销售时，先检查企业会计流水，避免重复扣减成本层和重复追加 COGS 流水。
- 企业所得税费用记录继续使用同一来源 ID，保持成本会计与税务费用的对应关系。

## 当前边界

- 库存、仓库发货、物流运输单和销售收款仍是多个持久化对象；本次只收紧了销货成本确认，不等同于跨对象原子事务。

### company-collateral-assessment-2026-09-06.md

# 企业抵押物评估基础 — 2026-09-06

## 本次实现

- 新增只读 `CompanyCollateralAssessment`，从企业现有资产负债表计算库存和设备的保守可抵押价值。
- 库存按 50% 折价、设备按 60% 折价，作为游戏内授信评估代理值。
- 新增 `/company collateral <名称>` 报告，显示资产原值、可计入抵押物价值、现有贷款负债和参考授信空间。
- 当前贷款审批和贷款合同保持不变，报告不会自动增加可借金额。
- 评估现在扣除质检冻结/拒收库存的市场价值，并在报告中显示 `restricted` 扣减项；受限库存不会计入参考授信空间。

## 现实逻辑依据

商业银行通常不会按存货和设备账面价值全额放贷，而会考虑变现折价、资产流动性和已有债务。本阶段先建立透明的资产评估层，后续再决定是否引入抵押登记、LTV、违约处置和优先受偿规则。

## 后续方向

- 将抵押物登记、冻结、释放和贷款关联关系持久化。
- 在抵押贷款产品中接入 LTV、优先受偿和违约处置流程。
- 将设备状态、库存质量和站点位置纳入抵押价值调整。

### company-collateral-haircut-config-2026-09-06.md

# 企业抵押品折扣率配置化 - 2026-09-06

## 本次实现

- 企业库存抵押品折扣率从硬编码改为配置项，默认 50%。
- 企业设备抵押品折扣率从硬编码改为配置项，默认 60%。
- 两项配置均限制在 0% 到 100%，可由服务器按资产风险政策调整。
- 质量冻结库存仍会先从可抵押库存中扣除，再应用折扣率。
- 设备残值仍先按设备状态计算，再应用设备折扣率。

## 现实依据

现实贷款承销会根据抵押品类别、流动性、质量和处置风险设置贷款价值比（LTV）或折扣率；库存和设备通常不会按账面/市场价值全额计入可贷额度。

## 当前边界

- 当前仍按库存和设备两大类统一折扣，尚未按商品流动性、账龄、设备型号分别定价。
- 抵押品处置、评估复核和贷款违约后的强制变现流程仍待完善。

### company-credit-repayment-behavior-2026-09-06.md

# 企业信用还款行为（2026-09-06）

## 本次实现

- 还款记录新增还款时的剩余天数和是否逾期标记，旧记录缺失字段时兼容为未知/非逾期。
- 新增 `CompanyCreditBehavior`，统计还款次数、按时还款次数、逾期次数、本金和利息累计额。
- `/company credit <公司名>` 现在显示还款行为和 0-100 的报告分数。
- 行为分数只作为信用报告信息，不直接改变现有授信审批门槛，避免未经测试的隐性惩罚。

## 现实逻辑对照

金融机构通常会同时查看当前逾期状态和历史还款行为；只看当前余额无法反映持续履约能力。本阶段先建立可解释的行为指标，后续若需要再把它接入授信定价或抵押贷款审批。

## 当前边界

- 旧存档中没有还款时点信息的记录不会被追溯推断为逾期。
- 分数是游戏内报告指标，不等同于现实征信机构的信用评分。

### company-credit-report-2026-09-05.md

# 企业授信报告 - 2026-09-05

## 已实现

- 新增只读 `CompanyCreditSnapshot`，复用企业贷款审批已经使用的现金流、资本上限、现有债务和偿债覆盖率。
- 新增 `/company credit <名称>`，显示近 90 个游戏日经营现金流、现有债务、资本/现金流债务上限、剩余债务空间、年化偿债额、覆盖率和逾期状态。
- 报告不新增存档字段、不自动放款，也不改变现有贷款审批规则。

## 现实逻辑

商业授信不能只看注册资本；还需要看可持续现金流、现有债务和偿债能力。报告把这些因素分开显示，避免玩家把“注册资本”误解成可直接支出的现金或固定贷款额度。

## 后续方向

- 在抵押物估值可靠后，再增加可选抵押贷款和贷款价值比（LTV）限制。
- 在明确分期计划和违约处置规则后，再加入自动还款、重组和资产处置；当前不自动改变企业资产。

### company-debt-service-2026-09-05.md

# Company debt service underwriting

## Implemented

- Company loan approval now considers loan term and annual interest, not only total debt and recent cash flow.
- Existing and proposed loans are converted to a conservative annual debt-service estimate.
- Companies with operating history need an estimated debt-service coverage ratio of at least 1.25.
- The calculation treats current company loans as balloon-style obligations, matching the current repayment model without changing saved loan records.
- New companies without operating history retain the startup-financing path and remain constrained by registered capital.

## Design basis

Debt-service coverage compares cash available for debt service with required debt service. The 1.25 threshold is a game rule inspired by common commercial underwriting practice, not a universal legal requirement.

## Next direction

- Replace the conservative balloon estimate with explicit amortization schedules when the loan UI supports installment payments.
- Add collateral valuation and loan-to-value limits after company equipment and warehouse valuation are made more comprehensive.

### company-employer-labor-cost-2026-09-05.md

# 雇主用工成本（2026-09-05）

## 已实现

- 新增 `companyEmployerPayrollRate` 配置项，默认值为 `0.10`，范围为 `0.0–1.0`。
- 企业每日结算分别计算工资总额、雇主承担的用工附加成本和合计劳动成本。
- 资金不足时，合计劳动成本形成持久化欠付负债；员工工资结算和清算优先级保持不变。
- `/company metrics` 同时显示工资总额、雇主附加成本和总用工成本。

## 设计依据

国际劳工组织将劳动成本定义为雇主为使用劳动力承担的总成本，除工资外还包括雇主社会保障支出、福利、培训和相关税费。由于这些比例具有司法辖区差异，本模组采用服务器可配置的抽象比例，而不是假定某一国家的具体费率。

参考：[ILO labour cost concepts and definitions](https://ilostat.ilo.org/methods/concepts-and-definitions/description-prices-indicators/)。

## 后续方向

- 将附加成本拆分为社会保险、福利、培训和雇佣税等可选科目。
- 增加工资单和雇主缴费的独立账单，便于税务系统分别处理。

### company-equipment-depreciation-2026-09-06.md

# 企业设备折旧 - 2026-09-06

## 本次变更

- 设备每完成一个生产批次都会根据耐久度下降确认一笔非现金折旧。
- 折旧额采用生产使用量代理：设备账面价值从使用前状态到使用后状态的差额，避免整数舍入导致设备寿命结束时账面价值仍残留。
- 折旧计入本批次产品的转换成本；产品留在库存时进入存货成本，销售或供应订单消耗时再通过销售成本结转。
- 设备账面价值仍由设备购置成本与当前耐久度计算，维护费用继续单独作为现金经营支出。
- 经营利润会包含折旧，经营现金流不会把折旧当作现金流出。

## 现实依据

IAS 16 将符合条件的生产设备作为不动产、厂房和设备确认，并要求将可折旧金额按系统方法在使用寿命内分摊；折旧方法可以反映预期产出或使用量。维护和修理不能替代折旧。

参考：

- https://www.ifrs.org/issued-standards/list-of-standards/ias-16-property-plant-and-equipment/
- https://www.ifrs.org/content/dam/ifrs/publications/pdf-standards/english/2022/issued/part-a/ias-16-property-plant-and-equipment.pdf

## 当前边界

- 游戏中设备耐久度被用作简化的使用量/技术状态代理，尚未单独建模残值、减值测试、处置收益和不同折旧方法。
- 设备购买仍由企业直接安装并消耗材料，后续可以继续接入设备制造商和二手设备交易。

### company-equipment-disposal-2026-09-06.md

# 企业设备处置与固定资产损益（2026-09-06）

## 本阶段完成

- 新增 `/company machine sell <company> <machineType> <count> <price>`。
- 设备出售前按当前耐久度计算所选设备的账面价值，并从企业设备台账移除设备。
- 出售收入作为非经营性现金流入记录；账面价值作为非现金资产减少记录。
- 出售价高于账面价值时记录处置收益；低于账面价值时记录处置损失，并同步进入企业税务期间记录。
- 设备数量、当前耐久度和售价均进行校验；现金记账失败时恢复设备，避免资产凭空消失。
- 设备处置后会影响企业资产负债表中的设备资产和后续生产能力。

## 现实依据与边界

IAS 16 对固定资产处置要求终止确认，并将处置净收益或净损失计入损益；本阶段采用“当前耐久度对应账面价值”的游戏化账面模型，处置价格由企业负责人输入，作为二手交易价格的简化代理。

参考：

- https://www.ifrs.org/issued-standards/list-of-standards/ias-16-property-plant-and-equipment/

当前还没有独立的设备市场、买方企业竞价或资产处置费用，后续可在二手设备交易系统中补充。

### company-equipment-group-depreciation-2026-09-06.md

# Equipment group usage depreciation — 2026-09-06

## Implemented

- Equipment records now persist a small `usageRemainder` counter.
- A group of N identical machines receives one condition-point of wear after N production batches.
- Old saves without the field load with a zero remainder.
- Maintenance resets the remainder because the equipment has been restored to a new service interval.
- Merger, installation, removal, and repair paths preserve or normalize the counter.

## Why this is more realistic

Parallel capacity represents multiple machines performing separate batches. Applying a full group-wide wear step to every batch made a company with four machines consume four times the useful life per cycle compared with a company using one machine. The new counter treats condition as a group-level average and counts actual batch uses before applying the next wear step.

## Boundary

This remains a game-scale condition model: it does not yet track machine-by-machine failure, spare parts, planned downtime, or different maintenance schedules.

### company-freight-contracts-2026-09-06.md

# 企业运输合同 - 2026-09-06

## 本次实现

- 新增持久化运输合同记录，合同包含运输单、买方企业、承运运输企业、报价、创建时间、接受时间和状态。
- 合同新增有效期；报价命令默认 30 个游戏日，也可追加 1–365 天的期限。过期的报价或已接受合同会转为 `expired`，同一运输单随后可以重新报价。
- 买方企业所有者可发起报价：
  `/company logistics offer <买方企业名> <shipmentId> <承运运输企业ID> <报价>`。
- 报价可以在交付前依据燃料运输计划创建，也可以在交付后依据已资本化的运输应付账款创建；两种情况下都必须与运输估算金额一致，避免合同金额与资产负债表脱节。
- 承运运输企业所有者可接受报价：
  `/company logistics accept <contractId>`。
- 买方或承运方所有者可以取消尚未结算的报价/合同：
  `/company logistics cancel <contractId>`；取消后同一运输单可以重新报价。
- 可通过 `/company logistics contracts <企业名>` 查看企业参与的运输合同。
- 运输结算现在会检查活动合同：如果存在合同，必须由正确的买方和承运方签订且状态为 accepted；结算完成后合同转为 settled。
- 运输途中发生保险货损或达到最大中断次数时，活动合同会转为 `loss`，不再继续占用运输单，也不会被错误地再次结算。
- 没有合同的旧运输记录仍允许使用原有直接结算入口，保证旧存档兼容。
- 合同只有 `offered` 或 `accepted` 状态才会占用运输单；`expired`、`cancelled`、`loss` 和 `settled` 状态都允许重新报价。

## 现实逻辑

运输业务通常先形成报价、订单或服务合同，再按承运方接受和交付结果结算。本阶段先建立报价—接受—结算的最小状态机，后续可继续加入运输期限、保险、货损、违约和发票。

## 当前边界

- 报价暂时不能偏离系统估算运费；这是为了避免在实际报价模型建立前产生无法解释的存货成本差额。
- 合同接受需要承运企业所有者在线执行命令，尚未加入 NPC 或自动承运方。
- 仍未模拟车辆、车队容量、路线锁定、装货单和电子发票。

## 后续方向

- 增加合同有效期、付款期限、逾期状态和取消规则。
- 把实际燃料、保险和货损结算接入合同最终金额。
- 为承运企业增加运力和报价竞争机制。

### company-freight-insurance-deductible-2026-09-06.md

# 货运保险免赔额（2026-09-06）

## 本阶段完成

- 新增 `logisticsInsuranceDeductibleRate` 配置项，默认按申报货值的 10% 计算免赔额。
- 保险赔付改为：`min(申报货值, 实际损失) - 免赔额`，最低为 0。
- 免赔额写入持久化索赔记录，`/logistics claims` 会同时显示免赔额和最终赔付。
- 旧索赔记录没有免赔额字段时按 0 读取，保持旧世界兼容。
- 企业索赔仍进入企业非经营性保险赔偿；个人索赔仍进入玩家邮箱余额。

## 现实依据与边界

现实财产保险通常区分保险金额、损失金额、免赔额和实际赔款；免赔额可以是固定金额或保险金额的一定比例。本阶段采用可配置比例，作为游戏内统一保单规则，暂不区分运输险种、事故原因和分项免赔额。

## 后续方向

- 根据运输方式、风险区域和货物类别差异化定价。
- 增加保险公司保单、理赔审核、免赔额类型和代位追偿记录。

### company-freight-insurance-link-2026-09-06.md

# 货运保险与承运合同关联（2026-09-06）

## 本阶段完成

- 保险索赔记录新增承运企业 ID。
- 货物发生保险事故时，从该货运单当前有效的报价/接受合同中读取承运方并写入索赔记录。
- `/logistics claims` 会显示承运企业，方便买方、承运方和管理员进行货损审计。
- 旧存档中的索赔记录没有承运方字段时自动按空值读取，不影响旧世界加载。
- 货损仍会关闭对应的有效货运合同，避免承运合同在索赔完成后继续显示为履约中。

## 现实依据与边界

现实货运通常同时存在运输合同、货物保险和损失理赔记录；保险赔付不等于自动认定承运方承担全部损失，责任还可能受合同、免责条款和保险代位追偿影响。本阶段只建立可追溯关联，暂不自动向承运企业追偿，避免把保险赔付和最终责任认定混为一谈。

## 后续方向

- 增加保险费、免赔额、承运方责任上限和理赔审核状态。
- 当承运方存在重大过失时，记录保险代位追偿应收款，而不是直接扣款。

### company-freight-settlement-2026-09-06.md

# 企业运输结算 - 2026-09-06

## 本次实现

- 将已资本化的入库运输成本继续保留为企业的 freight payable，而不是在到货时直接扣现金。
- 增加承运方字段和结算时间，旧存档会默认使用“未结算”状态，保持向后兼容。
- 运输企业可以作为承运方参与结算。企业所有者可执行：
  `/company logistics settle <买方企业名> <shipmentId> <承运运输企业ID>`。
- 结算时同步完成：买方企业支付现金、承运运输企业收款、承运方营业收入与企业所得税期间记录、VAT 输出税、买方进项税额记录，以及 freight payable 关闭。
- 如果付款失败，会回滚已写入的承运方现金，避免只收款不结清应付账款。
- `/company logistics <企业名>` 现在会显示每笔运输成本是未结算，还是已经由哪个承运企业结算。

## 现实逻辑依据

现实会把为使存货到达当前地点和状态而发生的运输、装卸等直接成本计入存货成本，并把尚未付款的部分记录为应付账款。实现参考 [IFRS IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/)，但当前仍使用模组内的估算运费作为结算金额。

## 当前边界

- 结算入口是显式命令，暂不自动替玩家选择承运方，避免在没有报价、合同和运力数据时强行模拟交易。
- 当前承运方资格以企业类型 `transport` 判断；尚未加入车辆、线路容量、报价单、保险和实际燃料消耗的独立合同模型。
- 运输成本仍来自现有燃料规划估算，后续应增加承运方报价与实际结算价的差额处理。

## 后续方向

- 建立运输服务报价/接受/取消流程，把 shipment 与承运合同绑定。
- 加入运输企业的运力、车辆、路线容量和服务质量记录。
- 支持发票、付款期限、逾期应付账款、保险与货损分摊。

### company-inbound-cost-idempotency-2026-09-06.md

# 企业在途库存成本幂等（2026-09-06）

## 本次完成

- 企业采购形成的在途库存成本增加交付来源 ID。
- 供应订单恢复时，即使运输单已经创建并跳过供应商库存扣除，也不会重复增加买方库存成本层。
- 同区交付、跨区运输和一次性采购都使用对应订单/交付来源作为幂等键。
- 旧库存成本数据兼容加载，来源集合使用有限保留窗口。

## 当前边界

- 在途库存成本仍是游戏内的简化到岸成本模型；运输到站后再叠加燃料/运费成本，尚未拆分保险费、关税和不同 Incoterms 责任。

### company-inventory-cost-ledger-2026-09-06.md

# 企业存货成本账簿 - 2026-09-06

## 已实现

- 新增持久化的企业存货成本账簿，按企业和物品记录数量及总成本。
- 供应采购交付时建立成本层，生产耗用时按加权平均成本扣减。
- 没有历史成本层的旧库存按商品市场价兜底，不影响旧世界继续生产。
- 企业财务快照对有成本层的存货采用成本与市场价值孰低的简化计量。
- 机器安装/维护消耗材料时同步扣减成本层；企业合并时转移成本层。
- 运输损失和保险理赔会移除对应在途存货成本，避免损失后仍保留资产价值。
- 制造成品会分配本批次原料成本、能源/维护成本和设备周期成本，形成成品转换成本层。

## 现实逻辑

存货需要同时记录数量和金额，生产耗用不能只减少数量而没有成本结转。本阶段采用移动加权平均：新采购批次与原有库存合并为新的平均成本，耗用按平均单位成本结转。资产展示暂采用成本与可变现市场价值孰低的简化规则。

## 兼容与边界

- 旧存档没有采购批次数据，因此历史库存的未跟踪部分使用市场价估值。
- 当前工资仍按企业日结算记录，暂未按具体批次精确分配到成品。
- 尚未实现 FIFO、批次追溯和期末存货跌价准备的独立凭证。

## 后续方向

- 在销售时结转成品销售成本。
- 在企业财务界面中分开显示在库存货、在途存货、存货采购和耗用成本。

### company-inventory-sale-idempotency-2026-09-06.md

# Company inventory sale idempotency — 2026-09-06

## Implemented

- Persisted inventory-sale source IDs alongside company cost layers.
- Replayed delivery events with an already-settled source ID now skip cost-layer and quality-ledger consumption.
- Zero-cost legacy inventory sales are also marked as settled, preventing repeated quality consumption on retries.
- The new field is optional in the saved-data codec, so older worlds load with an empty source set.

## Why this matters

Supply settlement already protects payment and delivery journals from retries. Cost-of-goods recognition must use the same source identity: otherwise a crash or repeated settlement could remove the same weighted-average cost layer and quality quantity more than once, distorting profit and inventory records.

## Boundary

The source ID must be stable across retries. Existing supply delivery keys provide that identity; future sales channels should use the same pattern before calling the cost-transfer helper.

### company-landed-cost-accounting-2026-09-06.md

# 企业物流到岸成本 - 2026-09-06

## 本次实现

- 跨区域企业采购货物交付时，读取该运输单的燃料成本估算。
- 预计燃料成本会按运输单追加到企业对应商品的库存成本层，形成简化的到岸成本。
- 新增独立持久化审计记录，保存运输单、企业、商品、数量、资本化金额和交付时间。
- 库存成本层通过运输单 ID 做幂等保护，服务器重启或交付重试不会重复资本化同一笔运费。
- `/company logistics <企业名>` 现在同时显示燃料规划和已资本化的到岸成本。
- 已资本化的物流成本同时作为未结运输应付账款进入企业财务快照；`/company statement <企业名>` 会单独显示 freight payable。
- 旧存档没有企业物流成本记录时会正常读取，不会补造历史运费。

## 会计边界

当前只做“预计运费进入存货成本”和“待结算应付账款”的记录，不扣除企业现金。原因是模组目前还没有独立运输企业、报价、发票和结算主体；在这些模块建立前，直接扣款会制造没有收款方的资金流。

## 后续方向

- 建立运输企业/车队报价和服务合同。
- 交付时生成运输服务发票、应付账款和税务凭证。
- 结算运输费用后，将当前估算记录转换为实际费用，并支持保险、损失和不同承运人的价格差异。

### company-level-removal-2026-09-06.md

# 企业等级模型移除 - 2026-09-06

## 本次变更

- 清理中英文语言文件里残留的企业“升级”按钮和升级成功提示。
- 移除中文企业列表中的 `Lv.%s` 展示，改为注册资本信息。
- 企业列表继续展示企业名称、企业类型和金库余额，不再暗示存在统一等级。

## 当前企业规模逻辑

- 企业没有通用的等级、经验值或升级费用。
- 企业的实际经营能力由注册资本、现金、资产、负债、员工、设备、库存、产能、收入和现金流共同体现。
- `micro/small/medium/large` 仅是只读统计标签，用于报表参考，不影响生产、税率、贷款额度或企业权限。

## 后续方向

- 在企业报表中继续补充行业相关指标，例如产能利用率、订单履约率、库存周转和资本回报率。
- 若将来需要监管分类，应以可解释的员工、营收、资产等指标组合实现，不恢复成单一等级升级树。

### company-lifecycle-2026-09-05.md

# 企业经营状态（2026-09-05）

## 本阶段已实现

- 增加独立的企业状态台账，不改变旧 `Company` 存档结构。
- 企业默认状态为 `ACTIVE`。
- 所有者可以使用 `/company suspend <名称>` 暂停经营，使用 `/company resume <名称>` 恢复经营。
- 暂停企业会停止自动生产，并移除该企业已有的供应市场报价；暂停期间不能新增供应报价。
- `/company status <名称>` 查询状态和最近一次状态变更原因。
- 状态台账持久化保存企业 ID、状态、变更时间和原因。
- 企业所得税账单进入逾期 enforcement，或公司贷款进入逾期时，会自动暂停企业经营。
- 恢复经营前必须结清企业所得税欠款和逾期公司贷款；普通暂停也通过同一套恢复检查。
- 所有者可以使用 `/company liquidate <名称>` 进入 `LIQUIDATING` 清算状态；上市企业不能直接进入清算。
- 清算状态保留企业资产、债务和历史账本，但停止生产与供应报价，不会瞬间删除企业。
- `/company liquidate <名称> settle` 按配置的资产回收率变现库存和设备，先清偿企业税款，再清偿公司贷款；全部清偿后返还剩余权益并标记为 `DISSOLVED`。
- 若资产不足以清偿全部税款或贷款，企业继续保持 `LIQUIDATING`，不会被错误注销。

## 现实逻辑对应

企业的法律主体和经营状态不是同一个“等级”。暂停经营只影响经营活动，不删除企业、资金、账本或历史交易，符合停业/暂缓经营与注销清算的区分。

## 后续方向

- 增加更细的税务冻结、行政处罚和贷款违约分级处置。
- 在清偿债务、税款和员工工资后增加注销/清算流程。
- 将状态变化写入企业治理和审计报表，并通知企业所有者。

清算资产回收率由 `companyLiquidationRecoveryRate` 配置，默认 70%。

## 资料依据

- 《中华人民共和国企业破产法》第一百一十三条关于破产费用、职工债权、税款债权和普通破产债权顺序：
  https://www.npc.gov.cn/npc/c2/c183/c198/201905/t20190522_25968.html

### company-logistics-cost-visibility-2026-09-06.md

# 企业物流成本可见性 - 2026-09-06

## 本次实现

- 物流燃料规划记录新增采购方企业标识，跨区域采购创建运输单时会持久化企业归属。
- 新增 `/company logistics <企业名>`，企业所有者可以查看最近的燃料种类、规划用量和预计成本。
- 旧存档中的物流记录没有企业标识时仍可正常读取，只会继续显示在玩家维度的 `/logistics costs` 中。
- 本阶段仍然是成本估算和可追溯记录，不会自动扣除燃料或企业资金。

## 现实逻辑

现实企业的采购、在途库存和运输成本需要按采购主体归集，才能在入库时形成存货的到岸成本；本次先把运输规划和企业主体接通，为后续“运费资本化到存货成本、运输服务企业结算、保险和损失分摊”保留稳定接口。

## 后续方向

- 在货物交付时将实际运输服务费或燃料成本纳入企业存货成本，而不是只记录估算值。
- 增加运输企业/车队作为独立服务提供方，形成报价、结算和税务凭证。
- 在企业财务界面区分在库存货、在途存货和运输成本。

### company-logistics-insurance-2026-09-05.md

# 企业物流保险结算 - 2026-09-05

## 已实现

- 企业采购的在途货物使用 `/logistics insure <shipment>` 时，从对应企业 USD 金库扣除保险费，并记录为企业经营费用。
- 企业货物发生运输损失并触发理赔时，赔款进入企业金库，作为非经营性保险赔偿记录。
- 个人货物维持个人钱包支付保费、邮箱接收赔款的流程。
- 服务端校验运输单中的企业归属必须与玩家所有者一致，避免玩家替其他企业支付或领取赔款。

## 现实逻辑

货物保险通常由货物权益人或其企业承担保费，赔偿也应回到对应的资产主体；企业采购不能把保险成本和理赔混入所有者个人现金流。本次只修正资金归属，不改变风险概率、保额或运输规则。

## 后续方向

- 继续区分货主、承运人和保险人的责任，并增加基于运输单据的理赔审核流程。

### company-merger-ledger-continuity-2026-09-06.md

# 企业合并账簿连续性（2026-09-06）

## 本次完成

- 企业合并时迁移源公司的总账历史，并将历史记录重新归档到存续公司编号下。
- 服务生产交付记录同步迁移，保留收入、投入成本、运营成本、折旧和用工数据。
- 合并后的记录按时间排序，并遵守服务生产历史的容量限制。

## 现实逻辑对照

企业合并后，历史会计凭证和经营记录仍需由存续主体保存，不能因为原公司编号不再作为独立主体而无法查询。本实现只改变归档主体标识，不重复记账、不改变历史金额。

## 后续方向

- 为总账增加合并调整凭证，明确区分原主体历史余额与合并后的新余额。
- 将企业经营场所从单一站点扩展为多站点，以便完整承继被合并企业的生产设施。

### company-merger-logistics-continuity-2026-09-06.md

# 企业合并物流账簿连续性（2026-09-06）

## 本次完成

- 合并时将运费合同中的买方和承运方公司编号从源公司重映射到存续公司。
- 合并时将已资本化的在途物流成本中的库存所有者和承运方编号同步重映射。
- 保留合同状态、报价、期限、货运编号和成本结算状态，避免合并破坏供应链审计链。

## 现实逻辑对照

现实企业合并并不会使运输合同和已经发生的存货运输成本失效；存续主体会承继合同权利义务，历史凭证也需要能够追溯到新的会计主体。本实现只重映射主体标识，不重复结算运费或改变货物数量。

## 后续方向

- 对合并时的公开订单、保险赔付和应付账款做统一的合同承继审计。
- 在企业报表中提供合并前后物流负债的对账视图。

### company-multi-site-operations-2026-09-06.md

# 企业多经营场所（2026-09-06）

## 本次完成

- 经营场所由“每家公司一个地点”扩展为“每家公司多个区块站点”。
- 旧存档仍按原有站点记录读取，现有 `get` 调用保留默认首个站点兼容行为。
- 新注册站点按维度和区块追加保存，同一地点重复注册会去重。
- 石油井生产会在公司登记的、有商业权利且仍有储量的站点中选择可开采地点。
- 公司合并时全部经营场所随主体迁移，避免源公司站点丢失。
- 世界地图站点展示会自然包含同一公司的多个站点。
- `/company operations <名称>` 会列出全部登记站点、维度、区块和已探明油田剩余储量。
- `/company site remove <名称>` 可注销公司在当前维度/区块的经营场所登记，不改变土地权属。
- 新增 `maxCompanySites` 配置项，默认每家公司最多登记 16 个站点，避免站点记录无限增长；上限不影响土地所有权。

## 现实逻辑对照

现实企业可以同时拥有工厂、仓库、采掘地和服务网点，生产和物流不应被强制绑定到一个地点。本阶段先完成站点登记与资源选择，未把不同站点的机器、员工和库存强行拆开，避免改变现有生产结算规则。

## 后续方向

- 将机器、员工、库存和产能按站点分配。
- 增加站点关闭、转让、租赁和站点级成本核算。

### company-operating-loss-reporting-2026-09-06.md

# Company operating loss reporting — 2026-09-06

## Implemented

- Company operating snapshots now preserve negative gross profit and operating profit.
- Other operating expenses remain a non-negative subtotal after cost of sales is separated.
- Saturating subtraction protects reports from long-integer overflow.

## Accounting rationale

Financial statements must distinguish a loss from zero activity. If cost of sales exceeds revenue, gross profit is negative; if other operating costs exceed gross profit, operating profit is negative. Flooring both values to zero hid underperforming companies and made size/credit analysis less informative.

## Boundary

The snapshot is still a read-only game-scale report. It does not yet include a separate other-income/expense section, deferred tax, or comprehensive-income adjustments.

### company-operating-metrics-2026-09-05.md

# 企业经营指标快照（2026-09-05）

## 已实现

- 新增 `CompanyOperatingSnapshot`，从持久化企业账本计算指定窗口内的营业收入、营业支出和经营现金流。
- 默认窗口为最近 90 个游戏日，可通过 `/company metrics <名称> <天数>` 调整为 1–360 天。
- 同时显示员工数、每日工资、设备数量、并行生产能力、累计成功批次、失败周期、资产和权益。
- 增加只读企业规模分类：根据员工数、窗口收入年化值和资产给出 micro/small/medium/large 标签，不影响生产、税率或贷款审批。
- 贷款、资本注入、分红、所有者提款、设备购置和清算现金流不计入经营现金流，避免把融资或投资误判成经营能力。

## 设计依据

企业规模不由单一等级决定，而应结合收入、资产、员工、产能、现金流和权益等维度观察。经营现金流与融资、投资现金流分开，符合财务报表中对经营活动、投资活动和融资活动的基本区分。

## 后续方向

- 将经营指标接入企业界面，并增加按期间比较的趋势数据。
- 为收入、成本和产能增加订单来源、产品和行业维度，形成更细的管理报表。
- 后续可将规模分类接入报表筛选或监管门槛，但不得恢复成企业升级树。

## 分类依据

规模分类参考欧盟委员会 SME 定义中的员工数、营业额和资产负债表总额三项指标；本模组用 USD 游戏账本作报告代理值，不把该标签当作现实法律认定。企业属于某一档时，员工数与营业额/资产上限采用“员工数 + 任一财务指标达标”的判断方式。

参考：[European Commission - SME definition](https://single-market-economy.ec.europa.eu/smes/sme-fundamentals/sme-definition_en)

### company-overdue-credit-policy-2026-09-06.md

# 企业逾期债务与新增授信 - 2026-09-06

## 已实现

- 企业贷款申请会检查本企业的现有贷款是否逾期。
- 只要存在逾期企业贷款，自动审批路径会拒绝新增企业贷款。
- 正常贷款不会被因为其他贷款逾期而强制提前收回，玩家仍可通过还款处理现有债务。
- 企业信用报告继续显示逾期状态，并与贷款审批使用同一个判断方法，避免报告和实际审批口径不一致。

## 现实依据与边界

商业贷款授信需要持续评估借款人的偿债能力、信用风险和贷款履约情况。逾期通常会触发更严格的授信、重组或人工审查。本阶段把自动审批简化为“先处理逾期债务，再申请新贷”，避免引入尚未实现的人工审批和债务重组流程。

## 后续方向

- 增加宽限期、逾期天数分级和重组/展期申请。
- 将按时还款、逾期和重组记录纳入企业信用历史，而不只保留当前是否逾期。
- 后续接入抵押物和贷款价值比（LTV）后，再允许部分有抵押的新授信例外。

### company-payroll-2026-09-05.md

# 企业工资结算（2026-09-05）

## 本阶段已实现

- 企业劳动合同的日工资由每日结算器统一处理，不再混在单次生产成本中重复扣除。
- 资金充足时，企业每日支付工资并写入企业账本的 `payroll` 费用记录。
- 资金不足时，未支付部分形成持久化欠薪，不会被静默丢失。
- 欠薪记录包含企业、结算日、支付金额、支付前欠薪和支付后欠薪。
- 企业暂停经营期间仍保留工资义务；进入清算后，欠薪在税款和贷款之前优先处理。
- 每日企业用工成本现在由工资总额和可配置的雇主用工附加成本组成，附加成本默认按工资总额的10%计提；欠付部分与工资一起持久化为工资/用工负债。
- 企业存在欠薪时不能恢复经营；清算资金不足以支付欠薪时继续保持清算状态。

## 现实逻辑对应

劳动报酬是企业经营周期中的独立现金流义务。生产成本可以包含人工成本，但工资支付应有独立的工资结算与欠薪记录；清算时员工债权优先于税款和普通债权。

## 后续方向

- 增加工资单明细、工种工资差异和员工社保/福利成本。
- 将工资支付通知、欠薪催收和劳动合同终止补偿接入企业界面。

### company-payroll-accrual-2026-09-06.md

# 企业工资应计与支付分离（2026-09-06）

## 本次实现

- 每个结算日先确认工资和雇主承担的用工附加成本，形成当日可抵扣费用。
- 资金不足时，未支付部分继续作为工资负债保留。
- 后续补发工资只减少企业现金和欠薪负债，不再次确认工资费用或税前扣除。
- `/company metrics` 不再把原料采购付款和欠薪补发重复计算为利润费用，但仍保留它们对现金流的影响。
- `/company metrics` 补充营业利润，用毛利扣除其他经营费用，和现金流分开显示。

## 现实逻辑依据

工资通常在服务已经提供时形成期间费用和应付职工薪酬负债，实际付款是负债结算；存货采购付款则先形成存货资产，投入生产或销售时才转入费用。

## 后续方向

- 将工资附加成本拆分为社会保险、福利和培训等可配置科目。
- 为欠薪增加账龄、逾期天数、员工流失和监管处罚。

### company-payroll-merger-liability-2026-09-06.md

# 企业合并工资负债迁移（2026-09-06）

## 本次完成

- 公司合并时迁移源公司的未付工资和雇主劳动成本负债。
- 合并工资账户的结算日取两家公司最近一次结算日，避免合并当日重复计提。
- 工资支付审计记录同步改写为存续公司的公司编号，并保留原结算日、支付额和结算前后余额。
- 源公司工资账户在迁移后不再残留，避免工资负债孤立。

## 现实逻辑对照

现实中的法定合并通常由存续主体承继被合并主体的资产、债务和劳动关系；未付工资属于劳动债务，不能因为企业主体变化而消失。本实现只处理模组内部的工资负债账簿。

## 后续方向

- 将员工合同的终止补偿、工资代扣和社会保险进一步拆分成独立的工资结算项目。
- 在企业报表中展示合并前后的工资负债变动。

### company-payroll-production-gate-2026-09-06.md

# 欠薪与生产停工 - 2026-09-06

## 已实现

- 企业工资结算不足时，未支付部分会继续保留为持久化欠薪负债。
- 只要企业存在欠薪，新的自动生产周期会停止，不再继续消耗原料并产生产品或服务收入。
- 员工合同不会被自动删除，企业补足欠薪后可以通过现有经营状态继续恢复生产。
- 清算流程仍可优先支付欠薪，避免生产停工逻辑破坏既有清算优先顺序。

## 现实逻辑

工资是企业持续经营的现金流义务。企业暂时缺乏现金时可以形成应付工资，但不能假定员工会无限期无偿继续生产；停工、暂停服务或进入整顿更接近现实中的劳动和经营结果。

## 当前边界

- 当前规则采用“存在欠薪即停止新生产”的简化模型，尚未加入宽限期、部分复工、员工离职或劳动争议。
- 后续可增加工资支付通知、欠薪天数、员工流失和恢复生产所需的复工条件。

### company-production-recipe-snapshot-2026-09-06.md

# 企业生产配方快照一致性 — 2026-09-06

## 本次修复

- 生产执行阶段统一使用本次校验通过的 `ProductionRecipe` 输出快照。
- 仓库入库、质量台账、商品供给、库存成本层和供应订单履约现在使用同一组产出。
- 避免企业当前配方在执行过程中发生变化时，出现批次记录、库存和市场履约口径不一致。

## 现实逻辑对照

现实生产通常以已下达的生产工单/批次为核算边界；工单执行后，不应因为产品目录后来变化而改变该批次的产出和成本归属。本次修改将一次生产周期视为不可变的工单快照。

## 后续方向

- 为批次记录配方版本和原料批次号，进一步支持召回、质量追溯和成本审计。
- 配方热重载时增加明确的版本迁移规则，避免旧批次引用已删除的配方。

### company-production-success-rate-2026-09-06.md

# 企业生产运营成功率 - 2026-09-06

## 本次实现

- 企业经营指标新增生产运营成功率。
- 该指标由持久化的成功批次与失败周期计算，并在 `/company metrics <公司名> <天数>` 中显示。
- 成功率只统计“该生产周期是否完成至少一批生产”，不会改变生产、库存、工资或现金结算。
- 失败周期同时按原因累计，例如缺料、缺工、欠薪、设备、资金和土地/资源权限问题。
- 对历史数据、零记录和极端计数增加边界处理。

## 现实逻辑

工厂经营中需要区分设备/劳动力/原料造成的停机可用性，与产品质量良率。当前系统的失败周期可能来自缺少员工、欠薪、设备、原料或现金，因此只能作为运营成功率，而不能直接宣称是产品合格率。本阶段保持这个边界，避免把停机率和质量检验混为一谈。

## 当前抽象边界

- 失败原因按生产尝试持久化，但同一周期多个并行批次在首个失败处停止，因此原因统计表示阻塞事件，不是每个批次的完整诊断日志。
- 生产批次暂时没有逐件质量、报废数量或返工记录。
- 新指标是只读派生值，不会自动改变企业评级、价格或税务。

## 后续方向

- 给失败周期增加原因分类，例如缺料、缺工、欠薪、设备停机和资金不足。
- 为半导体封装等工序增加独立的检验良率和报废物料。
- 再将质量、返工和退货接入产品成本、售后和企业经营报表。

### company-production-traceability-2026-09-06.md

# 企业生产批次追溯 - 2026-09-06

## 本次实现

- 新增持久化生产批次记录，保存批次 ID、企业、配方、投入物、产出物、设备类型、转换成本、过程质量分数、所需工人数和游戏时间。
- 每次非服务型生产成功后写入一条记录，最多保留最近 8192 条，旧记录按先进先出清理。
- 企业合并时，历史批次记录随企业标识迁移。
- 新增只读命令 `/company batches <企业名>`，查看该企业最近 20 条生产批次。
- 该功能只增加追溯能力，不改变当前产量、价格、库存、税务或质量分数的计算规则。

## 现实依据

制造业追溯通常需要把产品/批次标识与制造事件、物料批次、设备和质量数据关联，才能支持质量调查、召回和供应链审计。本实现采用游戏化的“批次记录”作为后续不合格品、返工、召回和质保系统的数据基础。

参考资料：

- [NIST：制造数据的可追溯性与可信性建议](https://www.nist.gov/publications/recommendations-ensuring-traceability-and-trustworthiness-manufacturing-related-data-0)
- [NIST：供应链追溯制造元框架](https://csrc.nist.gov/news/2024/supply-chain-traceability-manufacturing-framework)
- [ISO 9001 documented information guidance](https://www.iso.org/files/live/sites/isoorg/files/standards/docs/en/iso_9001_2015_guidance_documented_information.pdf)

## 当前边界与后续方向

- 当前记录的是生产事件和过程质量代理值，不代表现实中的法定合格证书或检测报告。
- 目前还没有逐批次库存隔离，因此销售和运输仍按企业商品总库存处理。
- 后续可在此基础上增加不合格批次、抽检/放行、返工、召回和质保索赔，并把批次追溯延伸到上游采购和运输记录。

### company-product-quality-ledger-2026-09-06.md

# 企业产品质量台账 - 2026-09-06

## 本次实现

- 新增企业产品质量台账，按企业和商品记录生产数量与加权质量分数。
- 新生产批次根据员工平均技能和生产设备状态计算一个 0–100 的过程质量代理值。
- 企业供货销售会按销售数量比例消耗质量台账；企业合并会合并加权质量历史。
- `/company metrics <公司名> <天数>` 显示新生产库存的平均记录质量。
- 新增只读命令 `/company quality <公司名>`，按商品列出质量台账数量和平均分。
- 质量台账独立于仓库数量和存货成本层，不改变当前售价、库存数量、税务或生产产量。

## 现实逻辑

现实制造业会把生产过程、设备、人员和产品质量数据关联起来，并通过可追溯记录支持质量改进。当前实现采用游戏化的加权批次台账，先保留批次质量信息，再为后续的检验良率、退货、返工和售后服务提供数据基础。

参考资料：

- [NIST - Trustworthiness and Traceability of Supply Chain Data](https://www.nist.gov/ctl/smart-connected-systems-division/smart-connected-manufacturing-systems-group/trustworthiness-and)
- [NIST - Assessing Process Capability](https://www.itl.nist.gov/div898/handbook/ppc/section4/ppc46.htm)

## 当前抽象边界

- 质量分数是过程质量代理值，不是现实标准认证或法定合格判定。
- 尚未拆分缺陷类型、检验设备、报废品、返工和批次序列号。
- 旧存量没有质量台账，因此报表只统计新生产并被记录的批次。

## 后续方向

- 为半导体封装、显示面板和电池等工序增加检验良率和报废物料。
- 将质量分数与客户验收、退货、保修和售后维修连接。
- 继续增加按批次查看质量分布，而不是只显示当前商品平均值。

### company-quality-control-screening-2026-09-06.md

# 企业生产质量控制初版 - 2026-09-06

## 本次实现

- 每条生产批次记录自动生成一条质量控制筛查记录。
- 根据现有过程质量代理值生成三种初始状态：`released`、`conditional`、`review`。
- 新增 `/company qualitycheck <企业名>`，查看最近 20 条质量控制记录。
- 新增 `/company qualityreview <企业名> <批次ID> <状态>`，允许企业所有者记录人工处置：`released`、`rework` 或 `rejected`。
- 记录企业合并时同步迁移，最多保留最近 8192 条。

## 现实逻辑边界

现实制造业会把制造批次、质量检查、放行决定和不合格处置作为可追溯记录；本阶段先把这些事件独立记录下来，作为之后返工、召回和质保索赔的基础。

当前状态是游戏内过程筛查，不是法定合格证、第三方检测或监管许可；人工处置也暂时不会自动扣减库存。这样可以先建立审计链，再接入逐批次库存隔离，避免把总库存误当成已放行库存。

参考资料：

- [NIST：制造数据的可追溯性与可信性建议](https://www.nist.gov/publications/recommendations-ensuring-traceability-and-trustworthiness-manufacturing-related-data-0)
- [ISO 9001 documented information guidance](https://www.iso.org/files/live/sites/isoorg/files/standards/docs/en/iso_9001_2015_guidance_documented_information.pdf)

## 后续方向

1. 将库存拆分为批次/状态层，只有放行批次可用于交付。
2. 增加返工消耗、报废损失和不合格原因分类。
3. 将已交付批次与退货、质保和召回事件关联。

### company-quality-release-gate-2026-09-06.md

# 企业产品质量放行门槛（2026-09-06）

## 本阶段完成

- 新增配置项 `companyQualityReleaseThreshold`，默认值为 80/100。
- 生产批次仍会进入企业仓库、生产批次台账和质量控制台账，但低于阈值的批次不会增加可交易市场供应。
- 低于阈值的生产批次不会触发供应市场的自动履约。
- 企业当前累计产品质量低于阈值时，不能新建该产品的供应报价。
- 高于或等于阈值的批次才会进入商品供应和自动履约流程。
- 保留低质量货物在企业仓库中，后续可接入返工、报废、召回和质量索赔；本阶段不直接销毁库存。
- 新增质量隔离台账：待检、返工和拒收批次的数量从企业可用库存中扣除，但仍保留在仓库资产中。
- 质量审查改为 `released` 后才解除该批次的库存隔离；`rework`、`rejected` 和待检状态继续隔离。
- 企业生产消耗和供应市场交付都会避开已隔离数量。
- 将批次标记为 `rejected` 时，系统会移除该批次仍在仓库中的隔离品，结转存货成本并记录可追溯的质量报废损失。

## 现实依据与边界

制造业通常会把生产批次置于检验、放行、返工或报废状态；未放行批次不应直接作为合格品交付。本阶段用质量分数作为游戏化放行代理，尚未把仓库存货按批次物理隔离，因此低质量库存仍可能被企业内部其他流程消耗。

## 后续方向

- 为仓库增加可视化的批次级状态和隔离区，并支持返工批次重新生产和质量索赔。
- 增加返工消耗、报废损失、召回和质保索赔。

### company-quality-release-market-2026-09-06.md

# 质检放行与市场解锁 - 2026-09-06

## 本次实现

- 修复低质量批次人工审核为 `released` 后仍不会进入商品市场的问题。
- 批次放行时，先删除质检冻结记录，再将对应数量加入商品供给并触发待交付订单履约。
- 保留拒收批次的库存处置和损失确认逻辑不变。
- 质检冻结仍然阻止采购订单直接取用，只有明确放行后才恢复市场流通。
- 已放行和已拒收批次现在是终态，不能通过后续命令逆转；返工批次仍可重新作出放行或拒收决定。
- `/company quality` 现在同时显示仓库总量、质检冻结量和可用量，便于核对库存与市场可交付数量。
- 新增 `/company qualityrework <企业名> <批次ID>`：对处于 `rework` 状态的批次收取相当于原转换成本 20% 的返工费用，重新进入待检状态并提高 10 点检验分数；返工后仍需再次审核放行。
- 返工后的分数改善会同步写入对应商品的加权质量台账，避免批次记录、质量报告和供货资格出现不一致。
- 返工成本比例和质量提升值改为服务器配置项 `companyQualityReworkCostRate`、`companyQualityReworkScoreGain`，默认分别为 20% 和 10 分。

## 现实逻辑依据

制造企业通常需要经过检验、放行或隔离处置；未放行的批次不能作为合格品交付，放行后才进入可销售库存。本实现将“仓库中存在”和“可销售/可履约”分开，避免仅凭库存数量把待检产品当成合格商品。

## 后续方向

- 将返工状态连接到重新生产或返工成本，而不是只记录状态。
- 为电子产品增加批次良率、保修期和召回记录。
- 将石化产品质量等级、检测项目和合同规格接入采购订单。

### company-scale-model-2026-09-05.md

# Enterprise scale model - 2026-09-05

## Change

The company `level` mechanic has been removed. Real companies do not normally
advance through a universal level ladder. Their scale is observable through
separate accounting and operating metrics.

## Implemented

- `registeredCapital` replaces the persisted company level field.
- `/company contribute <name> <amount>` records paid-in capital and places the
  contribution in the company treasury.
- Production recipes now represent one batch. Output, input and recipe income
  are no longer multiplied by a fictitious level.
- A production batch requires the appropriate functioning machine, workers and
  treasury funding. Parallel capacity will come from more equipment and labor.
- IPO share count and fundamental value are derived from registered capital.
- Mergers add registered capital instead of adding levels.
- The company and securities screens show capital rather than `Lv.`.
- Added `/company statement`, a read-only balance-sheet snapshot showing cash,
  inventory, equipment, liabilities and derived equity from the persisted
  company resources.
- Production now uses parallel capacity: each functioning machine can support
  one batch, while each recipe's worker requirement limits the number of
  simultaneous batches. `/company operations` reports this capacity.
- Wage allocation and finance-company returns use the configured production
  cycle length rather than assuming a fixed 40 cycles per day.
- Replaced the obsolete level-based maintenance setting with fixed operating
  overhead per production batch.
- Added `/company dividend <name> <amountPerShare>` for listed companies. The
  distribution is paid from company cash, recorded in the company ledger, paid
  to online or offline shareholders, and assessed under the dividend tax type.
- Company borrowing is now represented by separate persisted loan contracts;
  `/company borrow`, `/company repayloan` and `/company companyloans` expose
  principal, interest, maturity and cash-flow effects without treating debt as
  revenue or principal repayment as an operating expense.
- Company mergers now transfer equipment, labor contracts, production history
  and loan liabilities to the surviving company. Mergers are rejected while
  either company has a legacy tax mirror balance, an outstanding corporate-tax
  bill or an open corporate tax period, preventing unresolved tax liabilities
  from disappearing with the absorbed company ID.
- Old serialized companies without `registeredCapital` load with the safe
  default of 1000 major currency units; new saves never write `level`.

## Why this is closer to reality

Capital is not the same thing as revenue, profit, assets, workforce or output
capacity. The model therefore keeps these dimensions separate: capital belongs
to the balance sheet, treasury is liquid cash, labor is represented by worker
contracts, equipment is represented by maintained machines, and production is
represented by recipe batches. Future enterprise size indicators should use
revenue, assets, employees, utilization and profitability rather than restoring
a universal level.

## Next direction

- Add capital changes through retained earnings, dividends and formal equity
  issuance rather than treating every funding event as an upgrade.
- Add assets and liabilities so capital can be reconciled with a balance sheet.
- Add production scheduling so multiple machines and workers create parallel
  batches without changing recipe quantities.
- Add revenue, employee-count and asset-based reports for company comparison.

## Reference material

- [IFRS Conceptual Framework](https://www.ifrs.org/issued-standards/list-of-standards/conceptual-framework/)
  separates assets, liabilities, equity, income and expenses, and treats owner
  contributions as distinct from income.
- [SEC Beginners' Guide to Financial Statements](https://www.sec.gov/about/reports-publications/beginners-guide-financial-statements)
  explains the balance-sheet equation and the distinction between cash,
  inventory, fixed assets and liabilities.

### company-scale-model-cleanup-2026-09-05.md

# 企业规模模型清理 - 2026-09-05

## 本次变更

- 删除残留的 `UpgradeCompanyPayload` 网络包及其服务端处理器、注册项。
- 更新统一帮助、README 和企业开发日志，明确企业没有通用的“等级/升级”阶梯。
- 将生产扩张表述统一为追加机器、工人、库存和营运资金，并以实际经营指标衡量规模。

## 当前企业规模依据

企业规模不再由一个整数决定，而是分别观察：注册资本、现金与其他资产、负债、收入、利润/现金流、员工人数、设备数量、产能利用率、库存和经营状态。这样可以表达“资本少但高效率”“资产大但负债高”等现实情况。

`/company metrics` 用于经营指标，`/company statement` 用于资产负债快照，`/company operations` 用于员工、设备和产能。注册资本只表示所有者投入/承诺的权益基础，不等同于收入、利润或企业评级。

## 后续方向

后续如需分类，应增加可解释的统计标签或监管分类（例如微型、小型、中型、大型），并由营业收入、员工人数或资产等规则计算；它只能用于报告、税务或监管门槛，不能作为简单的升级树，也不应直接乘生产产量。

### company-service-income-idempotency-2026-09-06.md

# 企业服务收入幂等结算（2026-09-06）

## 本次完成

- 企业服务生产周期的收入入账改用周期来源 ID。
- 服务周期重试时，会先检查企业会计流水中的来源标记，避免服务收入重复进入企业金库。
- 税务收入、VAT 评估和服务交付记录继续复用同一个服务周期来源 ID。

## 当前边界

- 服务收入的金库流水、企业所得税期间记录、VAT 记录和交付记录仍分别持久化；它们已经共享来源 ID，但尚未合并为单一状态机。
- 普通商品生产目前不在生产周期中直接结算销售收入，而是在商品交付/销售环节确认收入，符合“生产与销售分离”的经营逻辑。

### company-site-allocation-lifecycle-2026-09-06.md

# 站点资源分配生命周期 — 2026-09-06

## 本次实现

- 出售机器后，站点机器分配会自动压缩到剩余设备数量以内。
- 解雇员工后，站点员工分配会自动压缩到剩余在岗人数以内。
- 企业清算完成后，会清理站点资源分配和经营场所登记，避免已解散企业留下可用站点。
- 设备出售结算失败时仍会保留原设备和原分配，避免部分回滚造成数据不一致。

## 现实逻辑依据

企业资产出售、员工离职和法人清算都会改变工厂的实际生产能力；资产台账、人员配置和经营场所登记不能继续保留超过实际资源的配置。本阶段补齐了站点资源分配的生命周期约束。

## 后续方向

- 将机器维护、折旧和员工技能按站点分别归集。
- 为站点搬迁、停产和重新启用增加状态与审计记录。
- 将站点资源变更接入企业资产负债和清算报表。

### company-site-merger-limit-2026-09-06.md

# 企业合并站点上限 — 2026-09-06

## 本次实现

- 企业合并时，目标企业的经营场所数量严格遵守 `maxCompanySites` 配置上限。
- 已存在于目标企业的重复站点仍会正确合并，不会因为站点已满而丢失对应的迁移记录。
- 因站点上限无法迁移的源企业站点，其机器和员工分配不会残留到目标企业。
- 站点资源迁移与经营场所迁移使用同一份最终站点清单。

## 现实逻辑依据

企业合并后，经营场所和生产资源需要经过资产与经营许可清单迁移；系统容量或登记上限不能被合并操作绕过。本阶段先确保站点登记和资源分配的一致性。

## 后续方向

- 对被上限截断的站点增加合并审计与资产处置记录。
- 将站点许可、租赁合同和机器/员工迁移状态纳入合并报告。

### company-site-operating-report-2026-09-06.md

# 企业经营场所运营报告 — 2026-09-06

## 本次实现

- `/company operations <名称>` 现在按经营站点显示已记录生产批次数、累计转换成本和平均质量分。
- 仍保留企业级员工、工资、设备和并行产能信息，便于与站点批次数据交叉核对。
- 老批次没有站点字段时会单独归入 `legacy/unassigned`，不会被错误归到当前站点。

## 现实逻辑依据

企业的工厂、仓库和采掘站点通常分别形成运营记录；批次、质量和成本需要能够追溯到实际生产地点，才能支持成本控制、质量调查和监管审计。本阶段先提供只读运营报告，不改变现有生产能力。

## 后续方向

- 逐步把机器、员工和库存按站点分配，并让报告显示站点可用产能。
- 接入站点间转运和到岸成本，区分生产地、仓储地和销售目的地。
- 将质量召回、税务审计和油田开采记录按站点筛选。

### company-site-resource-allocation-2026-09-06.md

# 企业站点资源分配 — 2026-09-06

## 本次实现

- 新增持久化站点资源分配数据，支持按经营场所分配机器和员工。
- 新增命令：
  - `/company site assignmachine <企业> <机器类型> <数量>`
  - `/company site assignworkers <企业> <数量>`
- 分配数量不能超过企业当前可用机器或在岗员工，也不能与其他站点的分配合计冲突。
- 配置了站点资源后，生产会优先选择拥有对应机器和员工的授权站点，并按该站点的资源计算产能。
- 未进行任何站点分配的旧企业仍使用原有的全公司资源逻辑，保证旧存档兼容。
- 注销站点会清理该站点的资源分配；企业合并会迁移分配记录。
- `/company operations <企业>` 会显示各站点分配的员工和机器。

## 现实逻辑依据

多工厂企业通常需要把设备和人员配置到具体生产地点；企业总资产不等于每个工厂都能直接使用全部资源。本阶段采用“未配置时兼容旧逻辑、配置后启用站点核算”的迁移方式，逐步接近多工厂运营。

## 当前边界

- 机器库存仍由企业主体统一持有，站点分配是使用权/产能配置，不是物理资产转移。
- 员工技能仍按企业整体平均值计算，尚未按站点单独统计。
- 仓库库存仍是企业主体库存，尚未建立站点仓库和站点间运输。

## 后续方向

- 将员工技能、机器维护和设备折旧按站点归集。
- 为站点建立独立仓库、在途库存和站点间物流成本。
- 增加站点关闭、搬迁和资源重新分配的审计记录。

### company-size-profile-2026-09-05.md

# 企业规模分类 - 2026-09-05

## 已实现

- 新增 `CompanySizeProfile`，提供 micro、small、medium、large 四种只读统计标签。
- `/company metrics` 显示当前规模标签和按查询窗口年化的营业收入代理值。
- 分类使用员工数与营业额或资产的组合，不改变企业产量、税率、贷款额度或经营状态。
- 分类从现有经营快照实时计算，不新增持久化字段，也不需要旧存档迁移。

## 现实依据与边界

欧盟委员会的 SME 口径使用员工数，并结合营业额或资产负债表总额判断微型、小型和中型企业。本模组借用这一结构作为游戏内统计参考，但金额仍是游戏 USD 账本代理值，不能视为现实中的法律或税务认定；集团合并口径也暂未纳入。

## 后续方向

规模标签可以用于报表筛选、统计和未来可配置的监管门槛，但不会作为“升级”按钮，也不会直接乘生产数量。

### company-supply-delivery-ownership-2026-09-06.md

# 企业采购交付所有权 - 2026-09-06

## 已实现

- 企业使用金库采购的供应订单，交付物现在进入企业仓库。
- 个人采购仍进入发起玩家的个人仓库。
- 跨区域运输到货时根据订单上的企业标识恢复正确的目标仓库。
- 运输中的买方 UUID 仍保留，用于保险、损失通知和离线交付；仓库所有权不再因此错误降级为个人。

## 修复的问题

之前企业采购虽然从企业金库扣款，但货物会进入发起玩家的个人仓库，导致企业生产无法使用已采购的原料，且可能造成企业与个人资产混淆。

## 后续方向

- 将企业采购交付与存货成本批次关联，按实际到货数量建立加权平均成本。
- 增加企业仓库与运输中存货的财务展示，区分在库、在途和已耗用状态。

### company-tax-expense-event-identity-2026-09-06.md

# Company tax expense event identity — 2026-09-06

## Implemented

- Operating debits continue to enter deductible company expense ledgers.
- Equipment acquisition is now explicitly treated as a non-operating capital outflow; its tax effect comes through the existing depreciation path rather than an immediate expense deduction.
- Deductible cash expense source IDs include a UUID. Parallel production batches at the same game tick therefore create independent expense events instead of being collapsed by the tax ledger's idempotency guard.

## Accounting rationale

Routine production, maintenance, and rework costs are period operating expenses. Purchasing a durable machine creates a fixed asset and should not be deducted in full at acquisition. Separating the cash-flow label from the tax-expense event keeps the company ledger, corporate tax periods, and depreciation treatment consistent.

## Boundary

Jurisdiction-specific capital allowance schedules and tax-loss carryforwards are not introduced yet; depreciation remains the mod's game-scale asset-cost recovery model.

### inventory-cogs-sales-2026-09-06.md

# 企业销售成本结转（2026-09-06）

## 本次实现

- 企业通过供应市场实际发货时，先从企业库存成本层扣除对应数量。
- 已有加权平均成本层优先作为销售成本（COGS）。
- 历史存量或没有成本层的库存，使用当前商品市场价兜底，避免销售后成本永久缺失。
- 销售成本作为非现金、可抵扣经营费用记入企业季度和年度税务期间；不会再次扣企业金库。
- 销售成本同时写入非现金会计分录，`/company metrics` 现在会展示销售成本、毛利和经营现金流；销售成本不会降低现金流数字。
- 工资改为先按日应计费用和欠薪负债，实际发放时只记录现金流和负债结清；原料采购付款也不再直接计入利润费用，待投入生产或售出时确认成本。
- 现货市场当前仍是玩家仓库交易，因此本次不把企业销售成本接入现货市场。

## 后续方向

- 在资产负债表之外增加完整的期间损益表，进一步区分毛利、营业费用、营业利润和税前利润。
- 对退货、取消运输和拍卖成交建立对应的成本回转规则。
- 将企业直接面向 NPC/消费者的销售统一接入同一套销售成本结转接口。
- 为欠薪、供应采购和存货成本补充迁移审计，避免旧账本中的现金支付重复影响历史利润指标。

### inventory-cost-recognition-2026-09-06.md

# 存货采购与耗用成本 - 2026-09-06

## 已实现

- 企业采购原料时使用非经营现金流扣款，不再立即形成企业所得税可扣除经营费用。
- 采购后的原料保留在企业仓库，并由企业财务快照作为存货资产估值。
- 生产批次实际消耗原料后，按商品市场成本估算存货耗用，并记录为企业税务费用。
- 原有增值税进项抵扣和订单退款冲回逻辑保持不变。
- 个人经营采购逻辑未改变，避免混淆企业存货和个人经营账簿。

## 现实逻辑

原材料采购通常先进入存货资产；生产或销售耗用时才结转成本。这样可以区分现金流出、资产持有和当期成本，避免企业在尚未消耗原料时提前虚增费用。

本阶段使用商品市场价格作为游戏内存货耗用成本代理。后续若需要更精确的会计，可增加采购批次、移动加权平均或先进先出成本层。

## 后续方向

- 为企业仓库增加采购批次和成本层，支持移动加权平均/FIFO。
- 将成品销售时的存货成本结转与销售收入放在同一交易事件中。
- 在企业财务界面中分开显示存货采购、存货耗用和销售成本。

### inventory-fifo-cost-2026-09-06.md

# 企业库存 FIFO 成本结转 - 2026-09-06

## 本次实现

- 企业库存成本现在持久化记录按进入顺序排列的成本批次。
- 生产、采购和其他入库都会追加新的 FIFO 批次。
- 销售、生产领料、运输损耗和企业清算消耗最早进入的批次，并按批次成本结转。
- 旧存档只有汇总成本层时，会在首次访问时转换为一个遗留批次，不会丢失原有数量或金额。
- 成本总层仍然同步维护，现有资产负债表和库存估值接口保持兼容。
- 合并企业时批次会随库存成本一起转移。
- 新增纯 Java FIFO 计算测试，覆盖跨批次消耗和部分批次成本分摊。

## 现实依据

IAS 2 允许可互换存货采用 FIFO 或加权平均成本，并要求存货按成本与可变现净值孰低计量。本阶段选择 FIFO 作为企业批次追踪的实现方式；旧数据继续可读取。

参考：[IFRS Foundation - IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/)

## 当前边界

- 尚未增加会计政策配置界面；当前企业统一使用 FIFO。
- 期末跌价准备仍沿用现有的成本/市场价值估值逻辑，尚未单独生成减值凭证。

### inventory-valuation-nrv-2026-09-06.md

# 企业库存可变现净值估值 - 2026-09-06

## 本次实现

- 新增独立的成本与可变现净值孰低估值计算器。
- 企业财务快照继续以孰低金额计入库存资产。
- 财务快照新增库存跌价金额字段，用于后续生成减值凭证和报表展示。
- 跌价金额仅影响资产估值，不会重复扣除企业现金。
- 新增测试覆盖可变现净值低于成本和高于成本两种情况。

## 现实依据

IAS 2 要求存货按成本与可变现净值孰低计量；可变现净值是正常经营中预计售价减去完工和销售所需成本。

参考：[IFRS Foundation - IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/)

## 当前边界

- 当前市场价格作为简化的可变现净值，暂未单独建模销售佣金、完工成本和滞销周期。
- 跌价金额目前在财务快照中提供，尚未按会计期间生成独立减值凭证。

### payroll-settlement-idempotency-2026-09-06.md

# 企业工资日结幂等性（2026-09-06）

## 本次完成

- 企业工资日结现在使用企业工资账户中的 `lastSettlementDay` 作为独立幂等键。
- 如果全局经济日结在处理完某企业后、保存全局进度前中断，重启后不会再次计提同一天工资或重复写入工资成本。
- 不影响清算流程对既有欠薪的支付。

## 现实逻辑对应

工资通常按固定结算日计提一次；付款失败会形成应付工资负债，而不是再次生成一笔新的同日工资费用。本次修复使工资应计与欠薪负债符合这一逻辑。

### production-batch-atomicity-2026-09-05.md

# Production batch atomicity

## Implemented

- Added an atomic warehouse batch-consumption operation for production bills of materials.
- All input item IDs, positive quantities, and available stock are validated before any input is removed.
- Production now uses that operation instead of consuming inputs one by one, preventing partial material consumption when a recipe is incomplete.
- Commodity supply statistics are updated only after the complete batch has been removed.

## Real-world alignment

A manufacturing work order consumes a defined bill of materials as one committed batch. A missing component should leave the work order unstarted rather than consuming only the components that happened to be available.

## Next direction

- Connect machine purchases and maintenance to industrial goods instead of treating all equipment as abstract balance-sheet entries.
- Add service contracts and customer orders for industries whose outputs are services rather than warehouse items.

### production-batch-site-traceability-2026-09-06.md

# 生产批次经营场所追溯 — 2026-09-06

## 本次实现

- 生产批次新增生产场所维度、区块坐标字段。
- 有经营场所且拥有商业经营权的企业，新批次会记录实际使用的第一个授权站点。
- 石油开采批次会记录本次实际开采油田所在的站点，而不是笼统记录企业主体。
- `/company batches <名称>` 会显示批次对应的维度和区块。
- 老存档批次使用空场所字段兼容读取，不改变旧数据和现有产能规则。

## 现实逻辑依据

制造业质量追溯通常需要把批次与工厂、生产线、设备和检验记录关联。当前先建立“批次—经营站点”关系，后续再把设备、人员、原料批次和召回/返工记录继续接入。

## 后续方向

- 将机器、员工和仓库存量按经营站点分配，而不是只按企业主体汇总。
- 为跨站点转移增加在途库存和站点间物流成本。
- 在质量召回和税务审计中按站点筛选批次。

### production-cogs-order-2026-09-06.md

# 生产成本与供应订单履约顺序修复（2026-09-06）

## 本次完成

- 调整企业生产结算顺序：先把产成品入库并建立实际转换成本层，再履约供应订单。
- 供应订单发货消耗的产成品现在优先使用本次生产的实际成本，不再因为成本层尚未建立而退回市场价估算。
- 未被订单取走的产成品仍保留在企业仓库，并保留对应的加权平均成本层。

## 会计逻辑

生产成本应先形成存货成本；产品售出或交付时，再将相应成本结转为销货成本（COGS）。本次修复使企业生产、仓储、供应订单和利润核算遵循这一顺序。

### service-delivery-cycle-identity-2026-09-06.md

# 服务交付周期标识（2026-09-06）

## 本阶段实现

- 服务交付记录新增可选 `deliveryId`，旧存档和旧构造方式保持兼容。
- 生产追赶期间按“企业 + 生产周期时间点 + 并行批次序号”生成稳定周期键。
- 服务收入的税务来源和服务交付台账现在可以关联到同一个周期标识。
- 制造批次追溯记录在离线追赶场景下也复用周期键，避免同一周期产生无法对账的随机批次号。
- 抽出独立的周期标识工具类，并增加稳定性与非法参数测试。
- 生产周期开始时检查已完成批次 ID；服务器重载后的重试不会再次扣料、扣费或产出已落账批次。
- 重复写入同一服务交付记录时，台账会拒绝重复事件。

## 现实逻辑对应

服务企业需要把收入追溯到具体服务项目/交付事件，而不是只保留按时间汇总的收入。稳定事件号也为后续客户合同、发票、应收账款和争议处理提供关联键。

## 当前边界

本阶段只补齐审计标识，不改变现有“服务配方周期直接结算收入”的玩法；跨多个 SavedData 的完全原子结算仍需后续统一交易流水。

## 后续方向

- 将周期标识接入服务合同和客户订单。
- 在合同交付后再确认收入，并把取消、退款和质量争议纳入同一结算链。

### service-delivery-ledger-2026-09-06.md

# 服务交付台账（2026-09-06）

## 本次实现

- 新增 `CompanyServiceDeliverySavedData`，持久化服务生产周期的交付记录。
- 每条记录保存企业、游戏时间、服务配方、收入、投入品成本、周期运营成本、设备折旧和所需工人数。
- 每家企业保留最近 512 条记录，避免服务周期长期运行导致存档无限增长。
- 服务周期成功结算后才写入台账；失败的尝试不会伪造交付记录。
- 新增只读命令 `/company services <公司名>`，显示最近 10 条服务交付及扣除直接成本和折旧后的“工资前贡献”。
- 工资仍由现有每日工资结算单独计提，台账不重复计算工资。
- 服务周期同时通过统一税务系统生成 VAT 销项发票/税务记录，使用独立来源键避免重复计税。

## 现实逻辑对照

现实企业的服务收入通常需要能够追溯到服务项目、交付时间、直接投入和人员成本；财务报表中的收入汇总不应替代业务明细。本阶段先建立业务明细层，后续再接客户、合同、发票和应收账款。

## 当前边界

- 当前服务收入仍由生产配方周期结算，台账只是审计与可视化记录，不改变付款方和收入规则。
- VAT 税率、免税和起征点仍由现有税务规则配置决定；没有适用规则时不会凭空生成税额。
- 服务交付尚未绑定具体客户或订单；这属于下一阶段的重大业务流程扩展。
- 台账中的“工资前贡献”不代表最终利润，工资、税费、融资成本和其他期间费用仍由现有系统处理。

### service-labor-requirement-2026-09-06.md

# 服务业最低用工要求 - 2026-09-06

## 已实现

- 默认服务行业的正收入服务周期最低需要 1 名员工。
- 新生成的行业配置会自动写入这一最低用工要求。
- 旧世界中与官方默认值完全一致、但因旧规则记录为 0 名员工的服务行业，会自动迁移到 1 名员工。
- 有意修改过输入、收入、产出或员工数量的自定义行业配置会保留原值。
- 金融行业收入为 0，仍不自动生成生产员工需求。

## 现实逻辑

运输、餐饮、教育、医疗、信息技术和咨询等服务需要人员提供、管理或交付。即使服务没有可存入仓库的物品，也不能在没有劳动力的情况下持续结算收入。

## 后续方向

- 按服务类型拆分岗位技能、最低资质和员工生产率。
- 将服务周期收入改为客户订单/合同结算，并让员工技能影响交付质量和收入。

### service-production-settlement-2026-09-06.md

# 服务业生产结算 - 2026-09-06

## 已实现

- 生产配方增加服务周期识别：无仓库物品产出但有正收入的配方属于服务周期。
- 服务周期会检查员工、设备、原料和运营资金，先扣除投入与生产成本，再将服务收入记入企业金库。
- 服务收入会写入企业账本，并进入企业所得税期间累计。
- 制造业和采矿业仍然先生产实物并进入企业仓库，不会把物品产出重复当作自动收入。
- 服务生产前增加原料和收入溢出预检查，避免扣除成本后才发现无法完成结算。

## 现实逻辑

运输、住宿餐饮、零售、信息技术、教育和咨询等行业通常出售服务，不会产生可存入仓库的统一物品。它们的收入来自完成服务周期或客户交付，因此使用“投入/劳动力/设备 → 服务收入”的抽象比虚构一个商品产出更接近现实。

## 当前边界

- 当前收入仍由配置中的服务周期收入代表，尚未建立客户、服务合同、订单量和服务质量系统。
- 下一阶段可将服务收入改为订单结算，并加入需求波动、交付延迟和客户满意度。