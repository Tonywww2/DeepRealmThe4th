# 漩涡 B 方案：实现与验证

本文保留 0.1.0 的历史实现和验证记录；当前 0.1.1 已调整噪声、中心断崖及测试存档，见 [自然地形与 Y60 断崖](自然地形与Y60断崖-实现与验证.md)。`build/reports/terrain*` 会随最新构建更新，旧算法与预览已另存 `build/reports/vortex-v4-before/`。

## 本次实现

按确认的中等剪切、自然破碎方向实现，不使用概念图作为生成效果的证据。所有地形预览均来自实际 Java 采样器。

- **高度**：保留半径约 64 的急降；42～86 格以一阶连续曲线过渡，86 格外径向基线导数严格为旧曲线的 0.1 倍。丘陵、波浪和山脊使用独立幅度，没有一起压缩为十分之一。
- **轮廓**：名义 75% 陆地、25% 虚空和 `twist=1.44` 不变。按种子扰动相位和带宽，在解旋坐标里生成沿切向拉长的裂隙与碎片。中心空洞唯一、没有重复漏斗。
- **群系**：用连续扭曲气候场替代 Voronoi 分区随机选择；主题切换不再决定山地的整体高度开关。四条大生态带保留，内部地形与群系交错细分。
- **河流**：低落差流域含弯曲主干、两支汇入的支流、变化的宽深与终端湖泊；河谷参与高度侵蚀。裂隙附近提前退水，危险断面形成封闭岸壁，不向虚空泄水。当前是解析流域模型，不是全局水文模拟，也不承诺所有流域相互连通。
- **海岸**：海平面固定 Y=64。原始海盆边界向内按切比雪夫距离收缩三格，外围形成连通的砂岩芯墙，顶层为有支撑的沙子，墙顶至少 Y=65，墙底连接邻近海床。该办法包含斜角和跨区块边界，不是仅一层贴皮。
- **地物**：每个装饰阶段收集邻域真实群系的原生 placed features（保留 biome filter），按稳定键排序并以世界种子派生随机源执行。接入原生树木、花草、矿脉、菌类、海草、珊瑚等尝试；增加绯红/诡异森林池和相应菌岩地表，群系总数从 27 增到 29。
- **生物**：使用原生群系刷怪表、区块初始生成和正常 `NaturalSpawner` 路径；修复海平面 -64 导致的水生高度条件，并把固定 0 的怪物光照阈值改为主世界标准 0～7 随机阈值，方块光上限仍为 0。保留猪灵安全属性。

## 生成保护和兼容边界

按 MC-Dev-Skills 的统一采样与实际服务端验证要求，区块填充、基础高度/列查询、群系、生成保护和预览共用 `TerrainProfile`。原生群系基质仅在表层替换材料，不改变列高度。

原生装饰在有作用域的 ThreadLocal 内运行，退出必定清理。保护覆盖 `WorldGenRegion`、`ProtoChunk/LevelChunk` 和 `LevelChunkSection` 三条写入路径；最后一层覆盖了原版矿脉的 `BulkSectionAccess`。拒绝虚空落块、挖穿底壳、修改砂岩护岸、掏空湿海床或放置盆地外液体。玩家后续建造/破坏和其他维度不受这些生成期保护限制。装饰候选读取 3×3 区块内的原始 quart 群系网格，不通过带额外模糊邻域的 `getBiome` 收集，避免 1.21 生成阶段的邻域越界。

地物调用的返回值可能在全部写入被拦截后仍为 true；`ecology` 的 `nativeReportedSuccesses`/`reportedFeatureIds` 只是原生返回值，`allowedWriteChecks`/`blockedWriteChecks` 是各接口检查次数，不能当作最终方块数量。生态回归另行扫描实际方块。

依赖自定义噪声生成器、绕过标准写入接口或有额外环境要求的第三方 features 仍需单独适配；运行异常按 feature ID 限次记录，并在测试中判失败。未执行原生结构和洞穴雕刻。不改写原生物种的全局刷怪规则，也不保证所有模组物种无条件生成。

Forge 1.20.1 的 Mixin 配置带 SRG refmap；NeoForge 1.21.1 使用 Mojmap，不携带该 refmap。维度光照 IntProvider 的 `value` 包装也按版本转换。

## 算法回归

两个节点的 `verifyTerrain` 结果一致：

```text
TERRAIN_VERIFICATION_OK
assertions=3157673
seeds=0,42,-739221
wetColumns=41739
measuredLandCoverage=0.7486910759063428
shoreColumns=4416
outerRadialSlope=10%
oceanWallMinimumWidth=3
oceanSeaLevel=64
```

面积统计使用半径 128～2000 的圆环内网格，避免正方形四角给旋转分区引入偏差。检查还覆盖中心、15 类主题、不同种子、三格护岸（包括斜角）、水床厚度、邻接流体约束和多线程/逆序采样。

实际算法图：[总览](../build/reports/terrain/overview.png)、[新旧径向高度对比](../build/reports/terrain/radial-profile.png)。这些不是游戏内截图。

### 传送落点搜索补测

2026-09-23 继续检查时发现，原搜索按两格步进，仅访问一种 X/Z 奇偶组合，会漏掉紧邻目标的唯一安全落点。已抽取共享的 `LandingSearch` 并改为一格步进；`enter` 和 `return` 都复用该逻辑。仍按切比雪夫距离由近至远搜索周围 32 格，最多检查 65×65=4225 列，找到就停止，不修改方块，也不放宽原有地面、液体、危险方块和净空检查。

新增 8654 项断言覆盖四种坐标奇偶组合、负坐标、区块边界、每列仅检查一次、完整搜索范围、近处优先、立即停止及无安全点时拒绝传送。先用原两格步进运行，已复现 `Landing one block away must not be skipped by parity` 失败；改为逐格后，两版强制重新预处理、构建和全部断言通过。此补测验证搜索算法及双版本编译/打包，不冒充玩家实际跨维度传送或客户端视觉验收。地形算法与参数未变。

## 服务端回归

使用独立的 `run/forge-server`、`run/neoforge-server` 和全新 `verification-world-vortex-v3-guarded` 存档，seed=42。旧世界保留，未迁移或删除。

Forge 已通过：24 列基础/保护检查、24 列实际砂岩护岸；地形模型指纹 `24b02d9c2fe2e58a`，群系数 29。实际扫描得到木质方块 252、树叶 1328、花卉 7、菌类生态方块 1447、水生植被 249、矿物 2961。13 处群系样点通过原生 `NaturalSpawner` 产生陆生动物、夜间敌对生物、水生生物及下界生物；4 个流体探针持续 tick 后通过 60 项非源流动状态检查。

NeoForge 已通过同样的 24 列基础/保护检查和 24 列砂岩护岸检查，地形模型指纹和 29 个可用群系与 Forge 一致。实际扫描得到木质方块 280、树叶 1004、花卉 4、菌类生态方块 1497、水生植被 249、矿物 2970；13 处群系样点的陆生、水生、下界及夜间敌对生成检查通过，地物异常列表为空。原生生态数量因 Minecraft 版本和装饰而不同，不要求逐块相同。

两版均通过 4 处水/熔岩探针、60 项非源流动状态检查，测试后撤销全部强加载票并保存关闭。一次 NeoForge 回归定位并修复了群系平滑查询越过生成依赖边界的问题，修复后的日志没有该异常。

2026-09-23 最终补测：将 quart 群系读取修复在 Forge 上重新验收，24 列地形、24 列护岸和 13 处生态样点全部通过。额外在 `(5000,0)`、`(0,5000)`、`(-5000,0)`、`(0,-5000)` 附近生成此前不存在的森林、海洋、沙漠、绯红森林区块；报告时已完成 319 个装饰区块、15005 次原生地物尝试，`failedFeatureIds=[]`。本轮服务器日志无 ERROR/Exception，强加载已撤销并正常保存关闭；日志保存在 `build/reports/vortex-v3/forge-quart-fix-server.log`。

生态测试使用临时观察者满足原生玩家距离条件，调用实际原生生成流程，并经过群系、光照、地面/液体、碰撞、初始化和加入世界检查；不是 `/summon`。测试只清理自身生成的临时实体、移除观察者并恢复时间。该测试验证生成放置路径，不替代在线玩家下的长期刷怪上限/密度/性能验收。客户端视觉验收仍需实际进入新世界。

运行中可执行：

```powershell
.\scripts\Test-VortexRuntime.ps1 -Loader forge -Phase Geometry
.\scripts\Test-VortexRuntime.ps1 -Loader forge -Phase Ecology
.\scripts\Test-VortexRuntime.ps1 -Loader forge -Phase Prime
# 至少等待 20 秒后：
.\scripts\Test-VortexRuntime.ps1 -Loader forge -Phase Fluids
```

NeoForge 将 `-Loader` 改为 `neoforge`。证据保存在 `build/reports/vortex-v3/`。`Ecology` 和流体修改仅允许指定的无人测试世界；流体测试如果失败会用 barrier 标记泄漏，不能用于正式存档。报告读取支持 RCON 多包响应。

恢复被中断的测试时，发现本机一项 Gradle Kotlin 脚本缓存及其压缩缓存损坏，另有自动生成的 `.vscode/launch.json`、测试服 `config/fml.toml` 为全零内容。已隔离到 `build/cache-quarantine-20260923/` 并重新生成，未删除源码或存档；最终构建用 `--no-build-cache` 避免复用损坏条目。

## 试玩

最终双版本构建通过；已检查 JAR 内三项保护 Mixin、Forge manifest/refmap 及两版光照 JSON：

| 产物 | 大小 | SHA-256 |
| --- | ---: | --- |
| `deep_realm_the_forth-forge-0.1.0+1.20.1.jar` | 92176 B | `c64a8fb9a3d1acd9e251e0be987fae95cee22e91cc584c03c1332115d4faa13d` |
| `deep_realm_the_forth-neoforge-0.1.0+1.21.1.jar` | 89549 B | `223f65a4cc23a0106a9c602766c24fce3d63d7ca2be35d81b3de47078f566637` |

上表为补齐落点搜索后的产物，已确认两版 JAR 均包含 `LandingSearch` 及其 `Probe` 接口。

使用对应构建 JAR 新建世界，再运行 `/fourthlayer enter`。想看海岸可尝试 `/fourthlayer enter 7 1024`；若目标附近都是海水，换一个半径。旧区块不会重新生成，此次算法变化会在旧存档产生明显接缝，因此不建议直接拿旧世界评价新布局。

2026-09-23 已启动 Forge 1.20.1 客户端：模组加载、OpenGL、声音引擎和纹理图集初始化完成，未见启动崩溃；开发身份的 Realms 登录提示不属于本模组异常。按 MC-Dev-Skills 的交互验收约定保留客户端运行，由试玩者新建开启作弊的创造世界后执行上述命令。该记录仅确认启动，不代表已完成游戏内视觉或长期刷怪验收。现有 `3323` 存档未改动。
