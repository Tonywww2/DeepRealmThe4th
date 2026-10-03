# 深境四层 · Deep Realm: The Forth

模组 ID：`deeprealm_4th`。项目使用 Stonecutter 共享源码，同时支持以下两个目标：

| Minecraft | 加载器 | 游戏 Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.0 | 17 |
| 1.21.1 | NeoForge 21.1.233 | 21 |

## 当前内容

- 深境四层是以中心 `(8, 8)` 为轴的螺旋维度。四条陆地股依次是干旱、熔岩、林山、海洋；偶数股为虚空。当前世界生成配置只接受 `generation_version=5`，旧存档不保证可用。
- 海洋包含岛群、大陆架、深海沟谷、沙滩与岩岸；干旱区包含平顶台地、山脊和恶地式黏土山材质。主干支流水系、内流湖、河岸及雪线由当前地形和群系气候共同决定。
- 原生地物、结构和生物生成使用群系规则，并保护虚空、河道与海洋护岸。可选的 Biomes O' Plenty 与 Terralith 开发配置仅用于兼容验证。
- 星界躯体是 Curios 专属槽中的容器。六种分数、三十九种晶石／晶核和三枚勋章已接入；初阶晶石只从深境四层指定结构宝箱获取，进阶填充物由星浆、星座投影、碎片和双工艺制作。Java 与 KubeJS 可以扩展容器、填充物与配方。

## 进入与检查

维度 ID 是 `deeprealm_4th:fourth_layer`。管理员可用原版 `/execute in` 和 `/tp` 在已确认安全的坐标间传送；世界生成检查使用原版命令。

生存流程可用紫菘果、龙息、下界之星和紫水晶碎片合成拟态星浆。手持它落入允许维度的虚空，即可进入深境四层。来源维度由服务端配置 `travel.void_entry_dimensions` 控制，默认 `["minecraft:the_end"]`，空列表禁用此入口；Forge 1.20.1 的文件在世界 `serverconfig/deeprealm_4th-server.toml`，NeoForge 1.21.1 的文件在游戏目录 `config/deeprealm_4th-server.toml`。

深境四层的虚空中会生成孤立星浆晶洞（Y=-32..128），内壁有 2–4 个星之源。母岩会长出四阶段星浆晶体；打碎成熟晶体获得星浆晶体材料，使用玻璃瓶右击则获得一瓶星浆。采集后晶体消失，母岩可重新生长。

## 构建与验证

Gradle daemon 使用 Java 25；目标模组分别以 Java 17 和 21 编译。首次构建需要联网获取依赖：

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat :1.20.1-forge:build :1.21.1-neoforge:build --console=plain
```

构建产物在 `versions/<节点>/build/libs/`。`build` 会运行当前地形与星界核心验证；实际区块、群系与河水可在新测试世界中用原版命令检查。

```powershell
.\gradlew.bat :1.20.1-forge:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
.\gradlew.bat :1.20.1-forge:runServer --args=nogui
.\gradlew.bat :1.21.1-neoforge:runServer
```

共享代码与资源在 `src/main/`。默认维度参数位于 `src/main/resources/data/deeprealm_4th/dimension/fourth_layer.json`；修改世界生成参数后请用新世界验证。

## 文档

- [代码结构与跨版本适配](docs/代码结构与跨版本适配.md)
- [海洋与干旱山地当前实现](docs/海洋与干旱山地-V5计划与验证.md)、[黏土山材质验证](docs/干旱区粘土山-材质接入与验证.md)
- [星界躯体开发者指南](docs/星界躯体-开发者指南.md)、[Java / KubeJS 快速示例](docs/星界饰品-开发者快速指南.md)
- [星界设定与视觉基准](docs/星界饰品-设定与视觉基准.md)、[晶石获取设计](docs/星界晶石-深境四层获取与设定设计稿.md)
- [星浆、星座投影与双工艺](docs/星界进阶材料与双工艺-设计稿.md)
- [群系兼容数据包示例](examples/biome-compat/README.md)
