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
