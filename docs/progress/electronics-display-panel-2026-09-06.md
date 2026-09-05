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
