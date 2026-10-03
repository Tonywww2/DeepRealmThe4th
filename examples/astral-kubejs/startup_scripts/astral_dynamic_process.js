// Copy beside astral_shared_gem_api.js. The recipe's named "gem" input is a real source item.
const DEMO_GEM_KEY = 'demo.gem'

AstralDataPredicates.register('kubejs:demo_gem', stack => {
  return GemPayload.decode(AstralItemData.read(stack, DEMO_GEM_KEY)).ok()
})

AstralTransforms.register('kubejs:gem_to_filler',
  ctx => GemPayload.decode(AstralItemData.read(ctx.source('gem'), DEMO_GEM_KEY)).ok(),
  ctx => {
    const copied = GemPayloadTransfer.toFiller(
      ctx.source('gem'), DEMO_GEM_KEY, ctx.template(), DEMO_GEM_KEY)
    return copied.ok() ? AstralTransforms.previewItem(copied.stack())
      : AstralTransforms.failedPreview(copied.error())
  },
  ctx => {
    const copied = GemPayloadTransfer.toFiller(
      ctx.source('gem'), DEMO_GEM_KEY, ctx.template(), DEMO_GEM_KEY)
    return copied.ok() ? AstralTransforms.producedItem(copied.stack())
      : AstralTransforms.failedProduce(copied.error())
  })
