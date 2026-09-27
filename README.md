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
- 星界躯体是 Curios 专属槽中的容器。六种分数、二十九种晶石和三枚勋章已接入；晶石只从深境四层指定结构宝箱获取，勋章可合成。Java 与 KubeJS 可以扩展容器和填充物。

## 进入与检查

安装对应版本的普通 JAR 后，在新测试世界使用权限等级 2 的命令：

```mcfunction
/fourthlayer enter
/fourthlayer enter 1 512
/fourthlayer enter 3 768
/fourthlayer enter 5 1024
/fourthlayer enter 7 1024
/fourthlayer return
/fourthlayer inspect
```

`enter [股号] [半径]` 默认使用第 5 股、半径 512；只接受奇数陆地股，半径范围为 128～8192。没有安全落点时命令会拒绝传送。维度 ID 是 `deeprealm_4th:fourth_layer`。

## 构建与验证

Gradle daemon 使用 Java 25；目标模组分别以 Java 17 和 21 编译。首次构建需要联网获取依赖：

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat :1.20.1-forge:build :1.21.1-neoforge:build --console=plain
```

构建产物在 `versions/<节点>/build/libs/`。`build` 会运行当前地形与星界核心验证；离线完整水系验证需要先在当前测试服执行 `/fourthlayer verifyclimate` 导出注册表气候，再运行相应节点的 `verifyHydrology`。实际区块、群系与河水可用 `scripts/Test-HydrologyRuntime.ps1` 及游戏内 `verify` 系列命令检查。

```powershell
.\gradlew.bat :1.20.1-forge:runClient
.\gradlew.bat :1.21.1-neoforge:runClient
.\gradlew.bat :1.20.1-forge:runServer --args=nogui
.\gradlew.bat :1.21.1-neoforge:runServer
```

共享代码与资源在 `src/main/`。默认维度参数位于 `src/main/resources/data/deeprealm_4th/dimension/fourth_layer.json`；修改世界生成参数后请用新世界验证。

## 文档

- [海洋与干旱山地当前实现](docs/海洋与干旱山地-V5计划与验证.md)、[黏土山材质验证](docs/干旱区粘土山-材质接入与验证.md)
- [星界躯体开发者指南](docs/星界躯体-开发者指南.md)、[Java / KubeJS 快速示例](docs/星界饰品-开发者快速指南.md)
- [星界设定与视觉基准](docs/星界饰品-设定与视觉基准.md)、[晶石获取设计](docs/星界晶石-深境四层获取与设定设计稿.md)
- [群系兼容数据包示例](examples/biome-compat/README.md)
