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
