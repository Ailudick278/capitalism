# Capitalism Mod - calendar progress log

Consolidated on: 2026-09-06

## Current system log

# 万年历与时间系统开发进度

最后更新：2026-09-05

## 已实现功能

- 提供独立的万年历/游戏时间换算基础类。
- 支持按游戏时间计算日期、周期和税务周期所需的时间节点。
- 日历命令和税务、租金、利息、市场结算等周期性逻辑已经具备接入基础。

## 当前边界与待处理问题

- 日历目前主要承担时间换算，节日、季节、月度政策和地区历法尚未扩展。
- 各系统的周期结算仍需要统一使用同一套日历服务，避免分别计算。

## 后续开发方向

1. 统一游戏时间、现实时间和经济周期的换算接口。
2. 为税务、租金、利息、债券和企业账期提供统一结算日。
3. 增加月、季、年、节日和政策事件，为经济模拟提供时间驱动。
4. 在界面中显示当前游戏日期、周期和下一次结算时间。

## Consolidated historical entries

### calendar-time-source-2026-09-05.md

# Calendar time-source progress log — 2026-09-05

## Implemented

- Perpetual-calendar dates now derive from monotonic `gameTime`, matching the time source used by economic settlement systems.
- `/time set` can still change the visual Minecraft clock (`dayTime`) without changing the economic calendar date.
- Added regression tests for Gregorian day mapping and Minecraft tick-to-clock conversion.
- Calendar command day counters now use the same `gameTime` source as the displayed date.
- Added a shared overflow-safe `ticksForDays` conversion utility for future economic periods.
- Company, individual-business, and annual tax-report periods now use that shared conversion utility without changing their existing 90/360-day values.
- Land leases, land transfer windows, tax late fees, refund windows, enforcement notices, and periodic settlement handlers now reference the shared day constant/conversion utility instead of duplicating `24000`.

## Design boundary

- The stable epoch remains 2000-01-01.
- Financial period lengths such as 90-day quarters and 360-day configured years were not changed in this phase.

## Follow-up

- Update the calendar command text so every displayed day counter explicitly uses the same game-time source.
- Add a shared period utility for systems that currently repeat raw `24000` conversions.