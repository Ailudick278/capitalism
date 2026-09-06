# Capitalism Mod - tax progress log

Consolidated on: 2026-09-06

## Current system log

# 税务系统开发进度

最后更新：2026-09-05

## 已实现功能

- 税务功能集中在独立 `tax` 系统中，其他系统通过服务和事件接入。
- 已建立土地持有税、企业所得税、个人经营所得税、增值税、土地转让税、印花税、资本利得税、股息税、资源税、关税、继承/赠与税等税种框架。
- 支持税率规则持久化、税期、税单、税务账本、税款支付和税务交易记录。
- 支持企业和个人经营收入凭证、采购成本凭证、费用凭证与周期汇总。
- 支持税务申报、缴税、欠税、税务执法、税期锁定和补税。
- 支持税务抵扣、退税申请、内置退税规则自动比对、管理员审核和退税通知。
- 支持税务更正申请、审核日志和管理员批准/驳回。
- 支持税务命令、服务层和网络同步。

## 当前边界与待处理问题

- 税种框架已较完整，但具体税率、免征额、扣除项和申报周期仍是游戏规则，需要继续配置化。
- 现实税制中的主体认定、发票链、跨地区税收和税务争议尚未完整模拟。
- 管理员审核与自动审核的责任边界还需要进一步明确。

## 后续开发方向

1. 完成税种参数配置化，避免把税率和资格条件散落在业务代码中。
2. 建立统一的计税、申报、审核、缴纳、退税状态机。
3. 增加免税、减免、抵扣结转、预缴和逾期滞纳金规则。
4. 将企业、土地、市场交易、证券和继承事件统一接入应税事件接口。
5. 增加税务报表和管理员审计视图，方便定位每一笔税款来源。

## Consolidated historical entries

### corporate-tax-loss-carryforward-2026-09-06.md

# Corporate tax loss carryforward — 2026-09-06

## Implemented

- Annual corporate tax now calculates signed operating result before applying tax.
- A company loss is saved as a tax-loss carryforward instead of being discarded.
- A later profitable year uses the carryforward to reduce taxable profit.
- Unused losses continue to carry forward after a partial offset.
- Quarterly prepayments remain based on the current quarter; the annual settlement reconciles the final taxable result.
- Existing overpayment handling still turns excess prepaid tax into a tax credit.

## Accounting rationale

Real corporate income-tax systems commonly distinguish accounting profit from taxable profit and allow qualifying losses to offset future taxable income, subject to jurisdiction-specific limits. The mod now models the core mechanism without introducing a jurisdiction-specific expiry or ownership-change rule yet.

## Future direction

- Add configurable carryforward expiry/limits if the tax rules become jurisdiction-specific.
- Separate tax adjustments from accounting expenses when the company accounting model is expanded.

### corporate-tax-treasury-payment-2026-09-05.md

# 企业所得税由企业金库支付（2026-09-05）

## 已实现

- 企业所得税单通过统一税务界面支付时，改为从企业金库扣款，不再从企业主个人钱包扣款。
- `/company pay tax` 对应的旧入口也改为遍历企业税单、完成申报并由企业金库结算。
- 个人所得税、个人经营所得税和其他个人税单仍由个人钱包支付。
- 企业税款支付写入企业非经营现金流账本，并通过统一税务账簿和税款支付事件更新欠税镜像。

## 设计依据

企业是独立的纳税和财务主体，企业经营收入、支出与所有者个人资金应分开记录。该改动使企业税款、企业现金流和企业负债保持一致。

## 后续方向

- 为企业税单增加专门的申报、授权和付款状态界面。
- 将企业税款支付、预缴、抵扣和年度汇算统一到企业税务工作流。

### stock-stamp-duty-withholding-2026-09-06.md

# 证券印花税预扣结算 — 2026-09-06

## 本次修复

- 股票成交时从卖方成交款中预扣印花税。
- 预扣金额现在通过税务系统的外部款项结算接口同步结清对应税单。
- 保留税务账本、付款记录和成交事件，不再让卖方收到净款后又产生一笔重复待缴印花税。
- 买方、卖方成交路径统一使用同一套预扣逻辑。

## 现实逻辑对照

证券印花税通常在交易结算时由交易系统代扣代缴；投资者收到的是扣税后的净卖出款，而不是先收到全额再被重复开具同额欠税。本次修改将“成交款扣除”和“税务结清”连接起来。

## 后续方向

- 将成交编号从随机事件号升级为持久化成交流水号，进一步增强服务器重启后的税务幂等性。
- 将资本利得税与印花税分别核算，避免把交易环节税和收益税混为一谈。

### tax-credit-source-idempotency-2026-09-06.md

# 输入税额抵扣来源幂等性（2026-09-06）

## 本阶段实现

- 输入 VAT 抵扣批次现在按纳税人、币种和来源事件号去重。
- 即使服务器在写入抵扣批次后、写入来源标记前重启，重试也不会重复增加抵扣余额。
- 没有来源号的历史/兼容调用仍保留原有行为。

## 现实逻辑对应

现实中的进项税额抵扣应对应一张可识别的发票或采购凭证；同一凭证不能因为系统重试而重复抵扣。来源号因此是抵扣批次的审计主键，而不仅是附加日志。

## 后续方向

- 将抵扣来源与采购订单、发票和退款冲正事件统一关联。
- 继续完善退款支付与抵扣扣减之间的可恢复结算流水。

### tax-enforcement-2026-09-05.md

# Tax enforcement progress log — 2026-09-05

## Implemented

- Added `TaxEnforcementTickHandler`.
- Every 1200 server ticks, the server scans unpaid tax bills, updates late fees, and dispatches delinquency events through the central tax service.
- Tax enforcement no longer depends on a player opening the tax screen; offline taxpayers continue to advance through the same tax lifecycle.
- Land tax enforcement therefore remains connected to the unified tax ledger even when no land or tax screen is open.

## Design boundary

- No tax rates, grace periods, or land disposal thresholds were changed.
- This is a server-side lifecycle correction, not a new player-facing gameplay rule.

## Follow-up

- Add focused tests for scheduled late-fee updates and idempotent enforcement notices.
- Review whether player notifications should be persisted as mailbox notices for offline players.

### tax-late-fee-config-2026-09-06.md

# 税款滞纳金配置化（2026-09-06）

## 本阶段实现

- 新增 `taxLateFeeRatePerDay` 配置，默认值为 `0.0005`，即每日万分之五。
- 税单逾期后按未缴税款、逾期天数和当前配置计算滞纳金；不再把费率写死在业务代码中。
- 保留原有的声明、到期、宽限期、欠税和持久化账单流程。
- 计算使用定点十进制并做溢出保护，最低按 1 个最小货币单位计收，避免浮点误差导致滞纳金消失。
- 计算已抽离为纯函数，并增加默认费率、最低单位、无效输入和溢出场景的单元测试。
- 部分缴款现在会减少后续滞纳金的计费本金；税款本金缴清后，只保留已产生的滞纳金，不再继续增长。

## 现实逻辑对照

中国税收征管法第三十二条规定，未按期缴纳税款可按日加收滞纳金；国家税务总局公开说明的常见标准为税款万分之五。模组将该标准作为默认值，同时保留配置项，以便不同世界规则使用不同税制。

参考：

- [国家税务总局政策法规库](https://fgk.chinatax.gov.cn/zcfgk/c100009/c5234556/content.html)
- [中华人民共和国税收征收管理法](https://www.samr.gov.cn/zw/zfxxgk/fdzdgknr/bgt/art/2023/art_1dbea81872cb4fda9ab3ccc45c627531.html)

### tax-ledger-idempotency-2026-09-06.md

# 税务总账幂等性与完整性 - 2026-09-06

## 本次实现

- 税单写入按税单 ID 去重。
- 带来源事件 ID 的税单按来源事件去重，避免同一笔交易重复计税。
- 付款写入按付款 ID 去重，避免重复记录同一笔缴税。
- 读取旧存档时也会过滤重复税单、重复来源事件和重复付款记录。
- `TaxService.createBill` 在创建带来源事件的税单前再次检查来源，统一保护普通税单和交易税入口。

## 现实逻辑依据

税务系统需要把申报、税单和付款作为可追溯的唯一业务记录；同一交易或同一付款不能因为重试、服务器重启或网络重复提交而重复产生税负或缴款流水。

## 后续方向

- 为税务总账增加来源事件、税单和付款的审计查询命令。
- 继续检查跨系统退税、发票和进项抵扣在重复回调下的状态一致性。
- 后续统一税务事件的唯一 ID 生成规范，减少不同模块自行拼接来源字符串的差异。

### tax-ledger-mirrors-2026-09-05.md

# Tax ledger mirror progress log — 2026-09-05

## Implemented

- Added a unified event listener for corporate and individual-business tax settlement/delinquency events.
- Legacy `taxOwed` fields are now refreshed from the central tax ledger when those events occur, including when the owner is offline.
- Mirror conversion now uses major-unit ceiling conversion instead of integer truncation, so a non-zero minor-unit liability cannot appear as zero.

## Design boundary

- The tax ledger remains the only source of truth for liability amount, payment, and late fees.
- The company/business fields remain compatibility and gameplay-state mirrors; they do not create additional tax bills.

## Follow-up

- Audit all remaining legacy fields that represent balances already owned by a central ledger.
- Add direct tests for mirror synchronization with sub-unit liabilities and offline owners.

### tax-notifications-2026-09-05.md

# Tax notification progress log — 2026-09-05

## Implemented

- Added a dedicated persistent tax-notification store instead of reusing the refund-notification store.
- Delinquency notices now use a common tax notification service.
- Online players still receive immediate notices; offline players receive a queued notice when they next log in.
- Notice delivery is deduplicated by notice ID and capped at 1024 records.

## Design boundary

- The change affects notification delivery only. Tax rates, declaration deadlines, grace periods, and land auction rules are unchanged.
- The tax ledger remains the source of truth for liabilities; notifications are informational and cannot settle a bill.

## Follow-up

- Add a client-side tax-notification panel when the tax UI is revisited.
- Add focused tests for offline delivery, deduplication, and persistence migration.

### tax-payment-priority-2026-09-06.md

# 税款清偿顺序（2026-09-06）

## 本次完成

- 玩家钱包支付和拍卖/其他收益代扣的税款结算统一按账单到期日优先。
- 到期日相同时按创建时间和账单编号稳定排序。
- 不改变税率、税额或账单金额，只修正多账单并存时对清偿对象的选择。

## 现实逻辑对照

现实税务征收通常会优先处理已经到期或逾期的税款，避免纳税人通过支付新账单而长期留下旧逾期债务。本实现只处理模组账簿中的分配顺序；具体税款优先级仍可由服务器规则扩展。

## 后续方向

- 对税款本金、滞纳金和行政费用建立更细的分项清偿顺序。
- 在税务界面显示本次付款实际分配到的账单和金额。

### tax-policy-defaults-2026-09-05.md

# Tax policy defaults

## Implemented

- Added configurable defaults for every active transaction-linked tax category.
- VAT, general transaction tax, land-transfer tax, securities stamp duty, capital gains, dividends, resources and customs now have non-zero baseline rules.
- Inheritance/gift tax remains disabled by default because it requires a more complete estate and beneficiary model.
- Existing operator commands remain authoritative: `/taxrule set`, `/taxrule schedule`, `/taxrule reset` and `/taxrule list` can override or inspect the defaults.

## Policy boundary

These are gameplay defaults inspired by common tax categories and publicly documented reference rates; they are not a claim that one jurisdiction's complete tax law is being reproduced. Tax rates, thresholds, exemptions and effective dates remain server-configurable.

## Next direction

- Add tax-class distinctions for goods/services and input-tax credits instead of applying one VAT rate to every transaction.
- Add taxpayer registration and filing records before enabling more complex exemptions or inheritance tax.

### tax-refund-crash-recovery-2026-09-06.md

# 退税审批崩溃恢复与幂等投递 - 2026-09-06

## 本次实现

- 退税申请增加可恢复的 `PROCESSING` 与 `CREDIT_CONSUMED` 状态流转。
- 审批过程中先保存处理中状态，再消费进项抵扣，最后完成资金投递和批准状态更新。
- 服务器在消费抵扣后、投递前崩溃时，下一次审批可根据处理中记录继续完成。
- 退款通过带有 `tax-refund:<requestId>` 来源 ID 的邮箱入账，同一退税申请最多投递一次。
- 在线玩家仍会在审批完成时立即领取邮箱资金，离线玩家在下次登录时领取。
- 每日经济结算会自动扫描并恢复中断的退税申请；普通 `PENDING` 申请仍然需要人工审批。
- 退税审计日志现在区分 `PROCESSING`、`CREDIT_CONSUME`、`PAYOUT` 和最终 `APPROVE` 阶段，并按申请阶段幂等去重。

## 现实逻辑依据

税务退款属于需要结算和交付确认的支付业务，审批状态、抵扣扣减和资金交付不能只依赖一次内存调用。使用处理中状态和唯一支付来源，可以在重试时避免重复退款，并为未完成的支付保留恢复入口。

## 后续方向

- 增加后台定期扫描 `PROCESSING` / `CREDIT_CONSUMED` 退税申请的恢复任务。
- 在退税审计日志中分别记录抵扣消费、资金入账和最终批准三个阶段。
- 对邮箱来源记录增加查询接口，方便管理员核对每笔退税是否已经领取。

### tax-refund-persistence-validation-2026-09-06.md

# 退税请求持久化校验（2026-09-06）

## 本阶段实现

- 退款请求写入时校验 ID、纳税人 UUID、币种、状态和重复 ID。
- 退款请求替换时使用相同的关键字段校验，避免无效数据覆盖已有请求。
- 保留已拒绝请求允许零金额的历史兼容规则；正常申请仍必须为正金额且遵守待审请求限制。

## 现实逻辑对应

税务申请需要具备可追踪的申请号、纳税主体、币种和状态；同一申请号不能在登记簿中出现两次，否则会破坏审批、付款和审计链。

## 后续方向

- 将退款付款改为带来源号的可恢复结算流水。
- 增加存档加载后的无效历史请求修复报告。

### tax-refund-rules-tests-2026-09-06.md

# 退税规则回归测试（2026-09-06）

## 本阶段实现

- 增加退税规则层测试，覆盖无效币种、已有待审请求、可用抵扣不足、周期申请次数上限、自动批准和人工审核分界。
- 规则层新增纯参数重载，测试可以验证边界而不依赖 NeoForge 配置类；实际运行仍使用服务器配置值。

## 现实逻辑对应

退税通常需要先验证资格、金额、可抵扣余额和重复申请状态；小额、材料完整的申请可以自动处理，超出自动阈值的申请进入人工复核。

## 后续方向

- 为退款支付结算增加可恢复的状态流水。
- 将发票、进项抵扣、退款和冲正统一到同一来源事件链。

### tax-rule-enabled-state-2026-09-06.md

# Tax rule enabled-state enforcement

## Implemented

- The latest effective rule remains authoritative even when its `enabled` flag is false; `TaxRule.taxableBase()` then produces zero taxable base.
- Disabled rules therefore act as an explicit tax pause instead of silently falling back to an older enabled version.
- Disabled rules remain in history and can still be inspected for audit purposes.

## Real-world alignment

An annulled or disabled tax rule may remain in the legal/audit history, but it must not be used to assess new liabilities. The effective-rule query now separates those two concerns.

## Next direction

- Add regression coverage for future-effective, disabled, and re-enabled rule versions.
- Continue reviewing administrator operations for auditability and effective-date behavior.

### tax-subject-payment-allocation-2026-09-05.md

# 纳税主体多账单结算 - 2026-09-05

## 已实现

- `TaxService.pay(ServerPlayer, TaxSubject, amount)` 现在按同一主体、同一币种的未缴税单顺序分配支付金额。
- 支付前先汇总本次需要扣除的金额并检查钱包余额，避免第一张税单扣款成功、后续税单失败却返回“缴清”。
- 不同币种不会被混合支付；主体级支付以第一张未缴税单的币种为结算币种。
- 按账单 ID 支付的税务局入口保持原有行为，企业税仍由企业金库专用流程支付。

## 现实逻辑

一个纳税主体可能同时存在不同期间或不同税种的税单。主体级“缴纳全部”应形成可审计的逐单分配，而不能把一个总金额错误地记到第一张税单上；不同币种也必须分开结算。

## 后续方向

- 在税务界面显示本次支付分别分配到哪些账单和期间。
- 增加多币种税款的明确换汇/指定账单流程，而不是自动跨币种抵扣。

### tax-supporting-records-load-dedup-2026-09-06.md

# 税务凭证与退税存档去重 - 2026-09-06

## 本次实现

- 加载税务存档时按纳税人、币种和来源 ID 去重进项抵扣批次。
- 发票加载按来源事件 ID 去重，防止同一交易产生多个凭证。
- 退税申请加载按申请 ID 去重，避免重复申请进入待审核列表。
- 保留无来源的历史兼容抵扣批次，不误删无法分类的旧数据。

## 现实逻辑依据

发票、进项凭证和退税申请都是税务档案中的业务凭证；系统恢复或数据迁移不能因为同一凭证出现多份而扩大可抵扣额或重复退款资格。

## 后续方向

- 增加发票、抵扣批次和退税申请的审计查询入口。
- 对旧存档中抵扣总额与批次明细不一致的情况增加诊断报告，而不是静默修正金额。
- 继续检查退税审批完成后资金发放与状态更新之间的崩溃恢复边界。

### vat-credit-idempotency-2026-09-05.md

# VAT credit idempotency

## Implemented

- Tax-credit storage now persists processed VAT source-event IDs.
- A VAT output event that is fully offset by input credit cannot consume the same credit again if the transaction is retried.
- Input-credit creation is also idempotent by source event.
- The event marker list is bounded to 8192 entries to prevent unbounded growth.
- Older worlds without the new list load normally.

## Next direction

- Add explicit invoice records and tax-category metadata so input eligibility can be audited by item and supplier.

### vat-input-credit-2026-09-05.md

# VAT input credit

## Implemented

- Added a stable VAT tax subject per taxpayer, so multiple sales share one output-tax account instead of creating unrelated accounts.
- Supply-market sales create output VAT bills for the supplier.
- Company and active sole-proprietor purchases through the supply market create input-tax credits.
- Later output VAT consumes the seller's available input credits before creating a payable bill.
- Ordinary player purchases do not create deductible VAT credits.
- Existing tax-credit storage and refund allocation records are reused; no migration is required.

## Design boundary

- This is a simplified invoice/input-credit model: it does not yet model tax invoices, different VAT classes, exempt goods, or cross-border zero-rating.
- Input credits are currently limited to registered company and active sole-proprietor supply purchases.

### vat-invoice-ledger-2026-09-05.md

# VAT invoice ledger

## Implemented

- Added a persistent VAT invoice-like ledger for output and input transactions.
- Each record stores gross amount, assessed VAT, input credit applied, source event and direction.
- `/taxinvoices` displays the player's latest VAT records for audit.
- Fully credit-offset output transactions now still leave an auditable invoice record.
- Source-event deduplication is retained across reloads.

## Design boundary

- These are simplified game invoices, not a full legal invoice system. Supplier identity, item tax classes and invoice transfer are not yet modeled.