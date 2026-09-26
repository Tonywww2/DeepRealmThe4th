# 深境四层 · Deep Realm: The Forth

一个以 `(0, 0)` 区块为中心、向内顺时针下沉的螺旋维度原型。Stonecutter 共用源码，目标版本：

模组 ID 为 `deeprealm_4th`。此前开发版本使用 `deep_realm_the_forth`；更名会改变维度、物品、资源及配置的命名空间。已有测试存档请先备份，建议用新世界验证本版本；旧世界数据不会自动整体迁移。`/fourthlayer return` 会兼容读取旧版保存的返回位置。

| Minecraft | 加载器 | 游戏 Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.0 | 17 |
| 1.21.1 | NeoForge 21.1.233 | 21 |

## 当前内容

- `0.1.8`（源码已实现，发行验收暂缓）：新世界使用 `generation_version=5`。海洋增加不同尺度的大陆/岛群、不规则大陆架、深海沟谷、宽沙滩与岩岸；干旱区增加实际平顶台地与独立山地标签。BOP + Terralith 已加入可开关的双端开发环境，并验证实际区块、原生地物与气候。对照图、版本、兼容边界及等待同期 Astral/Curios 工作完成后的发行复验事项见 [V5 工作记录](docs/海洋与干旱山地-V5计划与验证.md)。
- `0.1.7`：新世界配置使用 `generation_version=4`。群系边界增加相关细节，陆地仅在窄外缘侧蚀，山体侧翼不再完全平顺；保留中心断崖及内部连续陆地。增加有界基础地形缓存、湖盆距离查表和河段几何预计算。补上河水的原版生成后流体更新，验证不再靠手动激活水体。计划、对照图和实测结果见 [V4 工作记录](docs/边缘自然化与生成性能-V4计划.md)。
- `0.1.6`（保留）：全局主干—支流、非对称河岸、沿真实低地扩展的内流湖及有限分叉山脊已接入实际区块生成。水系供水读取加载器修改后的群系温度/降水；干热区域不会被强制铺上河水。见 [V3 接入记录](docs/全局水系接入-V3工作记录.md)。V1/V2 安装包、旧算法和实验失败记录仍保留作对照。
- 八股螺旋：1 干旱、3 下界/火山、5 温带/热带、7 海洋；2/4/6/8 为空。
- 新世界每股陆地占约 3/16、每股虚空占约 1/16；陆地/虚空总比例约 75%/25%，旋转强度由 0.72 提高到 1.44。
- 中心坐标 `(8, 8)`，中心范围按圆形直径 7 区块（112 格）、中央虚空直径 3 区块（48 格）实现，不是填满对应方形区块。半径 56 格的断崖上缘仍为 Y=60，向内降至约 Y=-52；有限厚度陆地下方没有基岩封底。
- 0.1.5：雪线改为多尺度噪声控制；河道使用曲线路径距离、圆滑源头、浅水河床和连续河谷，河岸混合沙/砾石/黏土。岛屿以世界坐标的多尺度噪声生成，扩大低缓沙滩，以斑块过渡到草地；天然岛岸的三格砂岩保护层埋入原有岸坡，不再额外抬成统一平顶圈。海洋靠虚空侧仍保留封水护岸。
- 0.1.4：群系改为世界坐标上的不规则二维斑块，各候选群系使用独立噪声竞争，去掉沿漩涡拉伸与单一噪声分段产生的条带。平原偏好低地，高山受实际高度约束；该版本基础高度、水位和陆地轮廓保持 0.1.3 不变。
- 0.1.3：将高度/河流扰动与陆地边界扰动分离，避免远处地形被角度噪声压成尖刺；山脊改为宽而圆滑的峰顶，沙漠使用较低的独立起伏幅度，保留恶地高地与山谷。
- 0.1.2：删除陆地股内部的裂隙遮罩，保留边缘的种子扰动及随机带宽；0.1.3 没有改动陆地范围、中心大小和断崖基线。
- 0.1.1：移除等距径向正弦波，多尺度梯度噪声混合世界坐标和漩涡方向，生成不规则丘陵、山脊与冲沟；中心外径向高度增幅仍为最初径向曲线的 10%。
- 连续扭曲气候场生成交错群系；河流长度、支流数量/位置、宽度与湖岸随机化，火山尺寸也随种子变化。海洋不再在固定半径切换海床。
- 海洋水面统一为 Y=64，边界有水平至少三格厚、沙子封顶且贯通海床的砂岩护岸。
- 执行群系原生 placed features（包括群系修改器添加的地物），接入正常生物生成；群系仍通过 tag 扩展。
- 0.1.2：接入原生结构集、候选位置、群系/高度判定、跨区块部件放置与保存；无结构 ID 白名单，跨虚空的候选整体拒绝，仍保护底壳和封水边界。世界需开启“生成结构”。
- 管理员进入/返回、地形诊断，以及可复现的算法预览和测试。

这是可测试的维度版本，不是全模组兼容成品。地物与结构须服从虚空、底壳和水体护岸约束；依赖特定生成器的第三方内容仍需适配。结构保持原生高度选择，不额外移植原版噪声生成器的地形塑形，低于浮空底壳等受保护部分不能写入。河流采用完整局部汇水区与长期均值供水，不模拟季节、动态降雨或真实地下水；无法安全容纳的流域整体不铺水。洞穴、正式传送门、客户端视觉验收和长期生态性能测试仍在后续计划中。

## 进入维度

安装对应版本 JAR，建议新建允许作弊的测试世界：

```mcfunction
/fourthlayer enter
/fourthlayer enter 1 512
/fourthlayer enter 3 768
/fourthlayer enter 5 1024
/fourthlayer enter 7 1024
/fourthlayer return
```

`enter [股号] [半径]` 默认前往 5 股、半径 512；只接受奇数陆地股，半径范围 128～8192。命令从近到远逐格检查目标周围 32 格的方形范围，不跳过奇数坐标；没有合适落点会拒绝传送，不会自动造平台。海洋股选定半径附近可能全是水，此时换一个半径。`return` 尝试返回入场前位置；落点受阻时使用相同的完整搜索，失败保留返回记录。

所有命令要求权限等级 2。维度 ID：`deeprealm_4th:fourth_layer`。不要直接传送到中心区块，那里是虚空。

建议新建世界查看 V5 外观；旧区块不会重绘，旧生成版本仍可读取，不承诺直接切换生成版本或改变群系标签后无接缝。0.1.7 起会自动更新新生成河水，但已保存的旧水体不会重跑生成后处理。备份存档后，站在第四层的河流附近执行 `/fourthlayer updatewater` 可安排周围已加载 3×3 区块内的河水更新；`updatewater 0` 只更新脚下区块，最大半径为 2（5×5）。不放置方块、不强制加载区块；水按原版规则变化，平坦封闭的水源河段仍可能静止。

```mcfunction
/fourthlayer inspect
/fourthlayer inspect 8 8
/fourthlayer verify
/fourthlayer ecology
```

`inspect` 查询第四层指定 X/Z 的模型；`verify` 检查 24 列基础查询、受保护方块和 24 处实际海岸墙，允许地物/结构正常改变非保护区。后者仅建议在未被玩家改造的测试世界运行。`ecology` 输出本次进程的地物及结构放置统计；返回成功不代表所有方块都获准放置，因此另有实际方块扫描验证。`verifystructures <结构名>` 仅允许开发用的无人 seed=42 测试存档，自动寻找自然结构并核对方块/战利品，不执行 `/place`。

## 构建与开发

Gradle daemon 使用 Java 25（ModStitch 0.8.5 要求），由 Foojay 自动提供；两个模组分别以 Java 17、21 编译，不要求游戏使用 Java 25。

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat :1.20.1-forge:build :1.21.1-neoforge:build --console=plain
```

产物在 `versions/<节点>/build/libs/`，安装不带 `-sources` 后缀的 JAR。首轮构建需要联网下载依赖。共享代码和资源位于 `src/main`，平台差异集中在 `platform`。

```powershell
.\gradlew.bat :1.20.1-forge:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
.\gradlew.bat :1.20.1-forge:runServer --args=nogui
.\gradlew.bat :1.21.1-neoforge:runServer
```

各运行环境使用独立的 `run/forge-*` 和 `run/neoforge-*` 目录。服务端首次运行需自行阅读并接受 Minecraft EULA。不要让多个构建进程同时操作同一 Loom 缓存。

修改 `stonecutter.gradle.kts` 的 active 节点切换编辑目标。新增 Java 文件若未被识别，运行对应节点的 `compileJava --rerun-tasks` 强制重新生成源码。

## 测试与预览

以下 `verifyTerrain` 用于保留旧算法的回归；V3 另由 `verifyGlobalHydrology` 覆盖，V4 使用 `verifyDetailedWorldgen`、`verifyEdgeDetails` 和 V10 真实世界测试，不能只看旧地形预览判断新版效果。

V5 使用双端 `verifyMarineArid`（海岸/台地/防漏/并行确定性）与 `verifyMarineHydrology`（先在 V11 对照服导出 `verifyclimate`）。真实服务器验收使用 `scripts/Test-HydrologyRuntime.ps1 -Loader forge -GenerationVersion 5 -Phase Compat`，NeoForge 替换加载器；群系模组组增加 `-Profile compat`。测试阶段还包括 `Climate`、`Geometry`、`Prepare`、`Check`、`Cleanup`，水流检查需等待至少 200 个实际游戏 tick。

开发模组开关：在 `runServer` 或 `runClient` 后加 `-PbiomeCompat=true`。固定版本的 BOP、Terralith 及必需库只进入独立 `run/<loader>-compat-<run>`；正常发行构建不要加此开关，也不会打包这些群系模组。Forge 的特定 BOP 树木开发映射修复位于 `buildSrc`，不进入发行 JAR。

```powershell
.\gradlew.bat :1.20.1-forge:verifyDetailedWorldgen :1.20.1-forge:verifyEdgeDetails
.\gradlew.bat :1.21.1-neoforge:verifyDetailedWorldgen :1.21.1-neoforge:verifyEdgeDetails
# 先完成编译，停止本项目测试服及构建，再顺序运行；需要保留的 0.1.6 JAR 和真实气候快照。
.\scripts\Measure-TerrainPerformance.ps1 -Loader forge
.\scripts\Measure-TerrainPerformance.ps1 -Loader neoforge
```

V4 样品/断言：`build/reports/hydrology-v4/<loader>/`；性能 TSV/JFR：`build/reports/terrain-performance/<loader>/`。性能脚本按旧二进制、优化后的 V3、最终 V4 顺序执行两轮独立 JVM；每轮预热至少 4 秒、测量 5 次。它测的是固定 65,536 列纯算法查询，不是完整原生区块生成、玩家探索延迟或帧率。

`build`/`check` 自动调用 `verifyTerrain`，运行独立 Java 断言（不依赖 JUnit）。覆盖三种种子、陆地股内部连续采样、7/3 区块直径、八股方向、10% 外坡、Y60 断崖上缘、上下界、水体约束、含斜角的三格护岸、全部地形类型以及并发/逆序采样。圆环网格实测陆地覆盖率约 76.05%，局部随机轮廓仍围绕名义 75% 变化。

```powershell
.\gradlew.bat :1.20.1-forge:verifyTerrain :1.21.1-neoforge:verifyTerrain
```

输出：`build/reports/terrain/` 和 `build/reports/terrain-neoforge/` 中的 `overview.png`、`relief.png`、`radial-profile.png`、`verification.txt`、`fluid-probes.json`，以及 `relief-samples.png`、`relief-verification.txt`（坡度/尖峰回归）、`biome-map.png`、`biome-verification.txt`（群系 ID 分布、远近尺度）、`shore-samples.png`、`shore-verification.txt`、`beach-patch.txt`（雪线、岸坡、大片沙滩及中心/陆地范围不变性）。图片来自实际算法采样，不是游戏截图。

当前调整与回归方法见 [0.1.5 雪线、河岸与海滩](docs/雪线河岸与海滩-实现与验证.md)。历史版本见 [0.1.4 自然斑块群系](docs/自然斑块群系-实现与验证.md)、[0.1.3 山峰与沙漠突起修正](docs/山峰与沙漠突起修正-实现与验证.md)、[0.1.2 连续陆地与结构生成](docs/连续陆地与结构生成-实现与验证.md)、[0.1.1 自然地形与 Y60 断崖](docs/自然地形与Y60断崖-实现与验证.md)、[B 方案实现与验证](docs/漩涡B方案-实现与验证.md)、[实现与验证记录](docs/实现与验证记录.md)；原始设计见 [制作计划](docs/深境四层-制作计划.md)。

离线水文实验单独运行，必须先在已配置的无人 seed=42 `verification-world-vortex-v8-shores` 测试服执行 `/fourthlayer verifyclimate`，导出加载器修改后的实际气候。命令不修改群系或方块。随后关闭测试服再运行：

```powershell
.\gradlew.bat :1.20.1-forge:verifyHydrologyPrototype :1.21.1-neoforge:verifyHydrologyPrototype --console=plain
```

输出 `build/reports/hydrology-prototype/{forge,neoforge}/`。任务通过仅表示有限域局部不变量通过，**不表示无限世界接缝、湖泊平衡或方块水已达标**；必须同时查看 `verification.txt` 的 `INTEGRATION_GATE`。该实验任务没有挂到普通 `check`，以免正常构建依赖本机的注册表导出文件。

V2 自然形态实验使用同一气候快照，独立保留 V1 对照：

```powershell
.\gradlew.bat :1.20.1-forge:verifyNaturalHydrology :1.21.1-neoforge:verifyNaturalHydrology --console=plain
```

输出 `build/reports/hydrology-v2/{forge,neoforge}/`：同坐标地形对照、完整河网、河岸形态受控对照及 `verification.txt`。岸线图只表达二维轮廓提案，不是已开挖的河床或可直接放置的方块水。V2 的 `INTEGRATION_GATE` 仍未通过。

## V3 保留路径与世界接入验证

V3 保形回归测试（需要先在对应测试服导出气候）：

```powershell
.\gradlew.bat :1.20.1-forge:verifyGlobalHydrology :1.21.1-neoforge:verifyGlobalHydrology --console=plain
```

输出 `build/reports/hydrology-v3/{forge,neoforge}/`，包含最终地形列样品、完整河网连通性、跨流域相交、多种子并发及缓存重建测试。独立运行的理由与 V1/V2 相同：不让普通构建依赖本机气候导出文件。真实世界的 `prepare/check/cleanup` 流水验证、重启流程与结果见 V3 记录；算法图不是游戏截图。

0.1.6 默认版本为 3、0.1.7 为 4、当前 0.1.8 为 5。缺少版本字段时仍按版本 1 解码。数据包重新读取和旧区块缓存可能造成新旧交界，**请新建世界体验新版，不要在重要旧存档中直接切换算法**。流水验证脚本为保护历史用例仍默认 V10/版本 4；测试当前 V11 必须显式加 `-GenerationVersion 5`，历史 V9 使用 `-GenerationVersion 3`。

## 数据包配置

默认参数位于 `data/deeprealm_4th/dimension/fourth_layer.json` 的 `generator.biome_source.parameters`，支持中心、核心半径、急降半径、螺旋松紧、旋转和径向尺度。世界高度固定为 `-64..447`；不能只改 dimension_type 而不改生成器高度。

当前 `land_fraction=0.75`、`twist=1.44`、`core_radius=24`、`plunge_radius=56`。宽度扰动和中心空洞会改变局部面积。游戏可能重新读取维度配置，而旧区块不会更新；**查看这次新布局和结构请新建开启结构生成的世界**。之前单独调整比例/旋转的历史记录见 [地形比例与旋转调整](docs/地形比例与旋转调整.md)。

群系扩展示例见 [examples/biome-compat](examples/biome-compat/README.md)。修改参数、算法或群系池后，应重启并使用新世界检查；旧区块不会重生成，旧世界可能保留存档中的生成器参数，新旧地块也可能出现接缝。不承诺 `/reload` 更新已解析的群系池。
