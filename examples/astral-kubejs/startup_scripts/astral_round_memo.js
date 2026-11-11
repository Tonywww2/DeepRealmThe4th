// Optional read-only adapter example for the round memo API.
// Requires the updated mod. Use distinct namespaced keys for each adapter/version.
StartupEvents.registry('item', event => {
  event.create('astral_memo_example').maxStackSize(1)
})

AstralFillers.register('kubejs:astral_memo_example', FillerDefinition.builder()
  .readOnlySnapshot()
  .node(ctx => FillerNode.builder().kindKey('demo:memo').role('producer')
    .intrinsic((ctx, data, out) => {
      let state = ctx.roundMemoGet('demo:v1/player')
      if (state === null || state === undefined) {
        state = Object.freeze({ healthRatio: ctx.state().healthRatio() })
        ctx.roundMemoPut('demo:v1/player', state)
      }
      out.add(ScoreType.CONSTITUTION, AstralNumber.fromDouble(state.healthRatio))
    })
    .calculate((ctx, data, inputs, out) => {
      let cells = ctx.memoGet('demo:v1/cells')
      if (cells === null || cells === undefined) {
        let list = new (Java.loadClass('java.util.ArrayList'))(ctx.cells())
        cells = []
        for (let i = 0; i < list.size(); i++) {
          let cell = list.get(i)
          cells.push(Object.freeze({ index: cell.index(), effective: cell.effective() }))
        }
        Object.freeze(cells)
        ctx.memoPut('demo:v1/cells', cells)
      }
      out.add(ScoreType.CONSTITUTION, AstralNumber.fromDouble(cells.filter(c => c.effective).length))
    }).build())
  .build())
