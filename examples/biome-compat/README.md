# 可选群系扩展示例

把此目录复制到测试世界的 `datapacks/`，将 `example_mod:your_desert` 替换为实际已安装模组的群系 ID，然后重启世界。在干旱带尚未生成的位置检查分布。示例使用 `required:false`，对应模组未安装也可以加载；`replace:false` 保留默认候选。

优先追加到具体子分类，文件路径为：

`data/deeprealm_4th/tags/worldgen/biome/region/<分类>.json`

| 陆地股 | 可用分类 |
| --- | --- |
| 1 | `arid/desert`、`arid/badlands`、`arid/river`、V5 的 `arid/plateau`、`arid/mountain` |
| 3 | `infernal/nether`、`infernal/basalt` |
| 5 | `temperate_tropical/plains`、`temperate_tropical/forest`、`temperate_tropical/jungle`、`temperate_tropical/mountain` |
| 7 | `oceanic/coast`、`oceanic/ocean`、`oceanic/deep`、`oceanic/island` |

也可只追加到 `arid`、`infernal`、`temperate_tropical`、`oceanic` 四个根 tag；根 tag 独有成员在对应大区约五分之一的非水体候选单元内参与选择。子分类成员使用对应地形分布，不受这条根 tag 回退规则影响。

当前实现会读取注册表中已经经过模组修改的真实群系温度、降水、地物和生物列表，并按原生阶段尝试 features、原生结构选点和生物生成；保留地物自身的 biome filter、刷怪条件和结构限制。日志中的 `failedFeatureIds` 用于报告需要专门适配的地物。

**外部模组的 noise router、surface rules、carvers 与专属维度机制不会自动移植。** 基础形状仍由第四层决定；要求某种专用表层或专用生成器的地物可能无法放置。`nativeReportedSuccesses` 也不等同于所有地物都成功，验收另扫描真实方块。

V5 内置 Biomes O' Plenty 和 Terralith 的一组可选标签映射，不强制安装它们，也不声称收录它们的所有群系。新增映射优先按生态/地形加入具体子池；`arid/plateau` 对应平顶台地，`arid/mountain` 对应干旱山脊，不能把雪山混入干旱池。

粘土山（恶地式红沙、分层陶瓦山体）可同时加入 `region/arid/mountain` 和 `surface/badlands` 群系标签。后者位于 `data/deeprealm_4th/tags/worldgen/biome/surface/badlands.json`，默认包含三种原版恶地；它只为 V5 干燥、非护岸的干旱山脊选择陶瓦材质，不改变高度、水系或群系气候。不要把所有干旱山地都加入此材质标签；风袭热带草原等仍保留砂土/岩石。自定义条目应使用 `required:false`，并保持 `replace:false`。

开发测试：`gradlew.bat :1.20.1-forge:runServer -PbiomeCompat=true` 或 `:1.21.1-neoforge:runServer` 同参数；客户端也可使用对应 `runClient`。测试运行目录独立为 `run/<loader>-compat-<run>`，普通启动不载入这些开发依赖。带模组创建的世界不要在移除模组后继续打开。详情见 `docs/海洋与干旱山地-V5计划与验证.md`。

本例支持 1.20.1 和 1.21.1；`tests/fixtures/tag-compatibility` 则是故意清空一个池的回归夹具，只用于一次性测试世界，不能作为正常兼容包安装。
