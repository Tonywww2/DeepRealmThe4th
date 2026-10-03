/** Editor supplement for the startup/server bindings. ProbeJS remains the source of exact Java overloads. */
declare namespace AstralGemAPI {
  type Score = 'STRENGTH' | 'AGILITY' | 'INTELLIGENCE' | 'CONSTITUTION' | 'PERCEPTION' | 'MAGIC'
  type Primary = 'alpha' | 'beta' | 'gamma' | 'delta'
  type EffectScope = 'CONTAINER' | 'PLAYER'

  interface NumberValue {
    add(other: NumberValue): NumberValue
    subtract(other: NumberValue): NumberValue
    multiply(other: NumberValue): NumberValue
    divide(other: NumberValue): NumberValue
    pow(exponent: number): NumberValue
    ln(): number
    log1p(): number
    serialize(): string
    toScientificString(): string
  }

  interface CellView {
    index(): number
    x(): number
    y(): number
    inBounds(): boolean
    open(): boolean
    occupied(): boolean
    effective(): boolean
    inactiveReason(): string
    actualItemId(): string
    kindKey(): string
    variantKey(): string
    ruleKey(): string
    role(): string
    itemCopy(): unknown
    instanceData(): unknown
  }

  interface FillerResult {
    cellRef(): number
    baseTotal(): NumberValue
    finalTotal(): NumberValue
    gainTotal(): NumberValue
    gainRatio(): NumberValue
    computed(): boolean
    state(): string
    diagnostics(): string[]
  }

  interface PlayerStateSnapshot {
    health(): number
    maxHealth(): number
    healthRatio(): number
    experienceLevel(): number
    food(): number
    dimension(): string
    attribute(id: string): number
    hasUnlockTag(tag: string): boolean
  }

  interface FillerContext {
    filler(): unknown
    container(): unknown
    index(): number
    state(): PlayerStateSnapshot
    cellAt(x: number, y: number): CellView
    offset(dx: number, dy: number): CellView
    orthogonal(): CellView[]
    diagonal(): CellView[]
    mirrorCell(): CellView
    baseAt(cell: CellView): FillerResult
    resultAt(cell: CellView): FillerResult
  }
}
