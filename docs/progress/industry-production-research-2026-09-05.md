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
