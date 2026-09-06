# Capitalism Mod - bank-currency progress log

Consolidated on: 2026-09-06

## Current system log

# 银行与货币系统开发进度

最后更新：2026-09-05

## 已实现功能

- 支持 USD、CNY、EUR、RUB 等多种货币和最小货币单位精确存储。
- 支持货币兑换、汇率查询和汇率服务。
- 支持借记账户、信用账户、活期存款和定期存款。
- 支持开户、存款、取款、转账、贷款、还款、利息结算和银行交易记录。
- 支持银行卡号、银行卡物品和账户相关网络同步。
- 货币、钱包、个人资产和企业资金已经具备统一经济数据基础。
- 账户间转账现在会在扣款前校验手续费、总扣款和收款账户余额是否溢出，避免扣款成功后无法安全入账。

## 当前边界与待处理问题

- 汇率目前更偏向基础服务，宏观经济和市场供需对汇率的影响还不充分。
- 信用额度、风险评估、抵押物、逾期处理和银行流动性尚需完善。
- 账户、钱包、企业金库和交易结算之间需要继续统一审计口径。

## 后续开发方向

1. 建立统一的资金转移事务，保证扣款、入账、失败回滚和日志原子性。
2. 增加信用评分、授信、抵押、逾期、坏账和银行风险规则。
3. 让汇率与贸易、库存、货币供给和跨区域交易产生可解释的联动。
4. 增加银行对账单、手续费、准备金和审计数据。

## Consolidated historical entries

### bank-cash-change-2026-09-06.md

# 现金支付与找零（2026-09-06）

## 本次实现

- 修复大面额货币支付小额金额时“余额足够但实际没有扣款”的问题。
- 现金支付现在可以先扣除足额纸币/硬币，再将多出的部分作为找零返还。
- 如果物理货币无法组成有效支付方案，则不会伪造成功，而是继续尝试从银行账户支付。
- 支付成功后才写入钱包变更事件和经济日志。

## 现实逻辑对应

现金交易的支付额与交付面额可以不同，差额通过找零结算；交易不能因为名义余额足够就跳过实际交付。

## 后续方向

- 为银行转账、现金支付和账户扣款统一建立可追溯的资金转移凭证。
- 增加兑换手续费、现金库存和银行流动性对找零能力的影响。

### bank-credit-risk-2026-09-05.md

# Bank credit-risk model

## Implemented

- Added a deterministic `CreditAssessment` derived from existing bank-account data, so old saves need no migration.
- The assessment considers debt utilization, repayment entries, and overdue status.
- New bank loans are checked against the score-adjusted approved limit.
- An overdue credit account cannot take additional loans until its debt is cleared.

## Design basis

The model follows common real-world underwriting concepts: repayment history, amount owed relative to available credit, and the borrower’s capacity to repay. It is intentionally a game abstraction rather than a jurisdiction-specific legal credit score.

## Next direction

- Add stable income/cash-flow evidence from wallet and company distributions.
- Add collateral and structured default/recovery handling before introducing more complex loan products.

### bank-interest-precision-2026-09-05.md

# Bank interest precision progress log — 2026-09-05

## Implemented

- Bank accounts now persist fractional interest remainders for both deposits and credit debt.
- Daily settlement carries values smaller than one minor currency unit into later days instead of discarding them.
- Remainders are cleared when the corresponding balance or debt reaches zero, preventing old accruals from leaking into a later position.
- The new fields are optional in the codec, so existing bank-account data loads with zero remainders.

## Design boundary

- Annual rates, overdue multipliers, settlement cadence, and term-deposit rules were not changed.
- Account balances and debts remain integer minor units; only the unposted fractional interest is persisted separately.

## Follow-up

- Added regression tests for remainder retention, the legacy constructor defaults, and clearing a balance/debt.
- Codec compatibility remains implemented through optional fields; the current unit-test classpath does not expose Minecraft's Codec runtime.
- Bank, transaction, and term-deposit codecs are now lazily initialized, allowing pure account tests to load without eagerly starting the data-fixer runtime.
- A future test pass should cover full daily settlement with small balances and partial repayment.
- Review whether bank transaction history should expose accumulated-but-not-yet-posted interest.

### bank-interest-remainder-persistence-2026-09-06.md

# 银行利息小数余数持久化（2026-09-06）

## 本次完成

- 每日结算只要账户存在正余额，就会持久化尚未达到最小货币单位的利息余数。
- 修复小额存款在连续多个结算日中因没有产生整单位利息而丢失小数累积的问题。
- 不改变年利率、日复利方式或已有账单/交易记录格式。

## 现实逻辑对照

银行利息计算通常保留内部精度，再按结算货币的最小单位入账；内部舍弃全部小数会使长期收益产生系统性偏差。本实现使用持久化余数模拟银行内部精度。

## 后续方向

- 为利息余数增加独立的结算审计字段，便于在银行界面解释累计过程。
- 对贷款利息余数和存款利息余数提供统一的对账视图。

### bank-settlement-idempotency-2026-09-05.md

# Bank settlement idempotency

## Implemented

- Bank interest and term-deposit settlement now receives an explicit Minecraft settlement day.
- The player's persisted settlement-day attachment prevents duplicate interest or duplicate term-deposit ticks when the same day is requested more than once.
- Offline catch-up still applies each missed day in order when the player logs in.
- Players with no bank accounts still advance their settlement marker, avoiding repeated empty settlement work.
- A zero transfer-fee rate now genuinely disables the fee instead of charging one minimum minor unit.
- Transfer principal and transfer fee are recorded as separate sender-side statement entries.

## Real-world alignment

An account statement should not contain two daily accruals merely because two application events were delivered. The settlement day is therefore treated as an idempotency key, while the existing transaction list remains the user-visible audit trail.

## Next direction

- Continue reviewing transfer, repayment, and company-treasury paths for atomic debit/credit behavior.
- Add a proper bank statement/reporting layer before introducing more complex financial products.

### bank-settlement-transaction-dates-2026-09-06.md

# 银行补结算流水日期修复 - 2026-09-06

## 本次实现

- 新增按指定游戏 tick 创建银行交易流水的接口。
- 存款利息、贷款利息和定期存款到期流水现在使用对应的结算日，而不是服务器当前补结算时刻。
- 保留普通存取款、换汇、转账等实时操作使用当前游戏时间的行为。
- 增加单元测试，验证历史结算流水保留指定结算 tick。

## 现实逻辑依据

银行在补记息或追补结算时，应把利息归属到实际计息期间，而不是把所有历史利息伪装成客户刚登录时产生的交易。这样账户流水才能正确支持对账、审计和按日期统计。

## 后续方向

- 在银行查询界面显示结算日期、利息归属日和当前生成时间的区别。
- 为补结算记录增加批次标识，便于服务器回档或重复结算时审计。
- 继续检查定期存款、贷款到期和银行交易历史在跨日补结算时的一致性。

### bank-statement-reporting-2026-09-06.md

# 银行对账报表 - 2026-09-06

## 已实现

- 新增只读命令 `/bankstatement <accountId>`。
- 报表显示账户类型、各币种余额、债务余额、现有流水的币种收支汇总，以及最近 10 笔交易。
- 报表直接读取账户持久化流水，不创建新的资金变动，不会重复计息、扣款或转账。
- 汇总使用饱和加法，避免极大数值造成溢出。
- 新流水增加结算 tick 和来源参考字段；旧流水以 legacy 标识，不伪造历史日期。
- 转账、现金存取款等新流水增加对手方字段；旧流水显示为 unknown。
- `/bankstatement <accountId> [days]` 支持按最近 1–360 个游戏日筛选新流水。
- 银行 UI 的最近流水页同步显示结算时间，旧记录显示为 legacy。

## 现实逻辑

银行对账单通常同时提供期初/期末余额、借方/贷方发生额、费用和交易明细。本阶段按游戏世界时间记录结算日和来源；旧版本流水没有时间戳，因此期间筛选会排除旧流水，而保留历史模式仍可查看它们。

## 后续方向

- 继续补充费用分类、交易渠道和企业资金账户的对账视图。
- 将完整期间报表接入银行 UI，并增加导出/分页显示。
- 为企业资金账户建立同样的银行报表视图。