# Capitalism Mod - market-logistics progress log

Consolidated on: 2026-09-06

## Current system log

# 市场、供应链与物流系统开发进度

最后更新：2026-09-06

## 已实现功能

- 支持 B2B 供应市场、报价、采购订单、预付款和补发订单。
- 支持商品现货市场、限价订单、实物交割和交易记录。
- 支持仓库、库存所有者、仓库访问权限和仓储数据持久化。
- 支持贸易区域、运输方式、物流基础设施和物流数据。
- 支持采购、市场订单、交易邮件/通知及相关服务端事件。
- 市场交易和部分费用/收入已经具备接入税务与经济结算的基础。
- 已补齐商品市场和供应市场订单、价格、供需、K 线及前收盘价变更后的持久化标记，避免重启后回退到旧市场状态。
- 商品限价单现在支持可配置有效期（默认 30 个游戏日）；过期时会释放卖单库存或买单资金，离线玩家通过邮箱收取退款。
- 物流保险费改用精确的十进制向上取整，并在转换为最小货币单位前进行溢出检查；极端货值不会绕过支付校验。
- 供给市场解析存档中的物品 ID 时会安全拒绝非法值，不会因损坏数据导致服务端命令崩溃。
- 供应市场待补发订单现在记录原始单价和创建时间，支持可配置有效期（默认 30 个游戏日）；过期时按未交付数量退款到买方邮箱，旧订单因缺少历史字段会保留并交由管理员处理。
- 企业采购的物流保险费从企业金库支付，企业货物发生保险事故时赔款回到企业金库；个人货物仍由个人钱包和邮箱处理。
- 生产批次在质量放行前会从企业可交付库存中隔离；放行后才增加供给并触发待交付订单，返工和拒收不会绕过该门槛。

## 当前边界与待处理问题

- 价格发现、供需变化、库存成本和区域价差仍需要更系统的模型。
- 旧版本没有创建时间或历史单价的订单会被保留，不会被新有效期规则自动清理。
- 运输时间、运力、路线、损耗和保险尚未形成完整闭环。
- 供应市场、现货市场、仓库和企业生产之间仍存在进一步整合空间。

## 后续开发方向

1. 统一订单生命周期：创建、冻结库存、付款、运输、交付、取消和退款。
2. 建立区域供需、库存和运输成本驱动的价格模型。
3. 完善物流网络、运力、路线、损耗、保险和基础设施等级。
4. 将仓库和物流能力接入企业生产、土地用途和市场区域。
5. 增加交易异常、违约、延迟交付和争议处理。

## Consolidated historical entries

### commodity-config-migration-2026-09-06.md

# Commodity configuration migration

## Implemented

- Existing `commodities.json` files now receive missing maintained commodity IDs when the mod loads them.
- Existing custom initial prices remain unchanged.
- Newly introduced manufacturing components, including machine frames and electric motors, therefore become available to the commodity exchange in existing worlds.
- Invalid or custom commodity entries are preserved rather than replaced.

## Next direction

- Add explicit data versions for future removals or renames.
- Continue auditing shop, stock, and industry configuration loaders for the same additive-migration guarantee.

### freight-payable-close-guard-2026-09-06.md

# 运费应付款关闭校验 — 2026-09-06

## 本次实现

- 运费现金转移后现在必须成功关闭对应的资本化运费应付款。
- 应付款关闭失败时，会冲回承运方收款并返还买方付款。
- 避免出现“现金已转移、应付款仍未结清”的账务分叉，也避免后续重试再次付款。
- 供应商收入、VAT 输出税和买方进项抵扣仍只在应付款成功关闭后确认。

## 后续方向

- 为跨 SavedData 的运费结算增加持久化分阶段状态，覆盖服务器在现金操作中途停止的情况。
- 继续把承运合同、应付款和税务凭证统一到同一结算编号下。

### freight-settlement-cash-order-2026-09-06.md

# 运费结算现金顺序 — 2026-09-06

## 本次实现

- 运费结算现在先扣除买方企业现金，再向承运方企业入账。
- 买方扣款成功但承运方入账失败时，会立即冲回买方扣款。
- 避免原流程中先给承运方入账、随后买方扣款失败而产生临时重复现金或反向扣款失败。
- 只有双方现金操作成功后，才关闭运费应付款、承运合同并确认相关税务记录。

## 现实逻辑依据

企业间结算需要先确认付款方资金可用，再完成收款方入账和应付款关闭；运输服务的收入、VAT 和采购方进项抵扣应建立在结算成功的基础上。

## 后续方向

- 为运费结算增加持久化付款状态，进一步降低服务器中断时跨账本重试的风险。
- 将承运方报价、实际燃料成本和最终结算价差纳入物流成本报告。

### freight-settlement-phase-ledger-2026-09-06.md

# 运费结算阶段流水 — 2026-09-06

## 本次实现

- 新增持久化运费结算流水，记录买方扣款、承运方入账和应付款关闭三个阶段。
- 普通重试会复用已完成的阶段，不再从头重复付款。
- 应付款已经关闭但税务记录尚未补齐时，重试会补写合同状态、收入、VAT 输出税和进项抵扣；这些记录使用原有来源键保持幂等。
- 企业合并会同步迁移未完成和已完成的运费结算主体。
- 对异常的过期阶段标记增加校验，避免跳过实际应付款关闭。

## 当前边界

- 跨多个 SavedData 的单次原子提交仍受 Minecraft 存档写入机制限制；阶段流水已经覆盖正常重试和大部分中断恢复路径。
- 现金操作中途发生硬件级进程终止时，仍需要后续更细的账本对账任务进行最终核验。

## 后续方向

- 增加管理员可查看的结算对账报告，显示现金、应付款、合同和税务凭证的阶段差异。
- 将同一阶段流水模式推广到其他企业间应收应付和服务合同结算。

### fuel-transport-service-2026-09-06.md

# 燃料油运输服务 - 2026-09-06

## 已实现

- 运输行业新增可选配方 `fuel_transport`。
- 该服务每个生产周期消耗 1 份燃料油，产生 70 美元服务收入。
- 原有 `coal_transport` 保留为运输行业的默认配方，旧公司不会被强制切换。
- 企业可以通过现有的企业配方选择入口切换到燃料油运输。

## 现实逻辑

运输服务需要燃料、车辆/船舶和人工等持续投入。燃料油是炼油过程中的重质燃料产品，适合作为工业运输和部分船舶/重型设备的抽象燃料。本阶段只把燃料消耗纳入运输企业的经营循环，暂不把所有物流路线强制绑定到燃料油，以保持不同运输方式的可选性。

## 后续方向

- 为公路、铁路、海运等运输方式分别设置燃料类型、容量和风险。
- 将燃料消耗与物流订单的运输模式联动，而不是只影响运输企业的服务周期。
- 增加燃料价格对运输成本和运价的传导。

### logistics-arrival-delivery-idempotency-2026-09-06.md

# 物流到站交付幂等（2026-09-06）

## 本次完成

- 到站物流单入库改用 `logistics-delivery:<shipmentId>` 来源级一次性入货。
- 服务器在仓库入货和到站记录写入之间重启时，重试不会重复给玩家或企业仓库增加货物。
- 与供应订单同区交付共用仓库来源记录机制。

## 当前边界

- 仓库来源记录按有限数量保留，适用于近期崩溃恢复；长期审计仍由物流到站记录和供应交付记录承担。

### logistics-claim-payout-idempotency-2026-09-06.md

# 物流保险赔付幂等性（2026-09-06）

## 本次完成

- 玩家/离线玩家的货运保险赔付改用带业务来源 ID 的一次性入账接口，来源为 `logistics-claim:<shipmentId>`。
- 服务器在“邮箱已入账、理赔记录尚未保存”之间崩溃后，重试不会重复赔付。
- 赔付金额转换失败时保留运输单，等待后续结算，不再把未到账的理赔标记为已完成。
- 企业收货单在非零赔付无法写入企业金库时保留运输单；只有金库入账成功后才继续扣除库存成本并完成理赔。
- 企业保险赔付写入带来源标记的企业会计流水，重试时可识别已完成的同一理赔。

## 当前边界

- 运输、企业金库和邮箱仍不是同一个原子持久化事务；本次通过业务幂等键与失败重试降低重复结算风险。
- 供应市场订单退款仍有独立的“退款后记录”窗口，后续应复用同一类一次性入账机制。

### logistics-delivery-duplicate-guard-2026-09-06.md

# Logistics delivery duplicate guard — 2026-09-06

## Implemented

- Delivery processing now checks the persistent delivery ledger before crediting a warehouse.
- A shipment that has already been recorded as delivered is removed as a stale transport entry instead of being credited twice.
- Fuel planning records now reject a second plan for the same shipment ID.
- Fuel planning records reject invalid transport modes, missing fuel identifiers and negative prices/costs before they enter the persistent ledger.

## Accounting rationale

Delivery, freight capitalization and supply-order audit are separate persistent records. A retry can therefore encounter a completed delivery while the transport entry is still present. The shipment ID is the stable business key used to prevent duplicate inventory and duplicate fuel-plan records.

## Boundary

The guard reduces retry duplication across normal server restarts and stale entries. A fully atomic cross-SavedData transaction would require a broader persistence transaction layer and remains outside this phase.

### logistics-delivery-ledger-2026-09-06.md

# 物流送达流水账（2026-09-06）

## 本次完成

- 新增持久化的物流送达流水账，按运输单号记录已经入库的货物。
- 送达处理会先检查送达流水；如果运输单已经入库但因异常未及时删除，再次扫描时只删除残留运输单，不重复增加仓库库存。
- 送达记录保留有限数量，避免世界存档无限增长。

## 设计边界

送达记录在仓库入账完成后写入，已经覆盖“入账完成、运输单删除前”的常见重试窗口。跨存档写入的完全事务化仍需要后续将仓库变更和运输状态合并为同一结算流水。

### logistics-fuel-cost-ledger-2026-09-06.md

# 物流燃料成本台账（2026-09-06）

## 本次实现

- 跨贸易区域的供应运输现在会持久化燃料规划记录：运输单、货物、数量、路线、运输方式、燃料物品、燃料单位数、创建时价格和预计成本。
- 每次运输按实际分批创建对应的燃料规划，避免大订单只显示一条不准确的容量记录。
- 新增只读命令 `/logistics costs`，显示当前玩家最近 20 条燃料成本规划。
- 台账最多保存 4096 条记录，防止长期运行导致存档无限增长。

## 现实逻辑对照

现实物流报价会把运输方式、路线、载量和燃料价格转化为运输成本。先保存报价时点的燃料价格，可以避免事后燃料价格波动覆盖历史成本依据，也为以后接入运输企业、车队效率和燃油实际结算保留数据基础。

## 当前边界

- 本阶段是成本规划和审计记录，不扣除玩家或企业仓库燃料，也不改变订单付款金额。
- 尚未模拟车辆、船舶、车队所有权、空载率、燃油实际消耗和运输企业报价。

### logistics-fuel-metadata-2026-09-06.md

# 物流运输燃料元数据 - 2026-09-06

## 本次实现

- 道路运输关联汽油，铁路运输关联柴油，海运关联燃料油。
- `TransportMode` 新增按货量和贸易区距离计算的粗略燃料规划量。
- `/logistics` 在查看运输中货物时显示运输方式、燃料类型和规划用量。
- 规划用量目前只用于可视化和后续成本模型，不会重复扣除仓库燃料或改变订单结算。

## 设计边界

现实运输燃料消耗与车型、载重、航速、路线和空载率有关，不能用一个固定比例完全替代。本阶段保留运输方式差异和距离/货量两个主要变量，避免在没有车辆和船队实体的情况下伪造精确油耗。

## 后续方向

- 将燃料规划量接入物流报价和运输企业成本，而不是直接从买方仓库扣除。
- 按运输企业拥有的车辆、船舶和设备效率调整消耗。
- 增加燃料价格波动、储油设施和运输方式的排放/合规成本。

### logistics-fuel-plan-recovery-2026-09-06.md

# 物流燃料计划恢复（2026-09-06）

## 本次完成

- 物流运输单已经存在但燃料计划尚未保存时，交付阶段会根据运输单重新计算燃料用量、燃料价格和预计成本。
- 恢复后的燃料计划使用运输单 ID 作为幂等键，不会生成重复物流成本记录。
- 企业收货时仍会把恢复出的运输成本资本化到对应库存成本层。

## 当前边界

- 仓库扣货、运输单创建和订单状态更新仍不是单一事务；本次修复的是运输单与燃料计划之间的崩溃窗口。

### logistics-idempotency-2026-09-06.md

# 物流结算幂等性修复（2026-09-06）

## 本次完成

- 为货物损失记录增加按运输单号查询的幂等保护。
- 货损处理重试时，如果同一运输单已经记录为损失，不再重复扣减企业库存成本、记录损失费用或写入供应订单审计。
- 为保险理赔记录增加按运输单号查询的幂等保护。
- 保险赔付路径在执行赔付前检查理赔记录；如果运输单已经完成过理赔但尚未从运输列表删除，只删除残留运输单，不会再次赔付。
- 将理赔记录使用局部引用，保证本次处理检查和写入使用同一个持久化账本。

## 当前边界

本次解决的是服务器正常运行期间的重复处理，以及“赔付后运输单删除前”这类重试场景。真正跨文件保存的完全事务化仍需要后续把仓储入账、企业金库入账和运输状态统一为带状态的结算流水。

### logistics-loss-accounting-idempotency-2026-09-06.md

# Logistics loss accounting idempotency — 2026-09-06

## Implemented

- Added a persistent inventory-loss source ledger keyed by shipment ID.
- Repeated loss processing cannot record the same inventory impairment twice.
- Loss accounting is performed before the durable loss notification is written, so a retry can complete accounting after an interrupted attempt.
- Existing loss notifications remain deduplicated by shipment ID.

## Accounting rationale

Cargo loss recognition and the user-facing loss notification are separate records. The shipment ID is treated as the stable business event key; accounting can therefore be retried safely without creating a second impairment charge.

## Boundary

Supply-order audit events still use their existing append-only model; a future event-key layer can make every order event idempotent across persistence boundaries.

### logistics-loss-ledger-2026-09-05.md

# 物流损失记录与通知（2026-09-05）

## 本阶段已实现

- 未投保货物达到最大中断次数后，写入持久化物流损失台账，不再静默删除。
- 台账记录货运编号、买方、物品、数量、起运地、目的地、运输方式、中断次数和损失时间。
- 供应市场运输还保存采购订单号、采购单价和买方企业，损失可以回溯到具体订单。
- 玩家在线时立即收到损失通知；离线时在下次登录后收到通知。
- `/logistics losses` 查看当前玩家最近的损失记录。
- 已投保货物按 `min(实际损失, 保险金额)` 自动结算，并通过 `/logistics claims` 查询理赔记录。
- 企业货物损失按采购成本写入 `inventory_loss` 非现金账本记录，不重复扣除企业现金。
- 台账使用货运编号去重，最多保存 4096 条，避免世界存档无限增长。
- 新增供应订单生命周期台账，记录 CREATED、DISPATCHED、DELIVERED、BACKORDERED、PARTIAL、FULFILLED、EXPIRED_REFUND 和 LOST 事件；在途货物只有进入买方仓库后才算 DELIVERED。
- 新增 `/logistics order <订单号>` 查询订单完整事件链。

## 现实逻辑对应

运输合同中，货物灭失需要留下可审计的运输事故记录；是否赔付由保险合同和保险状态决定。赔偿遵循实际损失不超过保险金额的原则。未投保货物不会自动返还货款或货物，已投保货物则记录保险金额、实际损失和最终赔付额。

## 资料依据

- 《中华人民共和国保险法》关于保险合同、实际价值和赔偿责任的规定：
  https://www.samr.gov.cn/zw/zfxxgk/fdzdgknr/bgt/art/2023/art_4c715a53f3d4402c89f62ae77809f638.html

## 后续方向

- 增加基于运输单据的索赔/理赔申请流程，而不是只依赖自动结算。
- 按运输方式、基础设施和货物类型细化风险与免赔额。
- 区分买方库存损失、卖方履约损失和承运人责任，并支持保险公司的代位追偿。

### logistics-loss-limits-2026-09-05.md

# Logistics disruption limits

## Implemented

- Shipments now persist their disruption count.
- Uninsured shipments are retried only up to the configurable `logisticsMaxDisruptions` limit (default 3).
- Insured shipments continue to settle through the declared-value payout path on a disruption.
- Old shipment records load with a disruption count of zero.
- This prevents permanently delayed cargo from accumulating in the logistics queue.

## Next direction

- Add a persistent cargo-loss/claims ledger and buyer notifications.
- Add transport-specific risk and insurance pricing after route and infrastructure data are richer.

### logistics-node-backfill-2026-09-06.md

# 物流节点旧存档回填 - 2026-09-06

## 已实现

- 新增管理员命令 `/logistics repair [radius]`。
- 命令扫描执行者所在维度、当前位置周围的已加载区块，并识别物流中心、转运站和港口。
- 半径默认为 `0`（只扫描当前区块），允许范围为 `0–8`；扫描范围受到明确限制，避免一次操作意外遍历整张地图。
- 已发现的设施写入 `LogisticsNodeSavedData`，之后会正常进入世界地图物流节点覆盖层并持久化保存。
- 已有节点不会重复登记，命令可以安全重复执行。
- 地图同步时会校验已加载区块中的节点方块；确认方块已被替换或移除的失效记录会自动清理，未加载区块不会被误删。

## 设计依据

旧存档兼容扫描不应绑定到普通区块加载流程。物流设施扫描涉及整个建筑高度，若每次加载区块都执行，会把存档兼容成本转化为持续的加载性能成本。因此采用管理员主动、范围受限、可重复的回填操作，更接近运维/数据迁移工具的职责。

## 使用方式

- `/logistics repair`：扫描当前位置所在区块。
- `/logistics repair 2`：扫描当前位置周围 5×5 个已加载区块。

## 后续方向

- 在世界地图管理入口中增加“修复当前区域”的按钮，并沿用同样的范围限制。
- 为节点补充有效性校验：如果持久化节点对应位置的方块已被外部方式替换，可在修复或地图同步时清理失效记录。

### logistics-shipment-idempotency-2026-09-06.md

# 物流货运单幂等性 — 2026-09-06

## 本次修复

- 物流持久化台账现在要求货运单号非空且全局唯一。
- 重复插入同一货运单号会被忽略，不会产生第二票运输记录。
- 加载旧存档时也会过滤空单号和重复货运单，避免历史脏数据恢复成重复运输。
- 该保护覆盖供应订单、跨区域运输和其他直接使用物流台账的入口。

## 技术原因

供应订单的库存扣减、运输记录、收款和订单进度属于多个持久化步骤。服务器可能在步骤之间重启，因此底层台账必须具备幂等保护，不能只依赖调用方的执行顺序。

## 后续方向

- 继续把供应订单的配送分批键与货运单号建立显式映射。
- 为运输记录补充状态转移日志，使“已发运、运输中、已签收、损失、理赔”能够完整审计。

### market-order-priority-2026-09-05.md

# Market order priority progress log — 2026-09-05

## Implemented

- Commodity orders now persist their creation game time.
- Matching uses price priority first, then time priority, then order ID as a deterministic tie-breaker.
- Existing orders without the new field load with `createdAt = 0`, preserving them as older orders.
- Partial fills retain the original creation time.

## Design boundary

- No prices, fees, escrow amounts, or order expiration rules were changed.
- This only makes the existing limit-order matching rule deterministic and closer to a normal price-time order book.

## Follow-up

- Add explicit order expiration and an auditable cancellation/refund event stream.
- Add market tests for same-price FIFO matching and legacy order decoding.

### shop-stock-config-migration-2026-09-06.md

# Shop and stock configuration migration

## Implemented

- Existing shop configuration files now receive missing maintained offers keyed by item and currency.
- Existing custom shop prices and quantities remain unchanged.
- Existing stock configuration files now receive missing maintained stock IDs without replacing custom listings.
- The migration keeps reserved/removed stock filtering in place.

## Next direction

- Add explicit data versions if a future release needs to remove or rename configured entries.
- Continue the same compatibility audit for tax rules and other data-driven systems.

### supply-company-purchases-2026-09-05.md

# 企业采购资金与库存成本（2026-09-05）

## 已实现

- 企业作为供应市场买方时，采购款由企业美元金库支付，不再从发起采购的玩家钱包扣除。
- 采购付款写入企业账本，并进入企业税务费用凭证和增值税进项抵扣流程。
- 不存在、暂停、清算或已解散的企业不能继续发起新的供应采购。
- 个人采购仍沿用个人钱包支付逻辑。

## 设计依据

采购原料属于企业取得存货的成本，企业采购和个人消费应当分离；生产时再从企业仓库消耗原料，销售时形成收入。该流程与 IAS 2 对存货采购成本和后续销售成本确认的基本区分一致。

参考：[IFRS IAS 2 Inventories](https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/)。

## 后续方向

- 将采购订单、库存取得成本和生产批次关联，进一步计算产品单位成本。
- 对部分交付、运输损失和退货建立存货成本结转规则。

### supply-delivery-progress-2026-09-06.md

# 供应订单发运进度账本（2026-09-06）

## 本次完成

- 新增持久化的供应订单发运进度账本。
- 每个订单的当前剩余量作为批次键，记录该批次已经发运的数量和发运后的剩余量。
- 履约重试时，如果批次已经发运，系统会恢复订单剩余量，不再重新扣除供应商库存或重复创建运输批次。
- 正常履约与恢复履约共用同一订单状态更新逻辑，避免订单停留在旧的剩余数量。

## 现实逻辑对应

现实采购订单通常会区分“已下单、部分履约、已发运、已收货、已完成”和“待退款”等状态。本次先把补货发运与订单剩余量建立持久化关联，为后续进一步拆分“发运、到货、验收、结算”状态打基础。

## 设计边界

发运进度在货物已经扣除并创建发运记录后写入，覆盖“发运完成、订单状态更新前”的常见重试窗口。跨多个 SavedData 的严格事务仍需要后续统一资金、库存、运输和订单状态的事务流水。

### supply-dispatch-recovery-2026-09-06.md

# 供应订单出货恢复（2026-09-06）

## 本次完成

- 跨区运输单 ID 改为由订单交付来源和批次索引稳定生成，不再依赖随机 UUID。
- 供应订单重试时，如果对应运输单已经创建但交付日志尚未保存，会识别已有运输单并跳过再次扣除供应商库存。
- 运输单、燃料计划和供应交付日志现在可以通过同一交付来源恢复。
- 即使重启后供应商仓库库存已经扣减为不足，也会根据已存在运输单恢复实际出货数量，不会让订单永久卡在待发货状态。
- 保持物流成本、供应商付款和订单进度的既有幂等保护。

## 当前边界

- 同区即时交付仍缺少独立的出货意图记录；后续需要为仓库直接交付增加同样的两阶段出货状态。

### supply-escrow-settlement-2026-09-06.md

# 供应订单按交付结算 - 2026-09-06

## 已实现

- 买方仍在下单时全额预付，保证订单资金已经锁定。
- 供应商只会收到已经从库存中取出并交付/发运部分的货款。
- 未交付余量的货款继续留在订单资金中，订单过期时退还买方。
- 部分补发会按本次实际交付数量释放供应商货款，并分别记录销售税和企业收入。
- 修复了“供应商已收到整单货款、订单过期后买方又获得退款”的资金凭空增加问题。

## 现实逻辑

预付款交易通常需要交付条件或托管安排：卖方不能把尚未交付部分同时当作已完成销售，买方也不应在卖方保留未交付货款的情况下再次获得退款。本阶段使用简化的订单托管模型，按发货/交付批次释放货款。

## 当前边界

- 跨区域运输目前在发运时释放货款，运输途中损失仍由现有物流保险和损失规则处理。
- 后续可以增加到货验收、争议期、取消订单和不同贸易术语（例如发运交货与到货交货）。

### supply-inventory-settlement-guard-2026-09-06.md

# 供应链库存结算一致性 — 2026-09-06

## 本次实现

- 即时采购和补交订单在发货前都会检查仓库实际扣除是否成功。
- 扣库失败时不再创建物流、库存成本、供应商销售收入、税务或订单进度记录。
- 这样可以避免库存快照与实际库存不一致时出现凭空增货或重复结算。

## 当前边界

- 结算仍在服务器主线程内执行，仓库扣除是原子操作。
- 物流运输过程继续使用既有的交付幂等记录，避免到达结算重复入库。

## 后续方向

- 继续审计供应链订单、物流损失、保险赔付与税务冲正之间的状态转换。
- 后续可将订单状态整理为明确的“已付款、部分交付、运输中、已完成、已取消、已损失”状态机。

### supply-local-delivery-recovery-2026-09-06.md

# 供应订单同区交付恢复（2026-09-06）

## 本次完成

- 仓库增加基于来源 ID 的一次性入货接口。
- 同区供应订单交付使用 `supply-local-delivery:<dispatchKey>` 作为幂等来源。
- 服务器在仓库已入货、交付日志尚未保存时重启，重试不会重复增加仓库库存。
- 同区订单重试时会读取来源对应的已入货数量，即使供应商库存已不足，也能继续写入交付日志、付款和订单进度。
- 物流运输单到站入库也使用运输单 ID 作为来源，避免“仓库已入货、到站记录未保存”时重复交付。
- 旧仓库数据保持兼容，新增来源记录作为可选持久化字段加载。

## 当前边界

- 仓库来源记录设有有限保留窗口；其设计目标是防止近期崩溃重试，而不是无限期保存全部历史来源。

### supply-online-payment-recovery-2026-09-06.md

# 供应商在线付款恢复（2026-09-06）

## 本次完成

- 在线供应商付款不再直接写入玩家钱包，而是先进入持久化市场邮箱，再立即兑换给在线玩家。
- 供应商下线时继续保留在邮箱；供应商在线时只兑换金钱，不影响其待领取的物品。
- 服务器在“付款已入邮箱、结算记录尚未保存”之间崩溃时，来源 ID 能防止重试重复付款。
- 新增只兑换金钱的邮箱接口，避免在线供应商付款意外领取其他业务的物品。
- 企业供应商收款改用带 `supply-sale:<sourceId>` 来源的经营收入入账；重试时会从企业会计流水识别已经入账的销售收入。

## 当前边界

- 企业税务收入记录与企业金库流水仍分属不同 SavedData，虽然两者都使用同一销售来源 ID，后续可继续统一为带状态的企业结算流水。

### supply-order-audit-event-idempotency-2026-09-06.md

# Supply-order audit event idempotency — 2026-09-06

## Implemented

- Supply-order audit events can now carry an optional stable event key.
- Delivery and loss events use the transport shipment ID as that key.
- Retrying the same shipment cannot add a second `DELIVERED` or `LOST` event to the order history.
- Existing audit records without event keys remain readable and valid.

## Accounting rationale

An order may be fulfilled through multiple shipments, so deduplicating only by order ID would incorrectly discard legitimate partial deliveries. The shipment ID distinguishes legitimate batches while making retries idempotent.

### supply-order-cancellation-2026-09-06.md

# 供应订单取消与退款（2026-09-06）

## 本阶段实现

- 新增 `/logistics cancel <订单ID>`，买方可以取消仍处于待补货状态的订单。
- 只退还尚未交付的数量；已经即时交付或已经发运的部分不会被重复退款。
- 公司采购退款回到公司金库，个人采购退款进入玩家邮箱。
- 同步冲回与未交付部分对应的进项税抵扣。
- 记录 `CANCELLED_REFUND` 生命周期事件，并使用持久化结算键避免重复退款。
- 订单状态解析会将取消退款显示为 `CANCELLED`，不会继续显示为待补货。

## 现实逻辑对照

采购合同取消通常需要区分已履约和未履约部分。供应商已经发运的货物不应因为买方取消待补货余额而被重复撤销；尚未履约的预付款则应按合同条件退回。本阶段只开放尚未发运的待补货余额取消，暂不模拟违约金和供应商争议。

## 后续方向

- 增加供应商接受取消、不可取消窗口和合同违约金。
- 增加到货验收、拒收、退货和争议期状态。

### supply-order-status-2026-09-06.md

# 供应订单状态解析（2026-09-06）

## 本次完成

- 新增供应订单当前状态解析，不再要求玩家自行从事件列表推断订单进度。
- 支持以下状态：`PLACED`（已下单）、`BACKORDERED`（待补货）、`IN_TRANSIT`（运输中）、`RECEIVED`（买方已收货）、`LOST`（运输损失）、`REFUNDED`（过期退款）和 `SUPPLIER_FULFILLED`（供应商已履约但缺少收货事件）。
- `/logistics order <订单号>` 现在会先显示当前状态，再显示完整事件流水。
- 远程运输中，即使供应商已经发出全部货物，也会显示为 `IN_TRANSIT`，直到物流系统记录送达后才显示 `RECEIVED`。

## 现实逻辑对应

现实采购订单通常将供应商履约、发运、到货和买方收货分开记录。本次先在现有事件账本上增加状态解析层，不改变既有订单和物流数据格式，为后续增加验收、拒收和争议处理保留接口。

### supply-order-tax-credit-overflow-2026-09-06.md

# 供应订单进项税额比例计算 — 2026-09-06

## 本次修复

- 供应订单取消或过期时，未交付部分的进项税额现在使用 `BigInteger` 做比例计算。
- 大额订单不再因 `long` 乘法溢出而错误地退回全部进项税额。
- 结果按未交付数量/原始数量向下取整，并限制不超过原始进项税额。
- 新增大额订单和普通订单的单元测试。

## 现实逻辑对照

取消或退货通常只冲回对应未履约部分的进项税额，而不是把整笔订单的税额重复返还。游戏中退款金额、未交付数量和税额冲回现在保持同一履约口径。

## 后续方向

- 将供应订单税额、退款、发票和交付批次统一显示在订单审计记录中。

### supply-quality-disclosure-2026-09-06.md

# 供应报价质量规格 - 2026-09-06

## 本次实现

- 供应报价新增 `qualityScore` 字段，记录挂牌时供应商该商品的平均质量分。
- 采购界面显示质量分；没有质量台账的旧库存显示 `quality:unverified`。
- 下单后，采购订单保存当时的质量规格快照；分批交付期间该规格不会随供应商新批次变化。
- `/logistics order` and `/marketorders` now expose the order quality specification for audit and troubleshooting.
- 旧存档中的供应报价通过可选字段兼容，缺失值默认为 0。
- 质量目前只作为采购信息披露，不改变价格、付款、物流、订单接受或结算规则。

## 设计依据

现实 B2B 采购通常会在报价或规格书中披露质量、认证或检验信息，但报价质量规格不等于最终验收结果。因此本阶段只展示供应商的历史生产质量快照，后续再把到货批次、检验和退货分开建模。

## 后续方向

- 为订单保存质量规格快照，避免供应商后续生产质量变化影响历史订单记录。
- 增加到货抽检、验收、不合格品和退货流程。
- 将质量等级接入电子产品良率、保修和售后维修，而不是直接硬编码价格倍率。

### supply-refund-accounting-2026-09-06.md

# Supply order refund accounting

## Implemented

- Expired backorders now refund the original payer's account type.
- Orders paid from a company treasury are refunded to that company's treasury.
- Personal orders and legacy orders without a valid company continue to refund the buyer's mailbox.
- The supply-order audit distinguishes company-treasury refunds from personal mailbox refunds.
- Unused VAT input credit is proportionally reversed when an undelivered part of a prepaid order expires, with a separate tax-invoice audit entry.

## Real-world alignment

A business refund returns to the business bank account or ledger that paid the invoice; it should not become a personal payment to the owner merely because the owner placed the order.

## Follow-up

- Add an explicit tax-credit reversal record when a prepaid business order expires before delivery.
- Continue auditing shipment loss, insurance payout, and company treasury ownership transitions.

### supply-settlement-ledger-2026-09-06.md

# 供应订单结算账本（2026-09-06）

## 本次完成

- 新增持久化的供应订单结算账本。
- 供应商收款按结算来源号去重，补货履约重试时不会重复给供应商付款，也不会重复登记供应商收入。
- 订单过期退款按订单号去重；如果退款已完成但订单尚未删除，下一次扫描只清理残留订单。
- 结算账本限制记录数量，避免长期运行的世界存档无限膨胀。

## 设计边界

结算键在资金操作完成后写入，覆盖“付款/退款完成、订单状态更新前”的常见重试窗口。若要做到跨多个 SavedData 的严格事务，还需要后续统一资金、库存和订单状态的事务流水。

### supply-settlement-mailbox-idempotency-2026-09-06.md

# 供应市场邮箱结算幂等性（2026-09-06）

## 本次完成

- 订单过期退款和玩家主动取消退款，改用 `creditMoneyOnce`，业务来源分别绑定到订单号。
- 离线供应商收款改用供应商付款来源 ID，服务器在邮箱入账后、结算记录保存前崩溃时不会重复支付。
- 供应市场的退款和离线供应商付款现在与物流保险赔付使用统一的邮箱幂等机制。
- 企业买方退款改用带来源标记的企业金库入账接口；重试时会通过企业会计流水识别已经完成的退款。

## 当前边界

- 企业供应商收款和在线玩家即时收款仍依赖余额与结算记录的跨 SavedData 写入；这些路径后续需要统一的结算流水层，才能进一步缩小跨文件崩溃窗口。
- 本阶段没有改变退款金额、税额抵扣或订单状态逻辑，只补强重复结算保护。