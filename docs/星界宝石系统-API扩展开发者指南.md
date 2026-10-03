# 星界宝石系统 API 扩展开发者指南

本指南对应 Forge 1.20.1、NeoForge 1.21.1 的当前实现。星界躯体仍只在装备于 Curios 槽位时提供属性效果。本轮交付通用 API 与示例；外部草案中的 34 种宝石目录、未审机制、正式共用物品 ID、配方和数值不在模组中硬编码。

## 一次结算，一格一个节点

`AstralFillers.register(itemId, definition)` 仍只注册一次。对于共用物品 ID，在 `FillerDefinition.Builder.node(resolver)` 中读取**每个实际物品栈**的数据，返回一个 `FillerNode`。相同 ID、相同规则键但位于不同格子的两颗宝石有独立的 B（基础分）、F（最终分）、Δ（`max(F-B,0)`）和 g（`F>0 ? Δ/F : 0`）。六类分数用 `BigScoreSheet` 和 `AstralNumber` 表示；普通节点不允许负分，显式声明的全局修正节点可输出有符号分数。

本模组内置的固定填充物也通过逐格节点引擎结算。固定规则的附加分读取本轮全部节点基础分，转换规则依格序读取已完成的修正节点；没有第二条旧分数管线。旧 `base/bonus/conversion/medal` 构建方法保留为便利接口，旧 `medal` 和新 `bigMedal` 都读取最终六分汇总。大分数建议用 `bigMedal`，先在 `AstralNumber` 上压缩，再向 `AttributeWriter` 提交有限 `double`。

节点计算顺序：解析每格 → 全部基础分 → 声明读取计划 → 按最终分依赖排序计算 → 汇总 → 徽章转属性。`ReadPlan.metadata/baseScores/finalScores` 的来源在声明时固定；只有 `finalScores` 建立依赖。闭环按格序兜底，尚未完成的来源不可读，并写入诊断。`calculate` 抛异常时该节点回退至 B；`intrinsic` 失败时保留物品但该格不生效。读取到的来源始终是副本，不能扣除来源自身的分数。

## 规范宝石数据

`AstralItemData.read/writeCopy/removeCopy` 在 Forge 用物品 NBT，在 NeoForge 用 `minecraft:custom_data`。读写均复制，传入的物品栈不会被修改。`GemPayload` schema 1 字段为 `source_item`、`primary`（`alpha/beta/gamma/delta`）、`natural.size/purity/polish`、`affixes`、`reinforcements`、`extensions`。α/β/γ/δ 可以作为输入别名，写出统一为英文稳定值。天然范围分别是 1–500、1–200、1–100；自然修饰最多 5 条，并校验草案给出的单条上下限。强化条目独立保存，尚未定义额外上限。

`GemPayload.decode(data)` 检查结构；附属需再用 `decode(data, allowedIdentities)` 或自己的规则表验证 `source_item#primary`。模组不自带 34 种来源清单。有效三维每次从天然值和修饰重新计算：`max(1, (natural + sumFlat) * (1 + sumPercent))`。分数和最终值不写入输入 payload。

`GemPayloadTransfer.toFiller` 复制原石 payload 到共用填充物，并核对源物品 ID；`embed` 将 payload 的独立副本放进徽章的单一 `gem` 槽；`update` 和 `updateEmbedded` 在保留种类与主修饰的前提下分别修改填充物和徽章内的数据。这些方法均返回新物品栈。服务端将结果写回真实容器格后，调用 `AstralRuntime.invalidate(player, container)`，或直接通过 `updateCell` 原子写回。旧数据用 `GemPayloadMigrations.version/register/migrateCopy` 显式迁移；没有注册迁移器时返回 `gem_schema_requires_migration`，不会猜测旧字符串的数值或字母。

## Java：同一物品 ID 的两个规则

以下假定附属已经注册 `addon:shared_gem`，且该物品继承 `AstralFillerItem`。物品数据保存在 `addon.gem`；允许名单与数值仅作示例。

```java
Set<String> allowed = Set.of(
    "minecraft:amethyst_shard#alpha",
    "minecraft:quartz#beta");

AstralFillers.register("addon:shared_gem", FillerDefinition.builder()
    .activation(FillerActivation.STACKABLE)
    .node(instance -> {
        var parsed = GemPayload.decode(
            AstralItemData.read(instance.filler(), "addon.gem"), allowed);
        if (!parsed.ok()) return FillerNode.invalid(parsed.error());
        GemPayload gem = parsed.value();
        boolean amethyst = gem.primary().equals("alpha");
        return FillerNode.builder()
            .kindKey(gem.sourceItem())
            .variantKey(gem.primary())
            .ruleKey("addon:gem/" + (amethyst ? "amethyst/alpha" : "quartz/beta"))
            .role(amethyst ? "receiver" : "producer")
            .data(gem.encode())
            .intrinsic((ctx, data, out) -> {
                GemPayload own = GemPayload.decode(data).value();
                if (amethyst) out.add(ScoreType.STRENGTH,
                    AstralNumber.fromDouble(own.effective(GemPayload.Stat.SIZE) / 100));
                else out.add(ScoreType.MAGIC,
                    AstralNumber.fromDouble(own.effective(GemPayload.Stat.PURITY) / 100));
            })
            .reads((ctx, data) -> ReadPlan.builder()
                .baseScores("adjacent_quartz", ctx.orthogonal(),
                    FillerFilter.kind("minecraft:quartz"))
                .build())
            .calculate((ctx, data, inputs, out) -> {
                if (amethyst) out.add(ScoreType.STRENGTH,
                    inputs.sumScores("adjacent_quartz").get(ScoreType.MAGIC)
                        .multiply(AstralNumber.parse("0.25")));
            })
            .build();
    }).build());
```

`FillerContext.cellAt/offset/orthogonal/diagonal/opposedPairs/clockwiseNeighbors/mirrorCell/squaresContainingSelf/ray/connected` 查询真实格子视图。`occupied` 与 `effective` 分开；关闭格、冲突格和坏数据仍可被物理检查，但不能充当生效来源。`FillerFilter.item/kind/variant/rule/role/anyOfRules/where` 支持组合；共用 ID 的种类统计必须用逻辑过滤，不能用 `count(itemId)`。`ctx.baseAt(cell)` 读本轮基础结果；`ctx.resultAt(cell)` 只允许已在 `finalScores` 中声明的来源。`inputs.results(name)`、`highest/lowest/sumScores/localCopy` 用于读取独立来源副本。

`PlayerStateSnapshot` 由一轮共享，提供生命、等级、饥饿、地面与潜行、主手耐久、维度、时间、天气、玩家标签及按完整属性 ID 查询的当前实际属性。规则应读取 `ctx.state()`，不要在回调中修改玩家或物品。属性查询由运行时防重入保护。

## KubeJS

注册放在 `kubejs/startup_scripts`，运行时物品写回与 `AstralRuntime` 放在 `kubejs/server_scripts`。可复制的双身份最小示例见 [astral_shared_gem_api.js](../examples/astral-kubejs/startup_scripts/astral_shared_gem_api.js)。这些绑定可用：`FillerNode`、`ReadPlan`、`FillerFilter`、`AstralNumber`、`BigScoreSheet`、`AstralItemData`、`GemPayload`、`GemPayloadTransfer`、`GemPayloadMigrations`、`AstralDataSchemas`、`AstralTransforms`、`AstralDataPredicates`、`FillerPresentations`。`AstralRuntime` 仅在 SERVER 脚本暴露。STARTUP 注册规则、schema、谓词、transform 和展示提供者；不要在运行时重复注册。

脚本中用 `AstralNumber.parse("1e400")`、`fromDouble(0.25)` 和显式方法链，不要先把超大分数转成 JavaScript Number。`GemPayload.decode(data)` 是无重载歧义的入口。`FillerResultSummary`、`CellView`、`PlayerStateSnapshot` 具备 Java getter；[类型声明](../examples/astral-kubejs/astral-api.d.ts)提供编辑器补全参考。实际运行时类型以 ProbeJS 生成结果为准。

## 动态加工与徽章

两类数据配方的 `stack` 和 `result` 保留完整 ItemStack 数据。锻造原料可用 `stack` 或 `tag`，投影步骤也可使用二者；默认精确匹配物品及 NBT／组件。变量宝石可写 `data_predicate` 并设置 `exact_data:false`，然后用 `AstralDataPredicates.register(id, predicate)` 检查每份真实输入。配方可写 `data_transform` 引用 `AstralTransforms.register(id, matches, preview, produce)`。锻造无序原料通过 `name` 进入 `ProcessContext.source(name)`，投影步骤也有名字；预览不消耗，完成时重读输入、检查匹配与消耗，再提交产物和返还物。`ProcessOutput` 的物品都是完整栈副本。示例配方与回调见本指南对应的 [动态工艺示例](../examples/astral-kubejs/README.md)。

专属徽章可用 `.effectGroup("addon:gem_badge", EffectScope.PLAYER)` 限制全部已装备星界躯体中只有一枚生效；顺序按 Curios 槽名、槽序、容器格序稳定选择。`.bigMedal((ctx, scores, out) -> ...)` 使用全容器汇总分数（每个容器独立计分）并通过 `out.add/multiplyBase/multiplyTotal` 输出有限属性值。`AstralFillers.disable("deeprealm_4th:warrior_medal")` 等入口可以在 STARTUP 禁用内置勋章；默认不禁用。具体专属徽章物品、镶嵌成本和曲线尚未定稿。

`FillerPresentations.register(itemId, provider)` 由附属为继承 `AstralFillerItem` 的共用物品设置动态名称、静态说明及基于 `FillerResultSummary` 的提示。普通 KubeJS 物品可用 `registerName`，并在生成或修改物品数据后通过 `applyNameCopy` 将当前名称写到产物副本；`GemPayloadTransfer` 已调用此方法。佩戴时的每格摘要随容器物品栈同步；未佩戴的容器界面会只读预览。摘要含身份、B/F/Δ/g、最终六分、各读取端口的候选格、计算状态和诊断，不作为下一轮计算输入。
