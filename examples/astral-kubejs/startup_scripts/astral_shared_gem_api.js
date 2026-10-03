// Copy to kubejs/startup_scripts. This example uses one physical item ID for two data rules.
const GEM_ITEM = 'kubejs:shared_astral_gem'
const GEM_KEY = 'demo.gem'
const GemStat = Java.loadClass('com.tonywww.deeprealm4th.astral.data.GemPayload$Stat')
const TextComponent = Java.loadClass('net.minecraft.network.chat.Component')

StartupEvents.registry('item', event => event.create('shared_astral_gem'))

FillerPresentations.registerName(GEM_ITEM, stack => {
  const parsed = GemPayload.decode(AstralItemData.read(stack, GEM_KEY))
  if (!parsed.ok()) return TextComponent.literal('未校准的星界宝石')
  return TextComponent.literal(parsed.value().sourceItem() + ' ' + parsed.value().displayPrimary())
})

AstralFillers.register(GEM_ITEM, FillerDefinition.builder()
  .activation(FillerActivation.STACKABLE)
  .node(instance => {
    const parsed = GemPayload.decode(AstralItemData.read(instance.filler(), GEM_KEY))
    if (!parsed.ok()) return FillerNode.invalid(parsed.error())
    const gem = parsed.value()
    const identity = gem.identity()
    const amethyst = identity === 'minecraft:amethyst_shard#alpha'
    if (!amethyst && identity !== 'minecraft:quartz#beta') {
      return FillerNode.invalid('unknown_demo_gem_primary')
    }
    return FillerNode.builder()
      .kindKey(gem.sourceItem())
      .variantKey(gem.primary())
      .ruleKey(amethyst ? 'kubejs:amethyst_alpha' : 'kubejs:quartz_beta')
      .role(amethyst ? 'receiver' : 'producer')
      .data(gem.encode())
      .intrinsic((ctx, data, out) => {
        const own = GemPayload.decode(data).value()
        if (amethyst) {
          out.add(ScoreType.STRENGTH,
            AstralNumber.fromDouble(own.effective(GemStat.SIZE) / 100))
        } else {
          out.add(ScoreType.MAGIC,
            AstralNumber.fromDouble(own.effective(GemStat.PURITY) / 100))
        }
      })
      .reads((ctx, data) => ReadPlan.builder()
        .baseScores('adjacent_quartz', ctx.orthogonal(),
          FillerFilter.kind('minecraft:quartz'))
        .build())
      .calculate((ctx, data, inputs, out) => {
        if (amethyst) {
          out.add(ScoreType.STRENGTH,
            inputs.sumScores('adjacent_quartz').get(ScoreType.MAGIC)
              .multiply(AstralNumber.parse('0.25')))
        }
      })
      .build()
  })
  .build())
