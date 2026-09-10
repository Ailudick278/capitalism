# 按现实行业完善工业产业链方案

## 目标

将模组中的工业配方从“可生产”提升为“上下游关系接近现实行业”的产业链模型。建议统一采用：

```text
资源开采 → 选矿/初加工 → 精炼/基础材料 → 中间材料 → 零部件 → 总成/终端产品 → 回收再生
```

现实工业资料显示，炼油厂会通过分离、转化和处理把原油转成燃料与化工原料；石脑油等中间馏分还会继续进入蒸汽裂解，生成乙烯、丙烯等化工基础原料。[EIA 炼油流程](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-the-refining-process.php)、[EIA 炼油输入输出](https://www.eia.gov/energyexplained/oil-and-petroleum-products/refining-crude-oil-inputs-and-outputs.php)

## 一、矿产与金属基础层

### 铝

```text
铝土矿 → 氧化铝 → 原铝 → 铝板/铝箔/铝型材 → 电子外壳、交通设备、电池集流体
```

当前已有铝土矿和铝锭，建议补充 `alumina` 与电解铝步骤，不要让铝土矿直接进入冶炼。

### 铜

```text
铜矿 → 铜精矿 → 粗铜/阳极铜 → 阴极铜 → 铜线/铜箔 → 电机、线缆、PCB、电子元件
```

当前铜矿直接进入铜锭，建议增加铜精矿、粗铜或阴极铜中的至少一层。

### 镍

```text
镍矿 → 镍精矿 → 镍中间品 → 镍金属/硫酸镍 → 不锈钢、NMC 正极材料
```

当前已有粗镍和镍锭，但缺少硫酸镍分支；电池产业链应使用电池级硫酸镍，而不是直接使用镍锭。

### 其他关键矿物

IEA 将铝、铜、铅、锡、锌列为基础金属，将锂、镍、钴、石墨、锰和磷酸等列为电池材料，并指出半导体、机器人和数字产业还会依赖镓、锗、铟、钨等战略小金属。[IEA Global Critical Minerals Outlook 2026](https://www.iea.org/reports/global-critical-minerals-outlook-2026/executive-summary)

建议后续按优先级补充：

1. 磷酸盐 → 磷酸 → 磷酸铁 → LFP 正极材料
2. 锰矿 → 硫酸锰 → NMC 正极材料
3. 钴矿/钴中间品 → 硫酸钴 → NMC 正极材料
4. 锡/银/金 → 焊料和电子回收副产物

## 二、石化与塑料层

```text
原油 → 常压/减压馏分 → 石脑油、燃料油、柴油、汽油、LPG、沥青
石脑油 → 蒸汽裂解 → 乙烯/丙烯
乙烯 → 聚乙烯、乙二醇、苯乙烯路线
丙烯 → 聚丙烯、合成橡胶路线
芳烃 → 苯、对二甲苯 → 苯乙烯、PTA → PET
```

现有石化链已经覆盖原油、石脑油、乙烯、丙烯、塑料粒子、ABS 和 PET，建议补充：

- 原油分馏，而不是单一 `refining` 直接产出所有产品
- 氢气、硫磺/硫酸等炼化副产物或处理成本
- PTA、MEG、PET 的明确命名和顺序
- 聚合物颗粒 → 薄膜/纤维/树脂件的加工分支

## 三、半导体产业链

半导体现实流程通常分为前段晶圆制造和后段封装测试。行业资料列出的步骤包括光刻、刻蚀、沉积、离子注入、金属化，以及晶圆切割、封装和测试。[SIA 半导体制造流程资料](https://www.semiconductors.org/wp-content/uploads/2026/03/Senate-EPW-testimony-3.4.2026.pdf)

建议改为：

```text
石英砂/高纯石英
 → 冶金级硅
 → 多晶硅
 → 单晶硅锭
 → 硅晶圆
 → 光刻胶/掩膜/特气/化学品
 → 晶圆制造
 → 晶圆测试
 → 切割晶粒
 → 引线框架/封装基板/塑封料
 → 封装芯片
 → 成品测试
```

当前链条从 `silicon_wafer` 较快跳到 `silicon_die`，建议新增：

- `silicon_ingot`
- `photoresist`
- `photomask`
- `process_gas`
- `wafer_fabrication`
- `wafer_tested`
- `lead_frame`
- `mold_compound`
- `final_chip_test`

这样可以把晶圆前段、后段封装和测试分成不同设备与企业类型。

## 四、PCB 与电子装配

```text
玻璃纤维 + 环氧树脂 → 覆铜板
覆铜板 → 钻孔 → 沉铜/电镀 → 蚀刻 → 阻焊/字符
被动元件 + 芯片 + 连接器 → SMT 贴装 → PCB 组装 → ICT/功能测试
```

当前已有 `pcb_substrate`、`copper_foil`、钻孔、蚀刻、阻焊和组装配方。建议将 `circuit_board` 拆成：

1. `copper_clad_laminate`
2. `drilled_pcb_panel`
3. `etched_pcb`
4. `solder_masked_pcb`
5. `assembled_pcb`
6. `tested_pcb`

同时把 `packaged_chip`、`passive_components`、连接器和焊料作为 SMT 装配输入，而不是让裸晶圆直接参与消费电子组装。

## 五、锂电池产业链

```text
锂矿/盐湖 → 锂精矿/锂盐 → 碳酸锂或氢氧化锂
镍/钴/锰/铁/磷 → 正极前驱体 → 正极活性材料
石墨 → 球形石墨/人造石墨 → 负极材料
聚合物 → 隔膜
锂盐 + 有机溶剂 → 电解液
正极 + 负极 + 隔膜 + 电解液 → 电芯
电芯 → 模组 → 电池包 → 终端设备/储能
```

IEA 指出，LFP 与镍系 NMC 是不同的电池材料体系，LFP 供应链需要磷酸、铁和石墨，不应与镍钴锰路线混为一条配方。[IEA 电池技术供应链](https://www.iea.org/reports/global-critical-minerals-outlook-2025/beyond-nmc-batteries-supply-chain-issues-for-emerging-battery-technologies)

建议将当前电池链分为：

- NMC：锂盐 + 镍盐 + 钴盐 + 锰盐 → NMC 正极 → 电芯
- LFP：锂盐 + 磷酸铁 → LFP 正极 → 电芯
- 通用部件：石墨负极、隔膜、电解液、铜箔、铝箔

当前的 `battery_pack → black_mass → hydrometallurgy` 方向是合理的，但应补充“拆包/放电/破碎/分选/湿法冶金”阶段。IEA 认为回收是重要的二次供应来源，但目前仍受收集率和处理能力限制。[IEA 电池回收](https://www.iea.org/reports/recycling-of-critical-minerals/executive-summary)

## 六、回收与再制造

建议统一为：

```text
废旧产品 → 收集 → 拆解/放电 → 破碎 → 分选
 → 黑粉/金属混合物 → 湿法或火法冶金 → 金属盐/再生材料
```

回收不应直接把完整消费电子产品变成铜线和塑料粒子；应根据产品类型产生不同的中间物料，并允许回收率、污染、处理成本和物流距离影响最终产出。

## 优先修改顺序

1. 先修正铜、铝、镍的矿产→精炼层，建立金属基础材料。
2. 拆分炼油与石化中间体，补充氢气、硫酸/硫磺和 PTA/MEG。
3. 补齐半导体前段与后段：晶圆制造→测试→切割→封装。
4. 将 PCB 组装与芯片、被动元件、连接器连接起来。
5. 将电池拆成 NMC、LFP 两条材料路线。
6. 最后完善回收、再制造、副产物和物流结算。

## 对当前模组的直接判断

- 当前最成熟的是石化—塑料—电子产品主链。
- 当前最需要修正的是金属精炼层和半导体前段。
- 当前最值得增加玩法深度的是 LFP/NMC 电池分化与回收物流。
- 原版 Minecraft 物料可以继续作为基础资源，但应在工业系统中明确区分“自然资源”“工业中间品”和“可回收材料”。
