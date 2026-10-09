# 深境四层 · Deep Realm: The Fourth

模组 ID：`deeprealm_4th`。项目使用 Stonecutter 共享源码，同时支持两个目标：

| Minecraft | 加载器 | 游戏 Java |
| --- | --- | --- |
| 1.20.1 | Forge 47.4.0 | 17 |
| 1.21.1 | NeoForge 21.1.233 | 21 |

Curios 为必需依赖；JEI 与 KubeJS 为可选集成，开发运行配置已包含相应依赖。

## 当前内容

- 螺旋维度包含干旱、熔岩、林山、海洋四条陆地股，交替分布虚空股。水系、湖泊、海岸及群系气候由当前生成模型共同处理。
- 星界躯体是 Curios 专属槽中的容器，支持可配置网格、六种分数、39 种晶石／晶核和三枚职业勋章。
- 初阶晶石只在深境四层指定结构宝箱追加；虚空晶洞中的星之源生长星浆晶体，可打碎取晶体材料，或使用玻璃瓶采集星浆。
- 星座投影、碎片及进阶填充物通过工作台、投影框架组合与组合锻造制作；两类工艺支持完整物品栈数据和 JEI 展示。
- Java / KubeJS 可扩展容器、固定规则、逐格节点、大数分数、动态物品数据及加工配方。

## 进入深境四层

维度 ID 为 `deeprealm_4th:fourth_layer`。紫菘果、龙息、下界之星和紫水晶碎片可合成拟态星浆；手持它落入允许维度的虚空即可进入。

服务端配置 `travel.void_entry_dimensions` 默认是 `["minecraft:the_end"]`，空列表禁用入口。Forge 配置位于世界的 `serverconfig/deeprealm_4th-server.toml`；NeoForge 配置位于游戏目录的 `config/deeprealm_4th-server.toml`。管理员检查使用原版命令。

## 构建与启动

Gradle daemon 使用 Java 25，目标代码分别以 Java 17、21 编译。首次构建需要联网获取依赖。

仅打包两个目标：

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat :1.20.1-forge:remapJar :1.21.1-neoforge:jar --console=plain
```

产物位于 `versions/<节点>/build/libs/`。需要完整构建与内置验证时运行 `:1.20.1-forge:build :1.21.1-neoforge:build`；验证范围遵循 [AGENTS.md](AGENTS.md)。

```powershell
.\gradlew.bat :1.20.1-forge:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
.\gradlew.bat :1.20.1-forge:runServer --args=nogui
.\gradlew.bat :1.21.1-neoforge:runServer
```

共享代码与资源位于 `src/main/`。世界生成默认参数见 [fourth_layer.json](src/main/resources/data/deeprealm_4th/dimension/fourth_layer.json)；当前只接受 `generation_version=5`，改动生成参数后使用新世界或新区块检查。

## 文档与示例

完整入口见 **[项目文档](docs/README.md)**。

- 玩法：[世界生成](docs/深境四层-世界生成.md)、[星界躯体与填充物](docs/星界躯体与填充物.md)、[材料与工艺](docs/星界材料与工艺.md)。
- 开发：[快速指南](docs/星界饰品-开发者快速指南.md)、[容器与配方](docs/星界躯体-开发者指南.md)、[宝石节点 API](docs/星界宝石系统-API扩展开发者指南.md)、[代码结构](docs/代码结构与跨版本适配.md)。
- 资源：[文案指南](docs/饰品系统文案指南.md)、[美术规范](docs/星界资源与美术规范.md)。
- 示例：[KubeJS](examples/astral-kubejs/README.md)、[群系数据包](examples/biome-compat/README.md)。
