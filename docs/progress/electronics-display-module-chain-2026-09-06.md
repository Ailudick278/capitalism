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
