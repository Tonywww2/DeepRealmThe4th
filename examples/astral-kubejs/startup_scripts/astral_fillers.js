// Copy this file to kubejs/startup_scripts/ on either supported game version.
StartupEvents.registry('item', event => {
  event.create('kubejs:astral_ruby')
  event.create('kubejs:astral_sapphire')
  event.create('kubejs:astral_opal')
  event.create('kubejs:astral_amber')
  event.create('kubejs:warrior_medal_example')
})

// A fixed score. Scores alone do not change Minecraft attributes.
AstralFillers.register('kubejs:astral_ruby',
  FillerDefinition.builder()
    .activation(FillerActivation.STACKABLE)
    .base(ScoreType.STRENGTH, 3)
    .build())

// Bonuses tied to health, another filler anywhere, and a directly adjacent ruby.
AstralFillers.register('kubejs:astral_sapphire',
  FillerDefinition.builder()
    .suppressedBy('kubejs:astral_opal')
    .base(ScoreType.MAGIC, 2)
    .bonus(AstralRules.whenHealthBelow(10, ScoreType.CONSTITUTION, 2))
    .bonus(AstralRules.whenFillerCount('kubejs:astral_ruby', 1, ScoreType.MAGIC, 1))
    .bonus(AstralRules.whenAdjacent('kubejs:astral_ruby', ScoreType.INTELLIGENCE, 2))
    .build())

// Add half of the current strength score to agility in the bonus phase.
AstralFillers.register('kubejs:astral_opal',
  FillerDefinition.builder()
    .bonus(AstralRules.perScore(ScoreType.STRENGTH, ScoreType.AGILITY, 0.5))
    .build())

// Example conversion policy: move one quarter of post-bonus magic into perception.
// This is an opt-in rule, not the system's default conversion semantics.
AstralFillers.register('kubejs:astral_amber',
  FillerDefinition.builder()
    .conversion(AstralRules.convert(ScoreType.MAGIC, ScoreType.PERCEPTION, 0.25))
    .build())

// A medal: treat negative Strength as zero, then ln(1 + Strength / 15).
AstralFillers.register('kubejs:warrior_medal_example',
  FillerDefinition.builder()
    .activation(FillerActivation.UNIQUE_EFFECT)
    .medal(AstralRules.warriorMedal())
    .build())
