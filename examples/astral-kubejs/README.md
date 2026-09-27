# 星界躯体 KubeJS 示例

十字容器、六分数徽章、等级条件及按相邻徽章 tag 计数的完整示例见[开发者快速指南](../../docs/星界饰品-开发者快速指南.md)。

适用于 Forge 1.20.1 和 NeoForge 1.21.1。安装对应版本的 KubeJS 后，将 `startup_scripts/astral_fillers.js` 复制到实例的 `kubejs/startup_scripts/`。示例创建五件一格填充物，展示固定分、玩家条件、容器内物品数量条件、相邻填充物条件、按分数加分、分数转换和职业勋章。

脚本中的红宝石、蓝宝石等名称和分数是**技术示例**，不属于项目的正式宝石／星体／星座设定。正式内容应遵循“宝石承载星体力量，星座决定组合规则”，待具体对应和组合规则确认后再命名。

公开脚本绑定：`AstralContainers.create(width, height, ...rows)`、`AstralFillers.register(itemId, definition)`、`FillerDefinition.builder()`、`FillerActivation`、`ScoreType` 和 `AstralRules`。`itemId` 必须为带命名空间的物品 ID。也可以给已有物品注册填充物定义，无须先创建新物品。每个 ID 只能注册一次；修改定义后重启游戏，不要在同一次启动中重复执行注册语句。

用 `.activation(FillerActivation.UNIQUE_WORN)` 等调用设置生效类型；可选值为 `UNIQUE_WORN`、`STACKABLE`、`UNIQUE_EFFECT`，默认 `STACKABLE`。示例蓝宝石使用 `.suppressedBy('kubejs:astral_opal')`：容器开放格有欧泊时蓝宝石留在格子里但暂时不生效。示例勋章使用 `UNIQUE_EFFECT`，多枚可存放但同 ID 只结算第一枚。

相邻条件写法：`.bonus(AstralRules.whenAdjacent('kubejs:astral_ruby', ScoreType.INTELLIGENCE, 2))`。蓝宝石的上下左右任一开放且有效的格子放有星界红宝石时，蓝宝石增加 2 智力；斜角、关闭格及冲突／重复失效的物品不计。同一种相邻物品出现多件时，这条规则也只加一次。`AstralRules.whenFillerCount(itemId, minimum, target, amount)` 同样只统计开放且有效的物品。

规则结算顺序：基础分、附加分、转换、勋章属性。附加分规则读取同一阶段开始时的分数快照；转换按格子顺序读取当时剩余分数，来源不足时按可用量缩小本条转换。`AstralRules` 是纯 Java 规则工厂，避免脚本回调在两版 Rhino 中的类型转换和服务器执行差异。任意 JavaScript 回调规则暂未提供稳定跨版本封装。`convert(source, target, fraction, targetMultiplier)` 扣除 `当前来源分数 × fraction`，再按倍率增加目标；省略倍率时为 1。

示例勋章使用 `AstralRules.warriorMedal()`，按最终力量 `s` 提供 `ln(1+max(0,s)/15) × 100%` 的最终攻击力加成；`s=15` 时约 +69.3%，负力量按 0 分计算。两个目标版本均使用攻击力属性 ID `minecraft:generic.attack_damage`。需要线性百分比时可用 `AstralRules.attributeMultiplyTotalPerScore(...)`，其中 `0.01` 代表每点 +1%。这些绑定只在 `startup_scripts` 中提供，防止运行时脚本重复注册填充物。

新增规则可直接用于脚本定义。例如 `.bonus(AstralRules.percentOf(ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.25, 4))` 按基础力量的 25% 获得感应，单件最多 +4；`.bonus(AstralRules.percentWhenAdjacent('kubejs:astral_ruby', ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.30, 4))` 只在有效正交相邻时结算；`.bonus(AstralRules.percentWhenCountAtLeast('kubejs:astral_ruby', 2, ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.30, 4))` 要求至少两件有效红宝石。玩家条件可用 `whenExperienceLevelAbove(30, target, amount)`、`whenHealthAbove(16, target, amount)`、`whenHealthAtMost(8, target, amount)`、`whenFoodAtLeast(18, target, amount)` 和 `whenNightVision(target, amount)`。两种生命阈值分别使用 `>` 与 `<=`；百分比读取同一轮基础阶段快照，不会重复放大同阶段加分。
