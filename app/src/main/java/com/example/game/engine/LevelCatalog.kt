package com.example.game.engine

object LevelCatalog {
  val levels: List<LevelDefinition> = listOf(
    buildLevel1(),
    buildLevel2(),
    buildLevel3(),
    buildLevel4(),
    buildLevel5(),
  )

  fun getLevel(id: Int): LevelDefinition = levels.firstOrNull { it.id == id } ?: levels.first()

  private fun buildLevel1(): LevelDefinition {
    // Sunlit Bamboo Scaffolding - Classic slanted girder climb with generous jumps & hammer
    val palette = BiomePalette(
      skyTop = Vec3Color(0.09f, 0.22f, 0.34f),
      skyBottom = Vec3Color(0.95f, 0.56f, 0.28f),
      girderMain = Vec3Color(0.86f, 0.25f, 0.27f),
      girderTrim = Vec3Color(1.0f, 0.80f, 0.25f),
      pillarColor = Vec3Color(0.18f, 0.55f, 0.32f),
      fogColor = Vec3Color(0.45f, 0.32f, 0.30f),
      accentGlow = Vec3Color(1.0f, 0.84f, 0.30f),
    )
    return LevelDefinition(
      id = 1,
      title = "Sunlit Scaffolding",
      subtitle = "Climb the bamboo girders & dodge rolling barrels",
      mechanicsTag = "Classic Slopes • Hammer • Shield",
      biome = BiomeStyle.SUNLIT_BAMBOO,
      palette = palette,
      startX = -5.6f,
      startY = 0f,
      bossX = -5.0f,
      bossY = 14.5f,
      goalX = 2.8f,
      goalY = 17.2f,
      barrelSpawnInterval = 2.65f,
      targetTwoStarScore = 1800,
      targetThreeStarScore = 3200,
      parTimeSeconds = 50,
      platforms = listOf(
        // Tier 0 (ground) slopes gently up to the right
        PlatformDef(1, -6.8f, 6.8f, 0.0f, 0.6f),
        // Tier 1 slopes up to the left
        PlatformDef(2, -6.8f, 6.0f, 3.3f, 2.5f),
        // Tier 2 slopes up to the right
        PlatformDef(3, -6.0f, 6.8f, 5.4f, 6.2f),
        // Tier 3 slopes up to the left
        PlatformDef(4, -6.8f, 6.0f, 9.0f, 8.2f),
        // Tier 4 slopes up to the right
        PlatformDef(5, -6.0f, 6.8f, 11.2f, 12.0f),
        // Tier 5 (Boss Deck)
        PlatformDef(6, -6.8f, 4.6f, 14.5f, 14.3f, isBossDeck = true),
        // Tier 6 (Goal Shrine)
        PlatformDef(7, -0.6f, 4.8f, 17.2f, 17.2f),
      ),
      ladders = listOf(
        LadderDef(1, 5.0f, 0.52f, 2.59f),
        LadderDef(2, -1.2f, 2.95f, 5.71f, isBroken = true),
        LadderDef(3, -5.0f, 3.19f, 5.46f),
        LadderDef(4, 4.8f, 6.08f, 8.30f),
        LadderDef(5, 0.4f, 8.55f, 11.60f),
        LadderDef(6, -5.0f, 8.89f, 11.26f),
        LadderDef(7, 3.6f, 11.80f, 14.32f),
        LadderDef(8, 0.4f, 14.38f, 17.20f),
      ),
      collectibles = listOf(
        CollectibleDef(1, 0.0f, 3.7f),
        CollectibleDef(2, 5.6f, 6.9f),
        CollectibleDef(3, -3.0f, 12.2f),
      ),
      powerUps = listOf(
        PowerUpSpawnDef(1, -2.5f, 3.7f, PowerUpType.HAMMER),
        PowerUpSpawnDef(2, 2.5f, 6.5f, PowerUpType.SHIELD),
        PowerUpSpawnDef(3, -1.5f, 9.4f, PowerUpType.HAMMER),
      ),
    )
  }

  private fun buildLevel2(): LevelDefinition {
    // Jade Conveyor Foundry - Conveyors, Spring Pads, and Fast Blue Barrels
    val palette = BiomePalette(
      skyTop = Vec3Color(0.04f, 0.14f, 0.16f),
      skyBottom = Vec3Color(0.11f, 0.48f, 0.36f),
      girderMain = Vec3Color(0.14f, 0.65f, 0.52f),
      girderTrim = Vec3Color(0.45f, 1.0f, 0.72f),
      pillarColor = Vec3Color(0.16f, 0.25f, 0.32f),
      fogColor = Vec3Color(0.08f, 0.28f, 0.24f),
      accentGlow = Vec3Color(0.25f, 1.0f, 0.68f),
    )
    return LevelDefinition(
      id = 2,
      title = "Jade Foundry",
      subtitle = "Ride conveyor beams & launch from bamboo springs",
      mechanicsTag = "Conveyors • Springs • Double Jump",
      biome = BiomeStyle.JADE_FOUNDRY,
      palette = palette,
      startX = -5.5f,
      startY = 0f,
      bossX = 4.6f,
      bossY = 15.0f,
      goalX = -3.4f,
      goalY = 17.8f,
      barrelSpawnInterval = 2.35f,
      targetTwoStarScore = 2400,
      targetThreeStarScore = 4000,
      parTimeSeconds = 55,
      platforms = listOf(
        PlatformDef(1, -6.8f, 6.8f, 0.0f, 0.0f),
        // Tier 1 split with conveyor pushing left
        PlatformDef(2, -6.8f, -0.8f, 2.8f, 2.8f, conveyorSpeed = -1.6f),
        PlatformDef(3, 1.2f, 6.8f, 2.8f, 3.2f),
        // Tier 2 slopes left-to-right
        PlatformDef(4, -5.8f, 6.8f, 6.2f, 5.6f),
        // Tier 3 split with right-pushing conveyor
        PlatformDef(5, -6.8f, -0.5f, 9.0f, 8.6f),
        PlatformDef(6, 1.5f, 6.8f, 8.8f, 8.8f, conveyorSpeed = 1.7f),
        // Tier 4
        PlatformDef(7, -6.8f, 5.8f, 12.1f, 11.7f),
        // Tier 5 Boss Deck
        PlatformDef(8, -3.5f, 6.8f, 15.0f, 15.0f, isBossDeck = true),
        // Tier 6 Goal
        PlatformDef(9, -5.6f, -1.2f, 17.8f, 17.8f),
      ),
      ladders = listOf(
        LadderDef(1, -5.2f, 0.0f, 2.8f),
        LadderDef(2, 5.4f, 0.0f, 3.1f),
        LadderDef(3, 3.0f, 2.98f, 5.77f),
        LadderDef(4, -4.6f, 6.14f, 8.88f),
        LadderDef(5, 5.2f, 8.8f, 11.72f),
        LadderDef(6, -1.8f, 11.94f, 15.0f),
        LadderDef(7, -2.0f, 15.0f, 17.8f),
      ),
      springs = listOf(
        SpringDef(1, 5.6f, 3.1f, 18.0f),
        SpringDef(2, -5.5f, 8.9f, 18.4f),
      ),
      collectibles = listOf(
        CollectibleDef(1, 0.2f, 4.2f),
        CollectibleDef(2, 0.5f, 9.8f),
        CollectibleDef(3, -5.2f, 13.3f),
      ),
      powerUps = listOf(
        PowerUpSpawnDef(1, 2.8f, 3.5f, PowerUpType.DOUBLE_JUMP),
        PowerUpSpawnDef(2, -1.5f, 6.6f, PowerUpType.HAMMER),
        PowerUpSpawnDef(3, 2.5f, 12.5f, PowerUpType.SHIELD),
      ),
      initialFlames = listOf(0.0f to 6.3f),
    )
  }

  private fun buildLevel3(): LevelDefinition {
    // Crimson Lantern Pagoda - Moving elevators, flame spirits & speed chili
    val palette = BiomePalette(
      skyTop = Vec3Color(0.11f, 0.05f, 0.16f),
      skyBottom = Vec3Color(0.58f, 0.14f, 0.24f),
      girderMain = Vec3Color(0.84f, 0.18f, 0.30f),
      girderTrim = Vec3Color(1.0f, 0.72f, 0.22f),
      pillarColor = Vec3Color(0.24f, 0.12f, 0.20f),
      fogColor = Vec3Color(0.32f, 0.10f, 0.18f),
      accentGlow = Vec3Color(1.0f, 0.42f, 0.35f),
    )
    return LevelDefinition(
      id = 3,
      title = "Crimson Pagoda",
      subtitle = "Ride sky elevators & outrun climbing flame spirits",
      mechanicsTag = "Elevators • Flame Spirits • Blaze Chili",
      biome = BiomeStyle.CRIMSON_PAGODA,
      palette = palette,
      startX = 0f,
      startY = 0f,
      bossX = -4.8f,
      bossY = 16.0f,
      goalX = 3.2f,
      goalY = 18.8f,
      barrelSpawnInterval = 2.15f,
      targetTwoStarScore = 2800,
      targetThreeStarScore = 4600,
      parTimeSeconds = 60,
      platforms = listOf(
        PlatformDef(1, -6.8f, 6.8f, 0.0f, 0.0f),
        // Tier 1 outer wings with center elevator gap
        PlatformDef(2, -6.8f, -2.0f, 2.9f, 2.9f),
        PlatformDef(3, 2.0f, 6.8f, 2.9f, 2.9f),
        // Tier 2 sloped center bridge
        PlatformDef(4, -5.0f, 5.0f, 6.2f, 5.6f),
        // Tier 3
        PlatformDef(5, -6.8f, 1.2f, 9.4f, 9.0f),
        PlatformDef(6, 3.0f, 6.8f, 9.2f, 9.2f),
        // Tier 4
        PlatformDef(7, -6.2f, 6.2f, 12.4f, 13.0f),
        // Tier 5 Boss Deck
        PlatformDef(8, -6.8f, 4.4f, 16.0f, 15.8f, isBossDeck = true),
        // Tier 6 Goal
        PlatformDef(9, 0.0f, 5.2f, 18.8f, 18.8f),
      ),
      ladders = listOf(
        LadderDef(1, -5.6f, 0.0f, 2.9f),
        LadderDef(2, 5.6f, 0.0f, 2.9f),
        LadderDef(3, -3.6f, 2.9f, 6.1f),
        LadderDef(4, 3.6f, 2.9f, 5.68f),
        LadderDef(5, -4.2f, 6.15f, 9.26f),
        LadderDef(6, 4.8f, 9.2f, 12.93f),
        LadderDef(7, -2.4f, 12.58f, 15.92f),
        LadderDef(8, 1.4f, 15.85f, 18.8f),
      ),
      movingPlatforms = listOf(
        MovingPlatformDef(1, 0f, 3.6f, width = 2.6f, moveRangeX = 0f, moveRangeY = 1.3f, speed = 1.5f),
        MovingPlatformDef(2, 2.0f, 9.1f, width = 2.0f, moveRangeX = 1.1f, moveRangeY = 0f, speed = 1.8f, phase = 1.2f),
      ),
      springs = listOf(
        SpringDef(1, 5.4f, 9.2f, 17.5f),
      ),
      collectibles = listOf(
        CollectibleDef(1, 0f, 5.6f),
        CollectibleDef(2, 2.0f, 10.6f),
        CollectibleDef(3, -5.2f, 13.5f),
      ),
      powerUps = listOf(
        PowerUpSpawnDef(1, -3.5f, 3.6f, PowerUpType.SPEED_CHILI),
        PowerUpSpawnDef(2, 0.0f, 6.6f, PowerUpType.HAMMER),
        PowerUpSpawnDef(3, 1.8f, 13.4f, PowerUpType.DOUBLE_JUMP),
      ),
      initialFlames = listOf(3.8f to 9.4f, -3.5f to 12.7f),
    )
  }

  private fun buildLevel4(): LevelDefinition {
    // Aurora Sky-Bridge Observatory - Crumbling bamboo bridges & double-jump shortcuts
    val palette = BiomePalette(
      skyTop = Vec3Color(0.06f, 0.09f, 0.24f),
      skyBottom = Vec3Color(0.24f, 0.56f, 0.72f),
      girderMain = Vec3Color(0.32f, 0.44f, 0.88f),
      girderTrim = Vec3Color(0.35f, 0.96f, 1.0f),
      pillarColor = Vec3Color(0.22f, 0.20f, 0.44f),
      fogColor = Vec3Color(0.14f, 0.25f, 0.42f),
      accentGlow = Vec3Color(0.65f, 0.52f, 1.0f),
    )
    return LevelDefinition(
      id = 4,
      title = "Aurora Sky-Bridge",
      subtitle = "Sprint across crumbling bamboo spans high in the clouds",
      mechanicsTag = "Crumble Bridges • Spike Barrels • Plume",
      biome = BiomeStyle.AURORA_SKYBRIDGE,
      palette = palette,
      startX = -5.8f,
      startY = 0f,
      bossX = 0f,
      bossY = 16.5f,
      goalX = 0f,
      goalY = 19.6f,
      barrelSpawnInterval = 1.95f,
      targetTwoStarScore = 3200,
      targetThreeStarScore = 5200,
      parTimeSeconds = 65,
      platforms = listOf(
        PlatformDef(1, -6.8f, -2.0f, 0.0f, 0.0f),
        PlatformDef(2, 2.0f, 6.8f, 0.0f, 0.0f),
        // Tier 1
        PlatformDef(3, -6.8f, -1.5f, 3.0f, 3.0f),
        PlatformDef(4, 1.5f, 6.8f, 3.0f, 3.0f),
        // Tier 2
        PlatformDef(5, -5.6f, 5.6f, 6.2f, 6.6f, conveyorSpeed = 1.3f),
        // Tier 3
        PlatformDef(6, -6.8f, -1.8f, 9.6f, 9.6f),
        PlatformDef(7, 1.8f, 6.8f, 9.6f, 9.6f),
        // Tier 4
        PlatformDef(8, -6.0f, 6.0f, 13.0f, 12.6f),
        // Tier 5 Boss Deck
        PlatformDef(9, -5.8f, 5.8f, 16.5f, 16.5f, isBossDeck = true),
        // Tier 6 Goal Summit
        PlatformDef(10, -2.4f, 2.4f, 19.6f, 19.6f),
      ),
      ladders = listOf(
        LadderDef(1, 5.4f, 0.0f, 3.0f),
        LadderDef(2, -5.4f, 0.0f, 3.0f),
        LadderDef(3, -3.2f, 3.0f, 6.29f),
        LadderDef(4, 3.8f, 6.53f, 9.6f),
        LadderDef(5, -4.8f, 9.6f, 12.92f),
        LadderDef(6, 4.4f, 12.71f, 16.5f),
        LadderDef(7, -4.4f, 12.89f, 16.5f),
        LadderDef(8, -1.8f, 16.5f, 19.6f),
      ),
      crumbleBridges = listOf(
        CrumbleBridgeDef(1, -2.0f, 2.0f, 0.0f),
        CrumbleBridgeDef(2, -1.5f, 1.5f, 3.0f),
        CrumbleBridgeDef(3, -1.8f, 1.8f, 9.6f),
      ),
      springs = listOf(
        SpringDef(1, 5.6f, 3.0f, 18.2f),
        SpringDef(2, 0.0f, 9.6f, 17.6f),
      ),
      collectibles = listOf(
        CollectibleDef(1, 0.0f, 1.4f),
        CollectibleDef(2, 0.0f, 11.2f),
        CollectibleDef(3, -5.2f, 14.2f),
      ),
      powerUps = listOf(
        PowerUpSpawnDef(1, -4.0f, 3.7f, PowerUpType.DOUBLE_JUMP),
        PowerUpSpawnDef(2, 1.0f, 7.1f, PowerUpType.SHIELD),
        PowerUpSpawnDef(3, 0.0f, 13.5f, PowerUpType.HAMMER),
      ),
      initialFlames = listOf(0.0f to 6.5f, 3.2f to 12.8f),
    )
  }

  private fun buildLevel5(): LevelDefinition {
    // Mecha-Kong's Steel Summit - Stomp 3 Golden Rivets & defeat the summit!
    val palette = BiomePalette(
      skyTop = Vec3Color(0.14f, 0.06f, 0.05f),
      skyBottom = Vec3Color(0.82f, 0.34f, 0.12f),
      girderMain = Vec3Color(0.28f, 0.30f, 0.36f),
      girderTrim = Vec3Color(1.0f, 0.78f, 0.18f),
      pillarColor = Vec3Color(0.42f, 0.14f, 0.10f),
      fogColor = Vec3Color(0.36f, 0.14f, 0.09f),
      accentGlow = Vec3Color(1.0f, 0.72f, 0.16f),
    )
    return LevelDefinition(
      id = 5,
      title = "Iron Summit Finale",
      subtitle = "Stomp all 3 Golden Rivets & rescue Red Panda!",
      mechanicsTag = "3 Golden Rivets • All Hazards • Boss",
      biome = BiomeStyle.VOLCANIC_SUMMIT,
      palette = palette,
      startX = -5.6f,
      startY = 0f,
      bossX = 0f,
      bossY = 17.2f,
      goalX = 4.2f,
      goalY = 20.2f,
      barrelSpawnInterval = 1.80f,
      targetTwoStarScore = 4000,
      targetThreeStarScore = 6500,
      parTimeSeconds = 75,
      platforms = listOf(
        PlatformDef(1, -6.8f, 6.8f, 0.0f, 0.5f),
        // Tier 1
        PlatformDef(2, -6.8f, -1.0f, 3.4f, 2.9f, conveyorSpeed = -1.3f),
        PlatformDef(3, 1.4f, 6.8f, 2.9f, 3.4f),
        // Tier 2
        PlatformDef(4, -5.8f, 5.8f, 6.4f, 6.9f),
        // Tier 3
        PlatformDef(5, -6.8f, -1.5f, 10.2f, 9.7f),
        PlatformDef(6, 1.5f, 6.8f, 9.7f, 10.2f, conveyorSpeed = 1.4f),
        // Tier 4
        PlatformDef(7, -6.2f, 6.2f, 13.6f, 13.6f),
        // Tier 5 Boss Deck
        PlatformDef(8, -6.0f, 6.0f, 17.2f, 17.2f, isBossDeck = true),
        // Tier 6 Goal Summit
        PlatformDef(9, 1.5f, 6.2f, 20.2f, 20.2f),
      ),
      ladders = listOf(
        LadderDef(1, 5.2f, 0.44f, 3.27f),
        LadderDef(2, -5.0f, 3.26f, 6.43f),
        LadderDef(3, 4.6f, 6.85f, 10.03f),
        LadderDef(4, -4.6f, 10.02f, 13.6f),
        LadderDef(5, 0.0f, 13.6f, 17.2f),
        LadderDef(6, 2.4f, 17.2f, 20.2f),
      ),
      crumbleBridges = listOf(
        CrumbleBridgeDef(1, -1.5f, 1.5f, 9.7f),
      ),
      movingPlatforms = listOf(
        MovingPlatformDef(1, 0.2f, 3.2f, width = 2.0f, moveRangeX = 0f, moveRangeY = 1.4f, speed = 1.6f),
      ),
      springs = listOf(
        SpringDef(1, -5.5f, 3.3f, 18.0f),
        SpringDef(2, 5.2f, 13.6f, 18.4f),
      ),
      rivets = listOf(
        RivetSwitchDef(1, -4.8f, 6.44f),
        RivetSwitchDef(2, 4.8f, 10.05f),
        RivetSwitchDef(3, -4.8f, 17.2f),
      ),
      collectibles = listOf(
        CollectibleDef(1, 0.2f, 5.3f),
        CollectibleDef(2, 0.0f, 11.4f),
        CollectibleDef(3, -5.2f, 14.8f),
      ),
      powerUps = listOf(
        PowerUpSpawnDef(1, 3.5f, 3.7f, PowerUpType.HAMMER),
        PowerUpSpawnDef(2, -1.5f, 7.1f, PowerUpType.SHIELD),
        PowerUpSpawnDef(3, 0.0f, 14.3f, PowerUpType.DOUBLE_JUMP),
        PowerUpSpawnDef(4, 3.8f, 17.9f, PowerUpType.SPEED_CHILI),
      ),
      initialFlames = listOf(-2.0f to 6.7f, 2.5f to 13.7f),
    )
  }
}
