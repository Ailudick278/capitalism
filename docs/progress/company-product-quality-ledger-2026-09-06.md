# 企业产品质量台账 - 2026-09-06

## 本次实现

- 新增企业产品质量台账，按企业和商品记录生产数量与加权质量分数。
- 新生产批次根据员工平均技能和生产设备状态计算一个 0–100 的过程质量代理值。
- 企业供货销售会按销售数量比例消耗质量台账；企业合并会合并加权质量历史。
- `/company metrics <公司名> <天数>` 显示新生产库存的平均记录质量。
- 新增只读命令 `/company quality <公司名>`，按商品列出质量台账数量和平均分。
- 质量台账独立于仓库数量和存货成本层，不改变当前售价、库存数量、税务或生产产量。

## 现实逻辑

现实制造业会把生产过程、设备、人员和产品质量数据关联起来，并通过可追溯记录支持质量改进。当前实现采用游戏化的加权批次台账，先保留批次质量信息，再为后续的检验良率、退货、返工和售后服务提供数据基础。

参考资料：

- [NIST - Trustworthiness and Traceability of Supply Chain Data](https://www.nist.gov/ctl/smart-connected-systems-division/smart-connected-manufacturing-systems-group/trustworthiness-and)
- [NIST - Assessing Process Capability](https://www.itl.nist.gov/div898/handbook/ppc/section4/ppc46.htm)

## 当前抽象边界

- 质量分数是过程质量代理值，不是现实标准认证或法定合格判定。
- 尚未拆分缺陷类型、检验设备、报废品、返工和批次序列号。
- 旧存量没有质量台账，因此报表只统计新生产并被记录的批次。

## 后续方向

- 为半导体封装、显示面板和电池等工序增加检验良率和报废物料。
- 将质量分数与客户验收、退货、保修和售后维修连接。
- 继续增加按批次查看质量分布，而不是只显示当前商品平均值。
