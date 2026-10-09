# 星界躯体 KubeJS 示例

十字容器、六分数徽章、等级条件及按相邻徽章 tag 计数的完整示例见[开发者快速指南](../../docs/星界饰品-开发者快速指南.md)。

适用于 Forge 1.20.1 和 NeoForge 1.21.1。安装对应版本的 KubeJS 后，将 `startup_scripts/astral_fillers.js` 复制到实例的 `kubejs/startup_scripts/`。示例创建五件一格填充物，展示固定分、玩家条件、容器内物品数量条件、相邻填充物条件、按分数加分、分数转换和职业勋章。

共用物品 ID 的新节点示例是 [`startup_scripts/astral_shared_gem_api.js`](startup_scripts/astral_shared_gem_api.js)，可与原示例一起复制。它创建一件物品，根据每个栈的 `demo.gem` 数据选择紫水晶 α 或石英 β 的规则。两种身份须用带有完整 schema 1 payload 的物品栈体验；没有 payload 的栈保留在格子里但不提供分数。数据格式、Java 例子、动态加工与徽章接口见[API 扩展开发者指南](../../docs/星界宝石系统-API扩展开发者指南.md)。

动态加工示例是 [`startup_scripts/astral_dynamic_process.js`](startup_scripts/astral_dynamic_process.js)。将对应版本的 [`gem_to_filler.json`](recipes/forge-1.20.1/gem_to_filler.json) 或 [NeoForge 配方](recipes/neoforge-1.21.1/gem_to_filler.json) 放进数据包的 `data/<namespace>/recipes/`（Forge）或 `data/<namespace>/recipe/`（NeoForge），并将其中的命名空间与文件 ID 调整为实际路径。配方用具名 `gem` 输入读取原紫水晶的完整 payload，预览与完成都复制原值到共用填充物，不重新随机生成三维。原紫水晶需预先带 `demo.gem` 数据，`source_item` 写 `minecraft:amethyst_shard`、`primary` 写 `alpha`；同一共用填充物也可带 `source_item: minecraft:quartz`、`primary: beta` 的另一份数据用于第二个规则。最小 payload 还需 `schema:1`、`natural:{size:200,purity:100,polish:50}`、`affixes:[{stat:"size",operation:"flat",value:5}]`、`reinforcements:[]` 与 `extensions:{}`。Forge 将 `demo.gem` 放在物品根 NBT；NeoForge 放在 `minecraft:custom_data` 组件。

脚本中的红宝石、蓝宝石等名称和分数是**技术示例**，不属于项目的正式宝石／星体／星座设定。正式内容应遵循“宝石承载星体力量，星座决定组合规则”，待具体对应和组合规则确认后再命名。

公开脚本绑定：`AstralContainers.create(width, height, ...rows)`、`AstralFillers.register(itemId, definition)`、`FillerDefinition.builder()`、`FillerActivation`、`ScoreType` 和 `AstralRules`。`itemId` 必须为带命名空间的物品 ID。也可以给已有物品注册填充物定义，无须先创建新物品。每个 ID 只能注册一次；修改定义后重启游戏，不要在同一次启动中重复执行注册语句。

用 `.activation(FillerActivation.UNIQUE_WORN)` 等调用设置生效类型；可选值为 `UNIQUE_WORN`、`STACKABLE`、`UNIQUE_EFFECT`，默认 `STACKABLE`。示例蓝宝石使用 `.suppressedBy('kubejs:astral_opal')`：容器开放格有欧泊时蓝宝石留在格子里但暂时不生效。示例勋章使用 `UNIQUE_EFFECT`，多枚可存放但同 ID 只结算第一枚。

相邻条件写法：`.bonus(AstralRules.whenAdjacent('kubejs:astral_ruby', ScoreType.INTELLIGENCE, 2))`。蓝宝石的上下左右任一开放且有效的格子放有星界红宝石时，蓝宝石增加 2 智力；斜角、关闭格及冲突／重复失效的物品不计。同一种相邻物品出现多件时，这条规则也只加一次。`AstralRules.whenFillerCount(itemId, minimum, target, amount)` 同样只统计开放且有效的物品。

规则由同一逐格节点引擎结算：全部基础分先完成，读取计划按依赖排序，再汇总并计算勋章属性。固定规则构建方法由节点适配器执行；附加分读取本轮基础分，转换按格子顺序读取已完成的修正结果，来源不足时按可用量缩小。`AstralRules` 是固定规则的便捷工厂。JavaScript 节点回调通过 `FillerNode`、`ReadPlan` 和 `AstralNumber` 定义。`convert(source, target, fraction, targetMultiplier)` 扣除 `当前来源分数 × fraction`，再按倍率增加目标；省略倍率时为 1。

示例勋章使用 `AstralRules.warriorMedal()`，按最终力量 `s` 提供 `ln(1+max(0,s)/15) × 100%` 的最终攻击力加成；`s=15` 时约 +69.3%，负力量按 0 分计算。两个目标版本均使用攻击力属性 ID `minecraft:generic.attack_damage`。需要线性百分比时可用 `AstralRules.attributeMultiplyTotalPerScore(...)`，其中 `0.01` 代表每点 +1%。注册绑定只在 `startup_scripts` 中提供；数据读写与大数工具也可在其他脚本阶段使用，`AstralRuntime` 仅在 `server_scripts` 中提供。

规则工厂可直接用于脚本定义。例如 `.bonus(AstralRules.percentOf(ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.25, 4))` 按基础力量的 25% 获得感应，单件最多 +4；`.bonus(AstralRules.percentWhenAdjacent('kubejs:astral_ruby', ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.30, 4))` 只在有效正交相邻时结算；`.bonus(AstralRules.percentWhenCountAtLeast('kubejs:astral_ruby', 2, ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.30, 4))` 要求至少两件有效红宝石。玩家条件可用 `whenExperienceLevelAbove(30, target, amount)`、`whenHealthAbove(16, target, amount)`、`whenHealthAtMost(8, target, amount)`、`whenFoodAtLeast(18, target, amount)` 和 `whenNightVision(target, amount)`。两种生命阈值分别使用 `>` 与 `<=`；百分比读取同一轮基础阶段快照，不会重复放大同阶段加分。
