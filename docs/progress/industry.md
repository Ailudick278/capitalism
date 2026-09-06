# Capitalism Mod - industry progress log

Consolidated on: 2026-09-06

## Current system log


## Consolidated historical entries

### electric-appliance-chain-2026-09-06.md

# 家用电器生产链 - 2026-09-06

## 本次实现

- 新增消费品：电风扇 `electric_fan`。
- 配方：电动机 + 钢板 + 铜线 + 机器框架，经装配线生产。
- 电风扇加入商品市场、企业生产和创造模式物品栏。
- 装配线继续复用到电灯、罐装食品、木箱、家具和电风扇，体现通用装配设备可生产多类成品。

## 现实依据与抽象边界

ENERGY STAR 对风扇组件的说明将电机、外壳、叶片和控制列为影响产品性能的关键部分；本阶段把叶片、外壳细节和控制器合并到成品工艺中，使用现有机架、钢板和电机表达主要物料消耗，后续再根据玩法需要拆分叶片、开关和电容等零部件。

参考资料：

- https://www.energystar.gov/products/ceiling_fans/performance_components_count
- https://www.energystar.gov/products/ceiling_fans/accessories_purchasing_choices

## 后续方向

- 增加塑料/橡胶等非金属材料后，再细分叶片与绝缘件。
- 增加电器质量、能效和售后维修指标，让同一成品存在不同质量档次。
- 将家电需求接入居民消费与零售库存，而不是只作为企业库存商品。

### electronics-battery-chain-2026-09-06.md

# 电子消费品电池链 - 2026-09-06

## 已实现

- 新增 `battery_cell` 电芯中间件。
- 电芯由铁、铜和红石等简化材料在电子装配线上生产，每次产出 2 个。
- `battery` 现在由电芯、铜线和塑料粒料进行电池组件装配。
- 智能手机继续使用 `battery`，因此自动接入新的电池上游链条。

## 现实依据与抽象边界

现实中的电池生产通常区分电芯制造和电池包/模组装配；电芯包含电极、集流体、隔膜和电解质，成组后还需要外壳、连接和保护电路。本阶段把这些工艺压缩为“电芯”和“电池组件”两个可管理节点，保留产业链关系，但不引入化学配方和危险品处理的复杂系统。

## 后续方向

- 将电芯进一步拆分为正极材料、负极材料、隔膜和电解液。
- 增加电池容量、循环寿命与产品质量差异，再决定是否影响电子产品售价和售后维修。

### electronics-camera-module-2026-09-06.md

# 摄像头模组生产环节 - 2026-09-06

## 已实现

- 新增 `camera_module` 摄像头模组零部件。
- 摄像头模组由玻璃镜片、电路板和塑料粒料，在电子装配线上生产。
- 智能手机总装现在额外需要摄像头模组。
- 摄像头模组加入商品目录、创造模式物品栏、模型和本地化文本。

## 现实逻辑

摄像头模组不是单独一片镜片，而是由镜头、图像传感器/电路和结构件组成的子组件，通常先完成模组装配，再安装到手机主板和机壳中。本阶段用现有镜片、电路板和塑料结构件表达这一层级关系。

## 后续方向

- 增加扬声器、麦克风和充电接口等通用电子零部件。
- 将手机产品质量、摄像头规格和售后维修纳入企业经营指标。

### electronics-display-module-chain-2026-09-06.md

# 电子显示模组产业链 - 2026-09-06

## 本次实现

- 将显示面板拆分为显示玻璃基板、背光模组、显示驱动芯片和塑料材料。
- 显示玻璃基板由玻璃在玻璃熔炉中加工。
- 背光模组由封装芯片、铜线和塑料粒料在电子装配线上生产。
- 显示面板由显示玻璃基板、背光模组、显示驱动芯片和塑料粒料在电子装配线上完成。
- 对旧版默认 `display_panel` 配方增加精确匹配迁移；自定义配方不会被覆盖。
- 将迁移判断抽成纯逻辑并加入单元测试，覆盖默认配方、改动配方和自定义配方三种情况。

## 现实依据

- Corning 说明显示玻璃基板需要高平整度和厚度控制，并用于电视、笔记本和移动设备。
- LCD 的显示堆栈包含玻璃基板、背光和电子控制层。
- BOE 将 LCD 工艺拆分为阵列、彩膜、成盒和模组，模组阶段连接驱动电路并安装背光。

## 后续方向

- 进一步拆分 TFT 背板、彩膜基板、导光板和扩散片，并加入切割损耗/良率。
- 按屏幕尺寸与技术路线区分手机、电视和笔记本的成本。

## 参考资料

- Corning, Display Glass Manufacturing Technology: https://www.corning.com/worldwide/en/products/display-glass/how-it-works.html
- Corning, The Glass Stack: https://www.corning.com/emea/en/products/display-glass/the-glass-stack.html
- BOE, LCD manufacturing process: https://www.boe.com/global/en/Investor.html

### electronics-display-panel-2026-09-06.md

# 电子消费品显示面板 - 2026-09-06

## 已实现

- 新增 `display_panel` 显示面板中间件。
- 显示面板由玻璃、硅晶圆、铜线和塑料粒料，在 `electronics_assembly` 电子装配线上生产。
- 智能手机现在使用显示面板，不再直接把玻璃镜片当作屏幕；玻璃镜片仍可服务于照明产品链。
- 显示面板加入商品目录、创造模式物品栏、本地化文本和物品模型。

## 现实依据

显示器件通常需要基板、半导体/导电层以及后续封装和装配。DOE 的显示制造资料将显示玻璃作为关键基板，并描述了透明导电层等后续工艺；NIST 对半导体封装基板也将玻璃、半导体和有机材料列为基板类别。因此在游戏中把显示面板作为独立中间件，比直接使用普通玻璃或镜片更接近现实产业链。

参考资料：

- [U.S. Department of Energy：OLED integrated substrate manufacturing](https://www.energy.gov/cmei/buildings/articles/manufacturing-process-oled-integrated-substrate)
- [U.S. Department of Energy：SSL manufacturing status and opportunities](https://www.energy.gov/sites/default/files/2022-02/2022-ssl-manufacturing-status-opportunities_0.pdf)
- [NIST：National Advanced Packaging Manufacturing Program](https://www.nist.gov/document/napmp-nofo-2)

## 后续方向

- 再拆分电池正负极材料、隔膜和电解液，形成更细的电池产业链。
- 增加显示面板良率、封装和售后维修等经营差异，但暂不改变现有核心玩法。

### electronics-lithium-battery-chain-2026-09-06.md

# 锂离子电池产业链 - 2026-09-06

## 本次实现

- 新增锂矿精矿、电池级碳酸锂、石墨负极材料、正极活性材料、电池隔膜和电池电解液。
- 新增电池材料处理设备 `battery_materials` 与电芯生产线 `battery_cell_line`。
- 新增基础路线：
  `石英（矿物代理） → 锂矿精矿 → 碳酸锂 → 正极材料`
  以及
  `煤/润滑剂 → 石墨负极材料`、`聚丙烯粒料 → 电池隔膜`、`碳酸锂/LPG → 电解液`。
- 新增 `lithium_ion_cell` 和 `lithium_ion_battery` 配方，分别产出电芯和电池。
- 新增电池包 `battery_pack`，由电池、电路板、钢板和塑料结构件总装。
- 新增电池回收设备 `battery_recycler`，支持“电池包 → 黑粉 → 锂电材料”的简化回收路线。
- 保留旧的 `battery_cell`、`battery` 配方，作为旧存档和简化生产路线的兼容方案。
- 新增商品价格、创造模式物品和中英文名称。

## 现实依据与抽象边界

美国能源部的锂电池供应链蓝图将上游原料、正负极粉体、隔膜、电解液、电极/电芯制造和电池包列为不同环节；石墨是锂离子电池常用负极材料，隔膜用于隔开正负极并允许离子传导。

本阶段保留 Minecraft 可玩性：没有新增矿石方块和复杂化学计量，而是用石英作为锂矿物来源代理，用现有煤、红石、铁等材料简化正极路线。后续如需要更高拟真度，可以再拆分锂、镍、钴、锰、溶剂、导电剂和粘结剂。

回收路线参考了 EPA 所描述的电池收集、拆解/粉碎、黑粉形成和后续金属回收流程；黑粉在游戏中作为中间物，之后重新生成碳酸锂、石墨负极和正极活性材料。

参考资料：

- [美国能源部：National Blueprint for Lithium Batteries 2021–2030](https://www.energy.gov/sites/default/files/2021-06/FCAB%20National%20Blueprint%20Lithium%20Batteries%200621_0_0.pdf)
- [美国能源部：Grid Energy Storage Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Energy%20Storage%20Supply%20Chain%20Report%20-%20final.pdf)
- [美国能源部：电池隔膜的功能说明](https://www.energy.gov/edf/entek)
- [美国地质调查局：石墨与锂离子电池负极](https://www.usgs.gov/data/grade-and-tonnage-data-disseminated-flake-graphite-deposits)

## 后续方向

- 为电芯增加容量、循环寿命、热失控风险和良率属性。
- 增加电池包、回收和梯次利用，连接电子产品质保与召回。
- 在不破坏旧配方的前提下，逐步增加镍、钴、锰和电解质溶剂的独立供应链。
- 将废旧电池和电子产品的回收来源从“企业自制电池包”扩展到消费端报废物，并加入运输安全与回收企业服务合同。

### electronics-phone-casing-2026-09-06.md

# 手机机壳注塑环节 - 2026-09-06

## 已实现

- 新增 `phone_casing` 手机机壳零部件。
- 手机机壳由 2 份塑料粒料在 `injection_molder` 注塑机中生产。
- 智能手机总装现在需要电路板、电池、显示面板和手机机壳。
- 手机机壳加入商品目录、创造模式物品栏、模型和本地化文本。

## 现实依据

消费电子的塑料结构件通常经过塑料粒料加热、均化并注入模具形成，再与电路板、显示组件和电池进行总装。这里将机壳单独建模，使注塑设备不只服务于风扇叶片，也能服务于电子消费品的结构件生产。

参考资料：

- [U.S. EPA：塑料加工与注塑工艺资料](https://www.epa.gov/sites/default/files/2017-06/documents/pv29_scope_06-22-17.pdf)

## 后续方向

- 增加手机摄像头模组、扬声器和充电接口等通用零部件。
- 将电子产品的良率、质量和售后维修纳入企业经营指标。

### electronics-phone-modules-2026-09-06.md

# 手机通用功能模块 - 2026-09-06

## 本次实现

- 新增扬声器模组、麦克风模组和充电接口模组。
- 三种模块均由电子装配线生产，并复用铜线、铁、红石和塑料粒料等上游物料。
- 智能手机总装现在额外需要这三种模块，形成显示、供电、影像、音频、拾音和接口的完整装配关系。
- 新物品加入商品目录、创造模式物品栏、模型和中英文名称。

## 现实依据

手机拆机资料通常将扬声器、麦克风和 USB-C/充电接口作为可拆分的底部组件或主板周边组件；部分机型的麦克风直接集成在充电接口板上。因此本阶段把它们作为独立可采购、可装配的模块，同时保留后续把麦克风与充电接口合并为不同产品方案的空间。

参考资料：

- [iFixit iPhone 16e teardown](https://www.ifixit.com/News/108430/iphone-16e-teardown)
- [iFixit Samsung Galaxy A56 repair guide](https://www.ifixit.com/Guide/Samsung+Galaxy+A56+5G+Disassembly+and+Basic+Repair/221402)

## 当前抽象边界

- 暂不区分听筒、扬声器、主麦克风和降噪麦克风的规格。
- 暂不实现 USB 协议、音频编解码器和维修更换周期。
- 模块的价格是游戏内商品价格，不代表现实采购报价。

## 后续方向

- 增加手机质量/规格档位，使不同模块组合影响售价、耐久和售后需求。
- 将充电接口与麦克风板在部分机型中建模为组合件，体现设计取舍。
- 后续接入返修、备件库存和售后服务，而不是只在总装阶段消耗物料。

### electronics-recycling-loop-2026-09-06.md

# Electronics recycling loop — 2026-09-06

## Implemented

- Added a dedicated `electronics_recycler` machine.
- Added recycling recipes for smartphones, televisions, laptops and wireless routers.
- Recycling returns copper wire and plastic pellets as secondary raw materials.
- The recycling output is intentionally lower than the original bill of materials; it represents dismantling loss, contamination and material separation rather than duplicating a finished product.

## Industry basis

The model follows the basic real-world hierarchy of used-electronics management: reuse or refurbishment should be considered before material recycling; recycling facilities then collect, sort, dismantle and mechanically separate devices to recover materials such as copper, plastics, glass and other metals.

## Future direction

- Add a separate refurbishment route that consumes repair parts and returns a discounted used product.
- Add battery/data-security and hazardous-material handling when the end-of-life system becomes more detailed.

### electronics-refurbishment-loop-2026-09-06.md

# Electronics refurbishment loop — 2026-09-06

## Implemented

- Added a dedicated `refurbishment_line` machine.
- Added refurbished smartphone, television, laptop and wireless-router products.
- Refurbishment consumes an existing device plus realistic repair/replacement parts and produces a separately valued refurbished product.
- Refurbished products are distinct from new products, allowing the market to price them differently and preventing refurbished stock from being mistaken for factory-new goods.
- Refurbished products can reach end of life and enter the electronics recycler with lower material recovery yields than new products.

## Industry basis

The flow follows the real-world priority of collection, triage, testing, repair/refurbishment and resale before material recycling. The game-scale recipes abstract diagnostics, parts replacement, cleaning, testing and quality grading into one production cycle.

## Future direction

- Add condition grades and warranty/return periods when the product-quality and after-sales systems are expanded.
- Connect used-product collection to logistics and trade-in contracts instead of treating the input as an immediately available factory feedstock.

### equipment-material-supply-chain-2026-09-05.md

# Equipment material supply chain

## Implemented

- Each industrial machine now declares a material bill of materials in `MachineType`.
- Installing equipment requires both the existing cash purchase price and the corresponding materials in the company's warehouse.
- Basic machines use vanilla materials so a new company can bootstrap; advanced machines consume steel sheet, copper wire, glass, and electric lamps from the existing industrial chain.
- Equipment material consumption is atomic and reduces commodity supply after the machine is installed.
- Existing installed equipment remains valid because the new requirement applies only to future purchases.
- If the material reservation fails after the cash debit, the cash purchase is rolled back as a non-operating ledger entry.
- The equipment BOMs were checked for dependency cycles and use upstream vanilla raw materials, so a machine never requires a downstream product that can only be made by that same machine class.
- Machine maintenance now consumes a proportional replacement-parts/material batch in addition to its cash service cost; a full repair consumes the full equipment BOM and a minor repair consumes at least one unit of each required material.

## Real-world alignment

Industrial equipment requires both capital expenditure and physical components. The model keeps the game abstraction manageable while making machinery demand visible to upstream steel, electrical, glass, and assembly industries.

## Next direction

- Add explicit equipment manufacturing recipes and supplier orders so companies can sell machines rather than only consume warehouse materials.
- Add machine-specific spare parts and maintenance material requirements after the supplier path exists.

### fuel-utilities-service-2026-09-06.md

# 燃料油公用事业服务 - 2026-09-06

## 已实现

- 公用事业行业新增可选配方 `fuel_generation`。
- 每个服务周期消耗 1 份燃料油，产生 90 美元供能服务收入。
- 原有 `coal_generation` 保留为默认配方，已有企业不会自动切换。
- 燃料油因此同时拥有运输和公用事业两个下游用途，形成石化产品到能源服务的基础链条。

## 现实逻辑

现实能源企业会在煤炭、燃油、天然气和其他能源之间进行设备与燃料选择；不同燃料会影响成本、设备适配和服务收益。本阶段用可选配方表达燃料选择，不强行模拟电网调度、排放许可和发电效率。

## 后续方向

- 将燃料类型与设备投资、维护成本和排放/环保规则关联。
- 增加天然气、柴油等能源产品，并区分发电、供热和燃气服务。

### industry-chain-roadmap-2026-09-05.md

# 产业链与企业生产阶段记录

更新日期：2026-09-05

## 本阶段已实现

- 生产配方增加机器类型、最低工人数、能源成本和设备维护成本字段。
- 工人以持久化合同记录，包含工种、人数、日工资、技能和合同状态；暂不依赖实体 NPC，避免区块加载状态影响生产结果。
- 设备以企业资产记录，当前包括农田、矿石处理机、车床、铣床、轧机和装配线。
- 生产周期只有在原料、最低工人数和设备均满足时才执行。
- 每次生产周期扣除工资、能源和维护成本，并写入企业账本及企业所得税费用记录。
- 采矿链已修正为“铁矿石 → 铁锭”，不再把煤和金错误地作为铁矿石副产品。
- 制造链使用“铁锭 + 煤 → 铁轨”，由轧机承担成型加工；农业链为“种子 → 小麦”，可继续作为餐饮和医疗服务的上游投入。
- 生产系统支持同一行业的多条可选配方；企业可以通过 `/company recipes <名称>` 查看，并用 `/company recipe <名称> <配方ID>` 切换。
- 增加首条消费品示范链：铁矿/铜矿 → 金属锭；金属锭 → 钢板、铜线；砂 → 玻璃 → 玻璃透镜；钢板 + 铜线 + 玻璃透镜 → 电灯。
- 新增产业链物品：钢板、铜线、玻璃透镜、电灯；新增工艺设备类型：玻璃熔炉、拉丝机、食品加工机。
- 增加第二条消费品链：小麦 → 面粉 → 面包，以及铁锭 → 金属罐；小麦 + 金属罐 → 罐装食品。
- 设备现在会在成功生产后产生磨损；企业可使用 `/company machine maintain <名称> <机器ID>` 支付维护费用并恢复设备状态，磨损归零时生产停机。
- 设备直接提供可计算的批次产能：普通设备每台支撑一个批次，装配线按自身能力支撑多个批次；扩大产量必须追加设备、工人和营运资金，不通过企业升级完成。

## 现实逻辑约束

产业链应拆成原料、初级加工、中间品、设备加工、成品销售和服务消费。不同企业可以共享中间品，供需变化应通过仓储、订单和市场结算传递，而不是每个行业独立生成一笔固定收入。

服务业通常没有可交易的物品产出，应由劳动力、场所、能源和订单/顾客需求形成服务收入；后续会把目前“无物品产出即跳过”的服务业生产调度改成服务合同结算。

## 下一阶段

1. 把设备采购从抽象资产逐步连接到工业品供应链，并增加设备折旧、维修停机和安全状态。
2. 把矿业拆分为采掘、选矿、冶炼，加入煤炭、金矿等各自独立的原料链。
3. 把制造业拆分为钢材、机械零件、设备和最终产品，建立跨行业共享中间品。
4. 增加服务合同、客户需求和订单交付，让运输、零售、餐饮、医疗等行业拥有完整的收入来源。
5. 在此基础上再加入可视化 NPC 员工，不让实体加载状态决定核心经济结果。

## 本阶段资料依据

- 美国能源部钢铁流程图将铁矿石、煤/焦炭、石灰石、炼铁和炼钢作为不同工序；因此模组不会把铁矿石直接变成多个不相关成品。
- 美国环保署玻璃制造资料将玻璃分为原料配料、熔融、成形和精加工阶段；模组以砂→玻璃→透镜表达这些阶段的简化版本。
- OSHA 将切削、成形、动力传动和控制系统视为不同的机器安全区域；因此机器类型与配方工艺分离，同一机器可以承载多条配方但仍有独立维护成本。

参考链接：

- [U.S. Department of Energy：钢铁制造流程图](https://www.energy.gov/sites/default/files/2022-11/DTG-final.pdf)
- [U.S. EPA：玻璃制造 AP-42 工艺说明](https://www3.epa.gov/ttn/chief/ap42/ch11/final/c11s15.pdf)
- [OSHA：Machine Guarding](https://www.osha.gov/etools/machine-guarding/introduction)
- [FAO：小麦制粉流程](https://www.fao.org/4/al376e/al376e.pdf)
- [USDA FSIS：Shelf-Stable Food Safety](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/shelf-stable-food)

### industry-config-migration-2026-09-06.md

# Industry recipe configuration migration

## Implemented

- Existing `industries.json` files now receive missing maintained recipe IDs when the mod loads them.
- Player or pack-author recipes with the same ID remain authoritative and are not overwritten.
- Newly added staged mining and smelting recipes therefore become visible without deleting or manually rebuilding an existing configuration file.
- The merge is performed in memory, so unrelated custom industries and recipes remain unchanged on disk.

## Compatibility boundary

This is additive migration only. It does not remove recipes, rewrite prices, or silently change an existing recipe with a matching ID.

## Next direction

- Add an explicit data version if future migrations need to rename or remove recipes.
- Continue expanding manufacturing intermediates while keeping recipe IDs stable.

### industry-config-validation-2026-09-06.md

# 产业配方设备校验 - 2026-09-06

## 本次实现

- 产业配置加载完成后，自动遍历每个行业及其全部生产配方。
- 使用 `MachineType.parse` 校验行业默认设备和配方设备；发现未注册设备时立即输出明确的行业、配方和设备 ID。
- 检测同一行业内重复的配方 ID，并输出警告，避免配置合并后出现不可预期的配方覆盖。
- 检查投入/产出数量是否为正数、物品 ID 是否为空、服务收入是否为负，以及既无产出又无正收入的无效配方。
- 增加 `/company recipes`，在游戏内显示当前生效的行业、配方、机器、工人数、投入、产出和服务收入，方便管理员核对配置合并结果。
- 校验只读配置，不改变旧存档和自定义配方内容；生产系统仍会在实际运行时继续执行正式的设备和资金检查。

## 解决的问题

之前自定义 `industries.json` 中的设备拼写错误，只有企业真正进入生产周期后才会表现为 `invalid_machine`。现在启动加载阶段即可发现，便于服务器管理员定位配置问题。

## 当前边界

- 由于自定义物品可能在模组注册事件之后才可解析，物品 ID 校验仍保留现有的延迟解析机制。
- 当前只校验设备类型和配方 ID，不自动修改玩家的自定义配置。

## 后续方向

- 增加管理命令查看当前生效的行业、配方和设备依赖。
- 对缺失物品、负数投入、零产出非服务配方和异常维护成本增加同一套配置诊断。

### industry-production-research-2026-09-05.md

# 企业生产与产业链设计依据

最后更新：2026-09-05

## 现实依据

- ILO 对雇佣关系的描述强调：劳动者在约定条件下为雇主提供劳动，并获得报酬；判断雇佣关系还要考虑企业控制、工作地点、工作时间、工具和材料由谁提供等因素。
- IFRS IAS 2 将存货成本分为采购成本、转换成本和使存货达到当前位置及状态的其他成本；转换成本包括直接人工以及固定、变动生产间接费用，存货出售时才结转为费用。
- OSHA 的机器安全资料将机器风险划分为作业点、动力传动和控制系统，并要求防护、停机控制、检修隔离和定期检查；机器不能只被视为一个提高产量的静态数值。

## 对模组的设计约束

1. 雇佣 NPC 先抽象为持久化劳动合同，至少记录工种、工资、技能、工作条件和合同状态；是否使用实体 NPC 只属于表现层，不能让实体加载状态决定工资和生产结果。
2. 机器需要记录类型、产能、维护状态和安全状态。车床、铣床等设备要有不同的工艺能力，维护不足时应停机、降效或产生事故风险，而不能仅提供统一产能加成。
3. 生产成本至少由原料、工资、能源/维护、运输和生产间接费用组成；销售前形成存货成本，销售后再确认收入和销售成本。
4. 行业配方必须区分投入、工艺设备、劳动力和产出。没有投入物的服务业可以产出服务收入，但不应凭空生成可交易实物。
5. 生产周期、工资结算、维护和库存状态必须持久化，并能在服务器重启和企业主离线时继续保持一致。

## 当前实现与后续阶段

- 已完成第一阶段：企业生产周期、原料检查、原子化消耗、成品入库、供应订单补发和生产状态持久化。
- 下一阶段：加入劳动合同和机器安装/维护数据，再将设备与行业工艺能力关联。
- 后续阶段：加入工资、折旧、能源、质量、事故与生产成本结转，并接入企业财务报表和税务。

## 参考资料

- ILO，《The employment relationship》：https://www.ilo.org/employment-relationship
- ILO，《Questions and answers on business and employment security》：https://www.ilo.org/ilo-helpdesk/questions-and-answers-business-and-employment-security
- IFRS，IAS 2 Inventories：https://www.ifrs.org/issued-standards/list-of-standards/ias-2-inventories/
- OSHA，Machine Guarding Introduction：https://www.osha.gov/etools/machine-guarding/introduction
- OSHA，Machine Guarding General Requirements：https://www.osha.gov/etools/machine-guarding/introduction/general-requirements

### manufacturing-components-2026-09-06.md

# Manufacturing component chain

## Implemented

- Added machine frames and electric motors as reusable manufacturing intermediates.
- Machine frames use steel sheet and iron ingots on a rolling mill.
- Electric motors use copper wire, iron ingots, and redstone on a lathe.
- Assembly lines now require those components plus glass, connecting equipment construction to upstream manufacturing.
- Added commodity entries and item models for both components.

## Real-world alignment

Industrial equipment is assembled from structural frames and drive/control components rather than appearing as a single abstract purchase. The chain remains intentionally simplified but exposes meaningful upstream demand.

## Next direction

- Add explicit equipment supplier offers and machine delivery/installation lead time.
- Expand the same component pattern to transport, energy, and agricultural equipment.

### mining-processing-chain-2026-09-05.md

# Mining and metal-processing chain

## Implemented

- Added an optional ore-concentration stage for iron, copper, and gold: two ore blocks become one raw-metal unit.
- Added an optional blast-furnace smelting stage: raw metal plus coal becomes an ingot.
- Added the `blast_furnace` industrial machine with a raw-material equipment BOM.
- Kept the former direct ore-to-ingot recipes so existing company saves and simple starter play remain compatible.

## Real-world alignment

Mining, beneficiation/concentration, and metallurgical smelting are separate industrial stages. The simplified chain now represents that separation without requiring a full chemical-process simulation.

## Next direction

- Add by-products and quality/yield differences for ore grades.
- Split manufacturing into reusable intermediate components and final assembly orders.

### pcb-fabrication-process-chain-2026-09-06.md

# PCB 制造工艺链（2026-09-06）

## 本阶段完成

- 新增 PCB 制造设备 `pcb_fabrication_line`。
- 将 PCB 制造拆为三个可选生产步骤：钻孔面板、蚀刻线路板、阻焊线路板。
- 新增完整装配路线：阻焊 PCB + 焊料 + 表面贴装元件 + 封装芯片 → 电路板。
- 原有 `printed_circuit_board` 简化配方保留，旧企业和旧世界仍可继续使用；新企业可以选择更完整的制造路线。
- 新增物品、市场价格、创意标签、双语名称和基础模型。

## 现实依据与边界

EPA 对 PCB 制造流程的描述包括覆铜基材准备、钻孔、镀铜、线路蚀刻、阻焊以及后续焊接/装配；本阶段把其中关键节点映射为游戏内批次工艺。实际工业还会包含除胶渣、化学镀铜、电镀、光刻、检测和废水处理，本阶段暂以制造线的设备能力和成本参数进行抽象。

参考：

- https://nepis.epa.gov/Exe/ZyPURL.cgi?Dockey=30004M80.TXT
- https://www.epa.gov/sites/production/files/2015-10/documents/metal-finishing_dd_1983.pdf
- https://www.ipc.org/system/files/technical_resource/E38%26S03-03%20-%20Joseph%20Fjelstad.pdf

## 后续方向

- 将电镀、AOI/电气测试、良率和铜/化学品损耗独立建模。
- 将 PCB 制造企业与 SMT/EMS 装配企业在订单和供应链上区分开。

### pet-polyester-packaging-chain-2026-09-06.md

# PET polyester packaging chain — 2026-09-06

## Implemented

- Added p-xylene as an aromatic reforming product from naphtha.
- Added terephthalic acid through an oxidation reaction stage.
- Added PET resin through terephthalic-acid/ethylene-glycol polycondensation.
- Added PET bottle production using the existing injection-molding machine.
- Added a separate PET-bottle recycling route: bottle sorting/washing/granulation produces PET flakes, which are remelted into PET resin.
- Registered the new commodities, creative-tab entries, translations and item models.

## Industry basis

The chain models the common PET packaging route at game scale: aromatic feedstock → p-xylene → terephthalic acid → PET resin → bottle preform/container. The recipe does not claim to reproduce industrial yields or catalysts exactly; it preserves the important separation between feedstock conversion, chemical intermediate, polymerization and forming.

## Future direction

- Add separate bottle preform and stretch-blow-molding stages if packaging becomes a deeper production specialization.
- Add deposit/collection logistics when the waste-management system is expanded beyond company production recipes.

### petrochemical-base-oil-blending-2026-09-06.md

# 石化基础油与润滑油调和链 - 2026-09-06

## 本次实现

- 炼油配方现在产出基础油 `base_oil`，不再直接产出润滑油。
- 调和装置使用基础油和燃料油生产润滑油，形成“原油 → 炼油馏分/基础油 → 调和润滑油”的连续链条。
- 保留原有 `lubricant` 商品和物品 ID，旧存档库存不会被删除；只改变新生产批次的来源。
- 为基础油补充商品初始价格、创造模式入口、名称和物品模型。

## 现实逻辑依据

炼厂先通过分离、转化和处理得到不同馏分与中间油品；润滑油通常还需要基础油与添加剂进行调和，不能把所有润滑油都视为炼油塔直接产物。本阶段用燃料油作为添加剂/调和组分的游戏化代理，后续可以继续拆出黏度等级和添加剂。

参考资料：

- U.S. Energy Information Administration, Refining crude oil - the refining process: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- U.S. Energy Information Administration, U.S. refinery and blender net production: https://www.eia.gov/dnav/pet/pet_pnp_refp_dc_nus_mbbl_a.htm

## 兼容与边界

- 没有删除旧物品，也没有修改已有库存；旧的润滑油仍可交易和作为生产投入。
- 没有改变企业配方选择、设备维护、税务或贷款规则。
- 当前仍未模拟基础油黏度、添加剂配方、硫含量和质量等级。

### petrochemical-consumer-electronics-components-2026-09-06.md

# 石化与电子消费品共用中间件 - 2026-09-06

## 本次实现

- 沿用现有石化上游：原油、炼厂馏分、乙烯/丙烯、ABS/通用塑料粒料，为电子外壳和包装提供材料。
- 新增键盘模组、存储模组、无线通信模组三个可交易的电子中间品。
- 键盘模组由 ABS 树脂、被动元件和铜线在电子装配线上生产。
- 存储模组由封装芯片、PCB 和被动元件在 SMT 线上生产。
- 无线通信模组由封装芯片、被动元件和铜线在 SMT 线上生产。
- 笔记本电脑现在需要键盘模组和存储模组；无线路由器现在需要无线通信模组。
- 三种模组复用已有设备和生产能力，没有为每种消费品创建专用机器。

## 现实依据

- 石化工业通过炼化、裂解和聚合提供塑料树脂等基础材料；ABS 常用于电子产品外壳。
- 电子消费品通常拆分为 PCB、封装芯片、存储、无线连接、输入部件和外壳，再由 EMS/整机装配完成。
- SMT 适合生产高密度电子模块；注塑/成型适合塑料外壳和键盘结构件。

## 后续方向

- 为显示屏拆分玻璃基板、背光和显示驱动，而不是把显示屏作为单一中间品。
- 增加电子产品质量等级、良率、返修和售后备件消耗。
- 将塑料、铜和电子废料回收分流，避免回收结果全部变成同一种通用材料。

## 参考资料

- U.S. EIA, Refining crude oil: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- Semiconductor Industry Association, How are semiconductors made: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/
- Intel, Silicon die to chip package: https://www.intel.com/content/www/us/en/newsroom/tech101/manufacturing/how-silicon-die-become-chip-packages.html

### petrochemical-consumer-electronics-expansion-2026-09-06.md

# 石化与电子消费品扩展（2026-09-06）

## 本次实现

- 在已有“原油 → 精炼 → 石脑油/烯烃 → 聚合物”链条上增加润滑油和沥青副产品。
- 增加聚乙烯粒料、聚丙烯粒料和合成橡胶，保留原有通用塑料粒料配方以兼容旧存档和旧配置。
- 增加调和装置（`blending_unit`），用于把炼厂馏分进一步加工为润滑油。
- 增加电源适配器、电视机、笔记本电脑三类电子消费品。
- 增加无线路由器，复用封装芯片、PCB、电源适配器、ABS 树脂和铜线等上游部件。
- 增加 SMT 生产线（`smt_line`），用于电源适配器等电子装配前段；电视机和笔记本电脑继续使用电子装配线。
- 新增商品初始价格、物品注册、英文名称和临时原版纹理映射。

## 现实逻辑依据

- EIA 将炼厂流程概括为分离、转化和处理，并说明原油精炼可得到燃料、石化原料等多类产品；本实现因此把炼厂建模为多产出节点，而不是单一产物机器。
- EIA 说明石脑油等石化原料经蒸汽裂解可生成乙烯、丙烯等基础化学品，这些基础化学品再进入塑料、合成橡胶等聚合物路线。
- EPA 的聚合物生产范围明确覆盖聚乙烯、聚丙烯等基础聚合物，因此第一版将两种主要牌号拆成独立商品；旧的通用塑料粒料仍作为简化路线保留。
- 半导体行业协会将芯片后段描述为切割、贴装、键合、封装和测试；现有晶圆/封装芯片路线继续沿用，新增消费品只消费封装芯片和装配件，不把晶圆直接当作终端电子产品。

## 当前简化边界

- 合成橡胶暂用“丙烯 + 石脑油”的游戏化原料代理现实中的更细分单体/聚合路线；后续可拆出丁二烯、苯乙烯和专用聚合物。
- 润滑油和沥青目前作为炼厂多产出及调和路线的商品节点，尚未加入质量等级、添加剂、粘度或硫含量。
- SMT 线目前是生产能力和机器成本的抽象，不模拟贴片机、回流焊、AOI 等单机设备。
- 电视机和笔记本电脑先作为消费品终端，后续可继续拆分存储器、摄像头、键盘、扬声器、显示驱动和包装物流。

## 后续方向

1. 将不同塑料牌号接入注塑、薄膜、包装、轮胎和家电外壳，形成跨行业需求。
2. 增加石化质量属性和副产品库存核算，避免所有炼厂产物都按同一质量处理。
3. 增加电子产品的质量、良率、返修和质保逻辑，连接现有企业质量台账。
4. 以 JSON 配置迁移方式把新增链条暴露给服务器管理员，而不是只依赖代码默认值。

## 资料

- U.S. Energy Information Administration, “Refining crude oil - the refining process”: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- U.S. Energy Information Administration, “Refining crude oil - inputs and outputs”: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-inputs-and-outputs.php
- U.S. Environmental Protection Agency, “Subpart X – Petrochemical Production”: https://www.epa.gov/ghgreporting/subpart-x-petrochemical-production
- U.S. Environmental Protection Agency, “Polymer Manufacturing Industry”: https://www.epa.gov/stationary-sources-air-pollution/polymer-manufacturing-industry-standards-performance-volatile
- Semiconductor Industry Association, “Stage 4: Back-end Manufacturing”: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/

### petrochemical-consumer-electronics-intermediates-2026-09-06.md

# 石化与电子消费品中间层（2026-09-06）

## 本阶段完成

- 石化链新增苯、苯乙烯单体和 ABS 树脂。
- 新增“石脑油重整/芳烃路线 → 苯 → 苯乙烯单体 → ABS 树脂”的可选生产链。
- ABS 树脂进入手机外壳、电视和笔记本外壳，体现消费电子常用工程塑料的中间材料属性。
- 电子链新增被动元件、电源管理芯片和显示驱动芯片。
- 手机、电视、笔记本装配现在需要相应的电源/显示控制器，产品价值、工人和能耗参数同步提高。
- 新增物品已加入创意标签、双语名称和基础物品模型；配方仍复用现有化工反应器、聚合反应器、SMT 线和电子装配线。

## 现实依据与简化边界

- EIA 将乙烯、丙烯等炼厂烯烃列为塑料、树脂和涂料的石化中间原料；本模组把苯系芳烃和苯乙烯作为进一步聚合的简化中间层：
  https://www.eia.gov/energyexplained/hydrocarbon-gas-liquids/uses-of-hydrocarbon-gas-liquids.php
- ABS 真实工业路线还涉及丙烯腈、丁二烯和苯乙烯。本阶段以现有合成橡胶作为丁二烯类橡胶相的游戏内代理，没有虚构完整化学计量。
- SIA 描述芯片后段包括切割、贴装、键合、封装和测试；本模组继续将芯片封装结果作为消费电子的 IC 来源：
  https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/
- PCB 资料显示电子产品还需要基板、铜层、焊接和表面贴装元件；因此新增的被动元件和控制 IC 作为 SMT/整机装配的中间投入，而不是把整台设备直接视为成品。
  https://nepis.epa.gov/Exe/ZyPURL.cgi?Dockey=30004DTH.TXT

## 后续方向

- 将 PCB 的钻孔、镀铜、蚀刻、阻焊、AOI 检测拆成可选择的工艺链，并记录铜损和废液处理成本。
- 增加显示面板玻璃、偏光片、背光和触控层等更细的面板供应链。
- 将消费电子按 OEM/EMS/品牌销售区分，加入批量订单、良率和质保返修，而不是继续单纯按配方产出。

### petrochemical-consumer-electronics-products-2026-09-06.md

# 石化与电子消费品扩展 — 2026-09-06

## 本次实现

- 在已有“原油/炼油/石脑油/烯烃/聚合物/塑料粒子”链条上继续接入两个终端消费品：充电宝、智能音箱。
- 充电宝使用电池包、充电接口、电源管理芯片和 ABS 树脂，体现电芯组装、电源管理、外壳注塑和电子装配的分工。
- 智能音箱使用无线通信模组、扬声器模组、麦克风模组、封装芯片和 ABS 树脂，体现芯片、声学器件、无线连接与塑料外壳的整机装配。
- 两个产品复用电子装配线，不新增“一种产品一台专机”的不现实约束；上游仍可由不同企业分别供应零部件。

## 现实依据与简化边界

- 石化链条继续把炼油副产物和裂解/聚合物作为塑料与电子外壳的上游，而不是把塑料直接凭空生成。
- 半导体链条仍按“晶圆制造 → 切割/测试 → 封装 → 进入电路板和消费电子”的顺序抽象。
- 充电宝配方把电池包作为已完成的电池系统，暂不模拟 BMS 固件、热管理和安规认证；智能音箱暂不拆分 Wi‑Fi/蓝牙协议栈与扬声器单体参数。

## 参考资料

- U.S. EIA, Refining crude oil: https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- Semiconductor Industry Association, How are semiconductors made?: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/
- U.S. Department of Energy, Battery pack components and assembly: https://www.energy.gov/documents/chapter13-electricpdf

## 后续方向

- 增加消费电子质量/良率、召回、质保和翻新分级，而不是继续只增加终端物品数量。
- 将 ABS、PC、PET 等塑料牌号与注塑、挤出、电子外壳需求关联，保留不同产品对材料的差异。
- 后续若引入智能家电，应先复用现有电池、显示、无线和装配模块，再增加产品专属零件。

### petrochemical-electronics-foundation-2026-09-06.md

# 石化与电子消费品产业基础 - 2026-09-06

## 本次实现

### 石化链

- 新增设备：油井 `oil_well`、炼油厂 `oil_refinery`、聚合反应器 `polymer_reactor`。
- 新增物品：原油 `crude_oil`、石脑油 `naphtha`、塑料粒料 `plastic_pellets`。
- 基础链条：油井产出原油 → 炼油厂分离出石脑油 → 聚合反应器生产塑料粒料。
- 塑料粒料继续供应注塑机，用于生产风扇叶片和后续塑料外壳。

### 电子消费品链

- 新增设备：半导体制造设备 `semiconductor_fab`、电子装配线 `electronics_assembly`。
- 新增物品：硅晶圆 `silicon_wafer`、电路板 `circuit_board`、电池 `battery`、智能手机 `smartphone`。
- 基础链条：石英/碳还原材料 → 硅晶圆 → 电路板；电池与电路板、镜片、塑料粒料 → 智能手机。
- 电子装配设备与通用装配线分开，体现电子产品需要专用装配与测试能力。

## 现实依据

EIA 说明炼厂通过分离、转化、处理把原油变成燃料和化工原料，石脑油等石化原料可进入裂解与塑料生产；EPA 资料说明塑料粒料经加热、均化并注入模具形成塑料件；美国能源部的半导体供应链报告将半导体视为消费电子的重要上游环节。

参考资料：

- https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php
- https://www.eia.gov/tools/faqs/faq.php?id=34
- https://www.epa.gov/sites/default/files/2017-06/documents/pv29_scope_06-22-17.pdf
- https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf

## 当前抽象边界

- 油井目前不区分钻井周期、井深和伴生气，但已经区分经营地点、油田区块和有限储量。
- 企业现在持久化登记经营地点；油井只在经营地点对应的油田区块生产，油田储量会随开采递减。
- 可使用 `/company site <公司名>` 在当前位置迁移企业经营地点并进行油田勘探。
- 油田按维度和区块生成确定性的有限储量；没有油田的区块不能生产原油。
- 经营地点登记与油井生产现在要求企业所有者拥有或合法租赁对应土地；税务冻结、拍卖、租期结束或租赁欠款会阻止经营。
- 石脑油与聚合物是简化的中间品，没有展开乙烯、丙烯、聚合催化剂和副产物。
- 半导体制造用石英和碳材料表达硅材料来源，没有展开高纯硅、光刻、掺杂、封装和良率。
- 这些简化只用于保留产业方向和经营决策，不代表现实配方比例。

## 后续方向

- 在世界地图上生成油田区块，要求油井建设在可采资源范围内，并设置储量衰减。
- 增加炼油副产品、燃料和化工原料的联产关系，避免炼厂只产出单一石脑油。
- 增加芯片良率、设备折旧、研发投入、产品质量与售后维修，形成电子企业的差异化经营。

### petrochemical-olefin-chain-2026-09-06.md

# 石化烯烃与聚合物链 - 2026-09-06

## 本次实现

- 新增乙烯 `ethylene` 与丙烯 `propylene` 两种石化中间品。
- 新增蒸汽裂解装置 `steam_cracker`：石脑油 → 乙烯 + 丙烯。
- 新增两条聚合路线：乙烯 → 塑料粒料、丙烯 → 塑料粒料。
- 保留原有“石脑油 + 煤 → 塑料粒料”配方，兼容已经存在的企业配置和存档。
- 乙烯、丙烯加入商品目录，可被采购、库存和市场系统使用。

## 现实依据

炼油过程会产生石脑油等中间馏分；石脑油可以进一步进入蒸汽裂解，生产乙烯、丙烯等基础化工原料。乙烯和丙烯再通过聚合分别对应聚乙烯、聚丙烯等树脂，树脂通常以粒料形式交给下游注塑、挤出和装配企业。本阶段将这些工序抽象成可替代的生产配方，不模拟真实工厂的完整催化剂、温压、分离塔和副产物平衡。

参考资料：

- [U.S. Energy Information Administration：Refining crude oil](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)
- [U.S. EPA：Plastics molding and forming](https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines)

## 后续方向

- 将乙烯、丙烯的裂解收率和燃料副产物纳入批次结算。
- 进一步增加柴油、汽油、液化石油气等炼厂产品，但要先设计联产库存与需求，避免单一配方无限产出。
- 将不同塑料树脂类型接入注塑件规格、质量和回收流程。

### petrochemical-pcb-materials-2026-09-06.md

# 石化中间材料与 PCB 供应链 - 2026-09-06

## 本次实现

- 石化链新增乙二醇和环氧树脂两个中间材料。
- 乙二醇由乙烯和水进入化学反应器生产，环氧树脂由乙二醇和塑料颗粒进入聚合反应器生产。
- 电子链新增 PCB 基材、铜箔、焊料和贴片元件。
- PCB 基材由玻璃与环氧树脂经层压生产，铜箔由铜锭经轧制生产，焊料由金属合金化生产，贴片元件由红石、铜线和塑料颗粒经 SMT 线生产。
- 新增完整的 PCB 装配配方：PCB 基材 + 铜箔 + 焊料 + 贴片元件 + 封装芯片 → 电路板。
- 保留旧电路板配方，避免破坏已有世界和旧的自定义产业配置。
- 新材料已加入企业商品目录、创造模式物品栏、英文名称和物品模型。

## 现实依据

石化行业通常以乙烯等基础化学品继续生产聚合物和树脂；印刷电路板则以基材、铜箔、焊料和电子元件经过线路加工与装配形成。石化原料与聚合物方向参考 [EIA Hydrocarbon Gas Liquids](https://www.eia.gov/energyexplained/hydrocarbon-gas-liquids/uses-of-hydrocarbon-gas-liquids.php)；电子装配和芯片后段参考 [Semiconductor Industry Association](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)。

## 当前抽象边界

- 乙二醇的真实工业路线还涉及环氧乙烷和催化/水合步骤，当前用化学反应器抽象。
- 焊料现实中通常涉及锡、银、铜等更明确的合金体系，当前暂用铜和铁代表金属合金输入，后续可加入锡资源与无铅焊料等级。
- PCB 的光刻、蚀刻、钻孔、阻焊和 AOI 检测目前仍合并在 `pcb_assembly_line` 中。

## 后续方向

- 增加锡、银、玻纤布、铜箔等级和无铅焊料路线。
- 将 PCB 制造拆分为覆铜板、线路加工、钻孔/阻焊和 SMT 装配。
- 让 PCB 良率、芯片等级和返修率影响电子消费品的质量与售后成本。

### petrochemical-polyethylene-consumer-loop-2026-09-06.md

# Polyethylene consumer loop — 2026-09-06

## Implemented

- Added the consumer commodity `plastic_container`.
- Added an `injection_molder` recipe consuming two `polyethylene_pellets` and producing one plastic container.
- Added an `extrusion_line` and a packaging-film recipe consuming one `polyethylene_pellets` and producing four packaging-film units.
- Added a `plastic_recycler` with separate film and container recycling recipes that return generic plastic pellets.
- Registered the item, market price, creative-tab entry, language names, and item model.
- Reused the existing injection-molding machine, which now serves phone casings, fan blades, and plastic containers; the new extrusion line models a different downstream process.

## Real-world basis

Polyethylene resin is commonly supplied as pellets to downstream processors. Injection molding heats and injects polymer melt into a mold, while extrusion melts and pushes resin through a die to form continuous film. Mechanical recycling commonly involves sorting, cleaning, shredding, melting, and pelletizing; the recycling recipes abstract those steps and apply a yield loss.

Reference: U.S. Environmental Protection Agency, “Plastics Molding and Forming Effluent Guidelines”: https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines

## Follow-up

- Later connect container demand to food, chemical, and retail contracts instead of relying only on the commodity market.
- Later separate recycled resin quality from virgin resin quality when the quality system supports material grades.

### petrochemical-polymer-pellet-correction-2026-09-06.md

# Polymer pellet route correction — 2026-09-06

## Implemented

- Corrected the default ethylene route to produce `polyethylene_pellets`.
- Corrected the default propylene route to produce `polypropylene_pellets`.
- Kept generic `plastic_pellets` as a separate simplified route for legacy and general-purpose recipes.
- Added a compatibility migration for old `industries.json` entries that used the two specific recipe IDs but incorrectly produced generic plastic pellets.
- The migration only changes the known old default output; deliberately customized recipe outputs remain unchanged.

## Why this matches industry practice

Ethylene and propylene are petrochemical feedstocks for distinct polymer families. Polyethylene and polypropylene are supplied as resin/pellet materials to downstream molding, extrusion, compounding, and battery-separator processes; they should not collapse into one generic commodity when the recipe explicitly represents a specific polymer.

## References

- U.S. Energy Information Administration, “How much oil is used to make plastic?”: https://www.eia.gov/tools/faqs/faq.php?id=34
- U.S. Environmental Protection Agency, “Plastics Molding and Forming Effluent Guidelines”: https://www.epa.gov/eg/plastics-molding-and-forming-effluent-guidelines
- U.S. EPA, “Polymer Manufacturing Industry” archive: https://archive.epa.gov/compliance/resources/publications/assistance/sectors/web/pdf/resfibsn.pdf

## Follow-up

- When quality batches become available for polymer production, add grade-specific yields and scrap rather than introducing more generic resin IDs.
- Later, connect pellets to injection molding, extrusion, and battery-separator capacity without forcing every downstream recipe to use the same polymer grade.

### petrochemical-refined-fuels-2026-09-06.md

# 炼油燃料联产品 - 2026-09-06

## 本次实现

- 新增柴油 `diesel`、汽油 `gasoline` 和液化石油气 `lpg`。
- 炼油配方现在以原油为输入，同时产出石脑油、燃料油、柴油、汽油和液化石油气。
- 三种产品加入商品目录，可进入企业库存、供应报价和商品市场。
- 旧世界和自定义配置仍保留原有配方；新的默认配置使用完整联产配方。

## 现实依据

现实炼厂并非只产出单一燃料。原油蒸馏和后续转化会产生汽油组分、柴油等馏分以及液化炼厂气；石脑油则继续作为石化原料。本阶段先用一个联产配方表达共同生产关系，尚未模拟不同原油品质、复杂设备组合、实际收率平衡和产品调和。

参考资料：

- [EIA：Refining crude oil—inputs and outputs](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-inputs-and-outputs.php)
- [EIA：The refining process](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)

## 后续方向

- 让柴油、汽油和液化石油气分别进入运输、发电、工业和居民能源需求。
- 按炼厂设备和原油类型调整联产比例，并处理副产品库存积压。
- 增加燃料质量规格、调和与储罐容量，避免燃料只作为静态商品存在。

### petrochemical-refinery-byproducts-2026-09-06.md

# 炼油联产副产品 - 2026-09-06

## 已实现

- 新增 `fuel_oil` 燃料油商品。
- 炼油配方改为：消耗 2 份原油，同时产出 1 份石脑油和 1 份燃料油。
- 多产出会分别进入企业仓库、商品供给和存货成本层；转换成本按产出数量分摊。
- 燃料油暂作为可交易工业品，尚未直接替换运输系统中的燃料消耗。

## 现实依据

现实炼油厂通过分馏、转化和处理把原油转化为燃料及化工原料，产品不是单一馏分。石脑油可继续进入石化原料链，较重馏分可作为燃料油等工业燃料。本阶段保留了游戏可读性，只抽象出一个可交易副产品。

参考资料：

- [U.S. Energy Information Administration：The refining process](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)

## 后续方向

- 将燃料油接入运输、发电或工业设备的燃料消耗。
- 增加柴油、汽油、液化气等产品，并为炼厂配置不同的产率和设备组合。
- 在不破坏旧存档自定义配方的前提下，提供配方迁移提示或管理员迁移命令。

### refined-fuels-service-demand-2026-09-06.md

# 成品燃料服务需求 - 2026-09-06

## 本次实现

- 公用事业新增柴油发电和液化石油气供能路线。
- 运输行业新增柴油、汽油和液化石油气运输路线。
- 煤炭和燃料油路线继续保留，企业可以按自身供应链选择燃料。
- 不强制替换已有企业配方，新增路线会通过配方目录提供给新企业或手动调整的企业。

## 现实逻辑

不同燃料对应不同的终端用途和设备适配。柴油常用于重型运输和柴油发电机，汽油主要对应轻型道路交通，液化石油气既可作为燃料也可作为居民和工业能源。本阶段仍采用服务配方抽象，没有模拟发动机类型、燃烧效率、排放许可或燃料储罐。

## 后续方向

- 将运输方式与柴油、汽油、液化石油气的适配关系细分。
- 把燃料价格、运输距离和车辆效率接入物流成本。
- 增加储罐容量、燃料安全和排放合规等经营约束。

### semiconductor-back-end-chain-correction-2026-09-06.md

# 半导体后段链路校正 — 2026-09-06

## 本次实现

- 将旧版 `chip_packaging` 的“晶圆 + 铜线 + 塑料粒子 → 封装芯片”配方识别为历史默认配方。
- 迁移后的默认路线为：晶圆切割得到裸 Die → 电性测试得到测试合格 Die → 引线框架/键合线/塑封料封装成成品芯片。
- 将旧版 `circuit_board` 的“晶圆直接做电路板”配方迁移为：阻焊 PCB + 焊料 + 表面贴装元件 + 封装芯片 → 电路板。
- 迁移只在输入、输出和配方 ID 完全匹配旧版默认值时触发；服务器管理员自定义过的同名配方不会被覆盖。

## 现实依据

半导体行业通常将前段晶圆制造与后段的切割、测试、封装区分开；封装后的芯片才进入电路板和终端产品装配。本次修改将这一边界反映到游戏生产链中，而不是让晶圆直接成为可装配芯片。

参考：Semiconductor Industry Association, How are semiconductors made: https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/

## 后续方向

- 后续可将良率、测试失败和封装等级接入现有批次质量系统。
- 暂不模拟光刻胶、特种气体和洁净室的全部细节，避免把现实工艺的复杂性简单堆成大量无意义物品。

### semiconductor-backend-packaging-2026-09-06.md

# 半导体后段封装链 - 2026-09-06

## 本次实现

- 新增硅裸片、测试裸片、引线框架和封装树脂四个中间商品。
- 晶圆经 `wafer_dicing_saw` 切割为硅裸片。
- 硅裸片经 `chip_testing_station` 进行测试，形成可封装的测试裸片。
- 铜箔与钢片经 `stamping_press` 制成引线框架。
- 环氧树脂与塑料颗粒经 `molding_compound_unit` 制成封装树脂。
- 测试裸片、引线框架、铜线和封装树脂经 `chip_packaging_line` 制成封装芯片。
- 为上述配方补齐 `chemical_reactor`、`lamination_press`、`alloy_furnace`、`pcb_assembly_line`、`wafer_dicing_saw`、`chip_testing_station`、`stamping_press` 和 `molding_compound_unit` 设备枚举，并设置购置材料、购置价和周期维护费；这些配方不会再因设备类型未注册而失败。
- 保留原有晶圆直接封装配方，兼容旧存档和旧自定义配方。

## 现实依据

半导体后段通常包含晶圆切割、芯片装配、键合、封装和测试；引线框架、键合线和封装材料共同构成封装结构。本阶段参考 [Semiconductor Industry Association 后段制造流程](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)。

## 当前抽象边界

- 暂未拆分晶圆良率、缺陷等级、键合方式和电气测试项目。
- 引线框架暂用铜箔与钢片代表铜合金/金属冲压过程。
- 设备名称表达独立工序，但仍沿用现有企业配方系统，不额外建立工厂图布局。

## 后续方向

- 让裸片测试结果影响封装芯片等级和电子产品良率。
- 增加封装类型、键合线材料和芯片测试设备维护成本。

### semiconductor-packaging-test-chain-2026-09-06.md

# 半导体封装测试链 - 2026-09-06

## 本次实现

- 新增封装芯片 `packaged_chip` 中间品。
- 新增芯片封装线 `chip_packaging_line`，消耗硅晶圆、铜线和塑料粒料，产出封装芯片。
- 新增完整可选配方 `circuit_board_with_packaged_chip`，由封装芯片继续装配电路板。
- 封装芯片加入商品目录、创造模式物品栏、模型和中英文名称。
- 旧的“硅晶圆直接装配电路板”配方保留作为兼容路径，完整路径由企业配方选择启用。

## 现实依据

现实半导体后端制造通常包括晶圆切割、裸片贴装、键合、封装和测试。完成测试的封装芯片才会交付给手机、汽车和计算机等下游电子产品厂商。本阶段把这些连续工序抽象为一条封装测试线，避免把每种键合线、引线框架和封装树脂都拆成单独物品，但保留独立的后端企业与中间品。

参考资料：

- [Semiconductor Industry Association - Stage 4: Back-end manufacturing](https://www.semiconductors.org/semiconductors-101/how-are-semiconductors-made/stage-4-back-end-manufacturing/)
- [Semiconductor Industry Association - Semiconductor production stages and business models](https://www.semiconductors.org/semiconductor-industry-primer/semiconductor-industry-primer-the-stages-of-production-and-business-models/)
- [U.S. Department of Energy - Semiconductor Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf)

## 当前抽象边界

- 暂不独立模拟晶圆切割、引线框架、键合线、封装树脂和电气测试仪器。
- 封装芯片暂不区分 CPU、存储器、射频芯片和电源管理芯片。
- 旧配方保留用于存档兼容；玩家可以通过 `/company recipes <公司名>` 查看配方，再用 `/company recipe <公司名> <配方ID>` 选择完整链条。

## 后续方向

- 引入封装良率和测试报废，使质量影响单位成本。
- 为手机主板增加不同芯片组合和规格档位。
- 将封装测试服务与跨企业采购、运输和质量验收衔接。

### semiconductor-polysilicon-wafer-chain-2026-09-06.md

# 多晶硅与硅晶圆上游链 - 2026-09-06

## 本次实现

- 新增高纯多晶硅中间品 `polysilicon`。
- 新增硅材料精炼设备 `silicon_refiner`，以石英和煤为输入生产多晶硅。
- 新增晶体生长炉 `crystal_growth_furnace`，以多晶硅生产硅晶圆。
- 新增商品目录、创造模式物品栏、模型和中英文名称。
- 既有直接“石英 → 硅晶圆”的配方保留为兼容配方；新增企业可选择更完整的 `silicon_wafer_from_polysilicon` 链条，避免破坏旧世界的自定义产业配置。

## 现实依据

半导体级硅不是把普通石英直接切成晶圆。现实流程通常先从含硅原料获得冶金级硅，再通过进一步提纯得到高纯多晶硅；多晶硅随后经过晶体生长形成单晶硅锭，再切片、研磨和清洗成为硅晶圆。本阶段将其中最关键的“多晶硅”和“晶体生长/晶圆”拆开，同时仍把复杂化学提纯、掺杂和抛光细节抽象掉。

参考资料：

- [U.S. Department of Energy - Solar Photovoltaic Manufacturing Basics](https://www.energy.gov/cmei/systems/solar-photovoltaic-manufacturing-basics)
- [U.S. Department of Energy - Semiconductor Supply Chain Deep Dive Assessment](https://www.energy.gov/sites/default/files/2022-02/Semiconductor%20Supply%20Chain%20Report%20-%20Final.pdf)
- [Semiconductor Industry Association - Polysilicon and wafer production background](https://www.semiconductors.org/wp-content/uploads/2025/08/Semiconductor-Industry-Association-SIA-Comments-Polysilicon-Section-232-Investigation.pdf)

## 当前抽象边界

- 暂不区分太阳能级与半导体级多晶硅。
- 暂不模拟氯硅烷、蒸馏、掺杂、光刻、刻蚀、CMP 等化学与晶圆制造工序。
- 旧的直接晶圆配方保留用于兼容已存在的自定义配置；完整产业链通过新配方显式提供。

## 后续方向

- 增加晶圆良率、洁净室投入和封装测试环节。
- 将半导体设备供应、备件和折旧接入企业财务报表。
- 让高质量芯片或电子产品规格影响售价、退货和售后维修。

### wood-products-chain-2026-09-06.md

# 木材与家具产业链 - 2026-09-06

## 本次实现

- 新增机器：锯木机 `sawmill` 与木材干燥窑 `dry_kiln`。
- 新增中间品：生材 `green_lumber`、窑干木材 `dried_lumber`。
- 新增消费品：木箱 `wooden_crate`、家具 `furniture`。
- 生产链为：橡木原木 → 锯切生材 → 干燥窑干木材 → 装配木箱/家具。
- 新物品进入商品市场、企业仓储、供应订单和成本结转，并加入创造模式物品栏。
- 同一条装配线可以继续生产电灯、罐装食品、木箱和家具，体现通用装配设备的多产品能力。

## 现实依据

美国农业部林务局资料指出，家具用硬木通常需要干燥以降低含水率、减少后续收缩和膨胀；现代木材厂会先锯切，再通过空气干燥或窑干获得适合家具加工的稳定材料。

参考：

- https://www.fpl.fs.usda.gov/documnts/fplgtr/fplgtr118.pdf
- https://research.fs.usda.gov/treesearch/7164
- https://research.fs.usda.gov/treesearch/14525

## 当前抽象边界

- 配方使用橡木原木作为统一木材原料，暂不区分树种、含水率、等级、胶黏剂、涂料和质量损耗。
- 家具目前作为一种可交易成品，尚未拆分板材、五金、表面处理和不同家具品类。