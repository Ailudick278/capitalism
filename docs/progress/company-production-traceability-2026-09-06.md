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
