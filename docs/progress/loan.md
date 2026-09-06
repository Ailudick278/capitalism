# Capitalism Mod - loan progress log

Consolidated on: 2026-09-06

## Current system log

# 贷款系统开发进度

最后更新：2026-09-06

## 已实现功能

- 支持玩家之间的点对点贷款数据结构。
- 支持贷款申请、放款、还款、到期和贷款状态持久化。
- 支持银行贷款相关事件、贷款命令和基础利息处理。
- 支持贷款与账户、钱包和经济数据的连接基础。
- 新增企业贷款：企业可在注册资本的 5 倍债务上限内借款，贷款本金进入企业资金而不计入经营收入；本金、利息、期限和逾期状态持久化。
- 企业贷款还款从企业资金中扣除，并写入企业账本；本金偿还不作为可抵扣经营费用。
- 贷款数据的 Codec 已改为延迟初始化，纯贷款计算不再强制依赖 Minecraft 数据修复运行时；贷款利息和异常值单元测试已通过。
- 点对点贷款到期和逾期会分别通知借款人与出借人；离线玩家登录后可收到持久化通知。
- 支持 `/repay <贷款ID> <金额>` 的利息优先部分还款；已支付利息会持久化，避免重复收取。
- 新增企业抵押物评估报告 `/company collateral <名称>`，按库存和设备的保守折价估算参考授信空间。
- 抵押评估会排除质检冻结/拒收库存，并显示受限库存价值；该报告只读，不改变当前无抵押贷款审批规则。
- 企业贷款债务上限倍数改为配置项 `maxCompanyDebtMultiple`，默认仍为注册资本的 5 倍。
- `/company credit` 授信报告现在与实际放贷审批共用该债务倍数配置，不会再出现报告与审批口径不一致。
- 企业贷款最长期限改为配置项 `maxCompanyLoanTermDays`，默认值为 3650 天。
- 企业贷款最低偿债覆盖率改为配置项 `companyLoanMinCoverageRatio`，默认仍为 1.25 倍。

## 当前边界与待处理问题

- 点对点贷款和银行贷款的规则还需要进一步区分。
- 抵押登记、担保人、优先受偿和违约处置尚未完整模拟；当前抵押评估仍只是授信准备层。
- 当前部分还款暂不直接冲减本金。
- 贷款现金流与企业、土地、税务之间的联动仍可加强。

## 后续开发方向

1. 统一贷款合同：本金、利率、周期、还款计划、费用和状态。
2. 增加抵押物、担保人、信用等级和违约处置。
3. 支持分期还款、提前还款、展期、逾期罚息和催收。
4. 将贷款风险纳入企业、个人和银行的财务状态。

## Consolidated historical entries

### company-loan-cashflow-multiple-config-2026-09-06.md

# 企业现金流债务倍数配置化 - 2026-09-06

## 本次实现

- 新增 `companyLoanCashFlowDebtMultiple` 配置项，默认值为 3.0。
- 贷款申请和企业信用报告都使用该配置计算现金流支持的债务上限。
- 配置范围限制为 0.0 到 20.0，便于服务器按风险偏好调整。
- 原有无配置调用仍默认使用 3.0，保持旧世界行为兼容。
- 新增保守和宽松授信倍数测试。

## 现实逻辑

现金流债务倍数属于授信政策参数，不同金融机构会根据行业、风险等级和担保情况设定不同上限；将其配置化比把单一倍数写死更符合现实授信流程。

## 当前边界

- 当前倍数仍作用于回溯期现金流，没有按行业或信用等级自动分层。
- 尚未把抵押品折扣、现金流倍数和 DSCR 组合为可解释的贷款产品规则。

### company-loan-cashflow-noncash-adjustment-2026-09-06.md

# 企业贷款现金流非现金项目校正 - 2026-09-06

## 本次实现

- 贷款现金流评估不再把销售成本、应计费用和库存损失直接计为现金流出。
- 这些凭证仍会被识别为经营历史，因此不会把有经营活动的企业误判为“没有经营历史”。
- 贷款授信现金流口径与企业经营报表的现金流口径保持一致。
- 新增测试覆盖三类非现金经营凭证。

## 现实依据

IAS 7 要求现金流量表调整非现金项目、递延或应计项目；不涉及当期现金的会计事项不应直接进入现金流量。

参考：[IFRS Foundation - IAS 7 Statement of Cash Flows](https://www.ifrs.org/issued-standards/list-of-standards/ias-7-statement-of-cash-flows.html/)

## 当前边界

- 现金流仍采用模组内部账本的简化直接法，尚未完整拆分经营、投资和融资三类现金流报表。
- 贷款承销仍是自动化规则，尚未增加人工审批、债务重组和抵押品处置流程。

### company-loan-installments-2026-09-05.md

# Company loan installments

## Implemented

- Added a scheduled-payment estimate based on the remaining principal, accrued interest and remaining days.
- Added `/company repayinstallment <name> <loanId>` for one scheduled payment.
- `/company companyloans <name>` now displays the current installment estimate.
- Overdue loans have one immediate payment equal to the current outstanding principal and interest.
- Manual `/company repayloan` remains available for arbitrary partial or full payments.

## Limitation

- The first version is an equal-payment estimate rather than a separately persisted amortization calendar. Daily interest accrual and payment dates can be made more precise in a later loan-contract revision.

### company-loan-interest-accounting-2026-09-06.md

# 企业贷款利息会计（2026-09-06）

## 本次实现

- 企业贷款还款中的利息部分现在单独写入 `interest_expense` 企业账簿记录。
- 利息费用进入经营费用和现金流统计；偿还本金仍保留为融资活动，不计入经营利润。
- 利息同步进入企业所得税的可扣除费用记录，避免把本金误当作税前费用。
- 实际扣款金额、还款顺序和本金余额规则保持不变。

## 现实逻辑对照

企业财务处理中，借款本金是负债偿还，利息是融资成本/财务费用，两者不应在利润表中混为一项。将两部分拆开后，现金流、利润、税务扣除和资产负债表能保持更一致。

## 当前边界

- 当前只对企业贷款还款拆分利息；个人贷款和点对点贷款仍使用各自的账务路径。
- 利息来源仍是现有按日简化模型，尚未引入复利、浮动利率或贷款重组。

### company-loan-payment-allocation-2026-09-06.md

# Company loan payment allocation — 2026-09-06

## Implemented

- Extracted company-loan payment allocation into a pure rule object.
- Payments first settle accrued interest, then reduce principal.
- Full repayment leaves zero principal; overpayments and invalid amounts are rejected.
- Added unit tests for interest-first partial payment, full payment and overpayment rejection.

## Accounting rationale

Separating the payment allocation from the command/service layer makes the loan balance auditable and prevents a future change to interest calculation from silently producing an incorrect principal balance.

## Boundary

The loan still uses the existing game-scale daily interest and maturity model. Collateral enforcement, guarantors and restructuring remain separate future features.

### company-loan-payment-audit-2026-09-06.md

# 企业贷款还款分配台账（2026-09-06）

## 本次实现

- 每次成功的企业贷款还款都会持久化记录总还款额、利息部分、本金部分和剩余本金。
- 全额还款也会写入最后一条分配记录，剩余本金为 0；部分还款继续更新原贷款合同。
- 新增只读命令 `/company loanpayments <公司名> <loanId>`，用于查看还款分配历史。
- 还款分配发生在扣款前校验之后，扣款失败不会产生虚假还款记录。

## 现实逻辑对照

银行还款通常需要将一笔付款拆分为利息和本金。仅保存“支付了多少钱”不足以支持贷款余额、利息支出、信用报告和审计，因此本阶段把分配结果独立持久化。

## 当前边界

- 贷款仍使用现有简化的按日利息和到期模型，尚未加入抵押物、担保人和重组流程。
- 还款台账是审计层，不改变既有贷款审批和还款命令的参数。

### company-loan-period-annualization-2026-09-06.md

# 企业贷款现金流回溯期年化 - 2026-09-06

## 本次实现

- 债务偿付能力评估新增按实际回溯天数年化的接口。
- 企业信用报告不再把任意回溯窗口都当成季度数据。
- 贷款申请继续使用固定 90 天经营现金流窗口，并明确传入 90 天参数。
- 保留旧接口并默认使用 90 天，兼容现有调用和旧测试。
- 新增测试验证 30 天与 90 天现金流的年化结果不同。

## 现实逻辑

年化经营现金流应根据观测期间折算，而不是无条件乘以 4；30 天、90 天和年度窗口代表不同的信息量和季节性风险。

## 当前边界

- 当前仍使用简单线性年化，尚未处理季节性行业、异常月份和现金流波动区间。
- 授信仍采用自动规则，未加入人工复核和债务重组。

### company-loan-repayment-2026-09-05.md

# Company loan repayment

## Implemented

- Partial company-loan payments are now supported instead of requiring either interest-only payment or full payoff.
- Payments apply to accrued interest first, then reduce outstanding principal.
- Full payoff still removes the loan contract; partial payments preserve the same saved loan record and update its principal.
- Existing saves and command syntax remain compatible: `/company repayloan <name> <loanId> [amount]`.

## Limitation

- The current contract remains a balloon-style loan. Interest is calculated from the current outstanding principal using the existing model; a future installment schedule can make daily accrual and amortization more precise.

### company-loan-segmented-interest-2026-09-06.md

# 公司贷款分段计息修复 - 2026-09-06

## 本次实现

- 修复公司贷款部分还本后的计息错误。
- 贷款现在会保存历史累计利息，以及最近一次本金变更时的已计息天数。
- 本金减少后，历史期间仍按变更前本金计息，后续期间才按新的未偿本金计息。
- 保留旧存档字段的默认值，旧贷款继续按照原有规则计算，并在首次部分还本后进入分段计息。
- 增加单元测试，覆盖部分还本后历史利息不被重算、后续利息按新本金产生的情况。

## 现实逻辑依据

分期贷款通常按每个计息期间的未偿本金计算利息；已经经过的计息期间不能因为之后发生还本而被重新按低本金计算。该实现将贷款计息拆成多个本金区间，避免少计利息。

## 后续方向

- 将点对点贷款也迁移到同一套可复用的分段计息模型，届时支持更完整的部分还本和摊销计划。
- 增加贷款还款计划、逾期费用和提前还款明细的界面展示。
- 继续检查服务器长时间离线后补结算与还款操作之间的边界情况。

### company-loan-servicing-2026-09-05.md

# Company loan servicing

## Implemented

- Company loans now expose explicit due and overdue transition checks.
- Daily settlement sends the company owner a notice when a loan becomes due.
- Daily settlement sends a second notice when the loan becomes overdue and penalty interest begins.
- Notifications are deduplicated and delivered after login through the existing persistent loan-notification queue.

## Next direction

- Add a company debt-service ratio based on operating cash flow.
- Add collateral and a documented default/recovery process before introducing forced liquidation.

### company-loan-settlement-idempotency-2026-09-06.md

# 企业贷款日结幂等性（2026-09-06）

## 本次完成

- 为每笔企业贷款增加 `lastSettlementDay` 持久化字段。
- 企业贷款期限递减、到期通知和逾期状态转换现在按贷款单独去重。
- 服务器在贷款结算后、全局经济日结标记保存前重启时，不会让同一笔贷款重复结算一天。
- 旧存档没有该字段时默认从未结算，能够平滑兼容。

## 现实逻辑对应

贷款机构通常按每笔合同的结算日计提利息和更新到期状态，而不是依赖一次不可分割的全局循环。本次把结算进度放到贷款合同本身，减少重启造成的期限和利息偏差。

### loan-history-query-2026-09-06.md

# 贷款历史查询完善 - 2026-09-06

## 本次实现

- 修复公司贷款结清后无法查看历史还款记录的问题。
- 公司贷款历史查询现在可以通过活动贷款或还款账本确认贷款归属。
- 历史时间统一使用万年历格式显示，不再直接显示原始游戏时间戳。
- 点对点贷款已提供 `/loanhistory <loanId>`，公司贷款继续使用公司贷款记录查询入口。

## 设计依据

现实中的贷款账户在结清后仍然保留历史流水，结清状态不会删除对账记录。本次调整将“当前负债状态”和“历史还款账本”分离，确保结清、转让或归档后仍可审计。

## 后续方向

- 将公司和点对点贷款的历史查询整合为统一的贷款账户视图。
- 增加结清状态、结清日期和累计利息等摘要信息。
- 后续自动扣款若加入，必须复用同一套还款流水记录逻辑。

### loan-overdue-interest-2026-09-06.md

# 贷款逾期利息修正 - 2026-09-06

## 已实现

- 企业贷款与个人互助贷款统一采用“正常期限利息 + 逾期期间罚息”的计算方式。
- 正常到期前的利息按原年利率和实际经过天数计算。
- 逾期后只对逾期天数计算罚息，当前罚息系数为正常日利息的 2 倍。
- 已支付利息仍会从累计应计利息中扣除，避免重复收取。
- 修正了逾期后把整个贷款期限利息追溯性翻倍的问题。

## 现实逻辑说明

逾期罚息通常针对逾期本金和逾期期间计收，不应把正常还款期内已经产生的利息重新翻倍。本次调整保留游戏中清晰的固定罚息规则，同时避免逾期瞬间产生不合理的追溯性费用。

## 后续方向

- 将罚息系数、宽限期和逾期分级纳入贷款产品配置。
- 后续可增加逾期记录对企业信用报告、授信额度和续贷条件的影响。
- 暂不改变现有贷款存档格式与还款入口。

### loan-persistence-2026-09-05.md

# Loan persistence progress log — 2026-09-05

## Implemented

- `PeerLoanSavedData.replaceLoan` now marks the world data dirty whenever a loan is replaced.
- Loan updates are now persistence-safe even when called outside the daily economy settlement path.

## Design boundary

- No loan rates, maturity rules, repayment amounts, or overdue penalties were changed.
- This is a SavedData lifecycle fix; the existing daily settlement remains compatible.

## Follow-up

- Add explicit peer-loan repayment receipts and overdue notices.
- Separate principal, accrued interest, and overdue penalty into auditable ledger fields before expanding loan gameplay.

### peer-loan-payment-journal-2026-09-06.md

# 点对点贷款还款账本 - 2026-09-06

## 本次实现

- 新增 `PeerLoanPaymentSavedData`，持久化记录每次点对点贷款还款。
- 记录贷款 ID、出借人、借款人、游戏时间、还款总额、利息、本金、剩余本金、剩余期限和逾期状态。
- 每次 `/repay` 成功后写入账本，完整还款也会保留一条记录。
- 账本最多保留 2048 条记录，并提供按贷款和按玩家查询的方法。
- 新增 `/loanhistory <loanId>`，借款人或出借人可以查看最近 10 条还款流水。

## 现实逻辑依据

现实金融系统会保留还款流水，用于核对债务余额、区分本金与利息、处理争议和生成信用记录。将还款从“只改变贷款状态”扩展为“状态 + 持久化流水”，为后续对账、自动扣款和信用评估提供基础。

## 后续方向

- 为玩家增加查看个人贷款流水的命令或界面。
- 将公司贷款与点对点贷款的流水查询统一成贷款账户视图。
- 让自动扣款和每日结算复用同一套流水写入服务，确保所有还款来源都有记录。

### peer-loan-repayment-service-2026-09-06.md

# 点对点贷款还款服务抽离 - 2026-09-06

## 本次实现

- 新增 `PeerLoanHelper.repay`，集中处理点对点贷款的借款人扣款、出借人到账、逾期利息、本金分配、贷款状态更新和还款账本写入。
- `/repay` 命令入口改为调用共享还款服务。
- 保持原有还款规则和还款记录格式不变，避免命令层重复实现资金结算。
- 为后续 GUI、授权自动扣款和其他服务器端入口复用同一套还款流程打下基础。

## 设计依据

现实金融系统通常将支付执行、债务余额更新和交易流水写入放在同一个结算服务或事务边界内，避免不同入口产生不同的本金、利息或收款规则。本次先完成点对点贷款的服务化抽离，公司贷款已有 `CompanyLoanHelper` 作为对应服务入口。

## 后续方向

- 清理命令类中的旧兼容实现，并让所有点对点贷款入口只保留共享服务调用。
- 将公司贷款与点对点贷款的公共支付分配规则继续抽取为统一的纯逻辑组件。
- 如果未来加入自动扣款，必须显式保存借款人的授权状态，并复用该服务写入同一份还款账本。

### peer-loan-segmented-interest-2026-09-06.md

# 点对点贷款分段计息与部分还本 - 2026-09-06

## 本次实现

- 点对点贷款迁移到与公司贷款一致的分段计息模型。
- 还款按照“先结清已产生利息，再冲减本金”的规则分配。
- 支持在支付完当前利息后进行部分还本，不再把部分还本限制为只能支付利息。
- 保存历史累计利息和最近一次本金变更时点，避免后续计息重算历史期间。
- 增加纯逻辑的还款分配类与单元测试。
- 保留旧存档字段默认值，旧点对点贷款可以正常读取。

## 现实逻辑依据

现实中的贷款还款通常会区分利息和本金：当期应计利息优先结清，剩余款项减少未偿本金；本金减少后，未来计息期间才按新的未偿本金计算。本次实现将这一规则落到贷款数据和还款命令中。

## 后续方向

- 增加还款记录 SavedData，使借款人和出借人都能查询本金、利息、逾期费用的历史明细。
- 将自动扣款、到期结清和提前还款统一到同一个结算服务，降低命令和每日结算之间的差异。
- 后续再加入逾期宽限期、违约费用和可配置的还款计划。

### peer-loan-settlement-day-2026-09-06.md

# 点对点贷款结算日标记（2026-09-06）

## 本阶段实现

- 点对点贷款新增可选 `lastSettlementDay` 字段，旧存档默认使用 `-1`。
- 每笔贷款在处理某个世界结算日后保存该日标记；同一结算日重试会跳过已经处理的贷款。
- 贷款期限通知和逾期状态更新继续使用原有规则，不改变利率或到期日逻辑。

## 现实逻辑对应

贷款系统通常按账期记录每笔合同的已计提/已结算期间，而不是只依赖全局批处理完成标记；这样单笔合同可以在批处理故障后安全恢复。

## 后续方向

- 为点对点贷款的利息支付增加独立的付款流水和对手方对账。
- 继续审计债券、期货和股票日结算的单笔事件标识。