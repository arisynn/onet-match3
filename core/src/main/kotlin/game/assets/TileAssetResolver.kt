package game.assets

/**
 * Resolves tile IDs to filesystem/asset paths and atlas region names.
 * Replicates the logic from src/shared/tiles.js.
 */
object TileAssetResolver {

    val BEACH_TILES = listOf(
        "beach_ball", "beach_hat", "camera", "coconut", "crab",
        "ice_cream", "sandal", "seagull", "seashell", "summer_shirt",
        "summer_shorts", "sunglasses", "swim_ring", "tropical_fish", "tropical_juice"
    )

    val BASE_THEMES = listOf("candy", "ocean", "forest", "space")

    val PREMIUM_THEMES = listOf(
        "sakura", "bakery", "galaxy", "meadow", "fruit",
        "autumn_harvest", "magic_atelier", "retro_arcade", "alpine_winter"
    )

    val REWARD_THEMES = listOf("sakura", "bakery", "galaxy", "meadow", "fruit").map { "reward_$it" }

    val TILE_SETS: Map<String, List<String>> = buildMap {
        for (theme in BASE_THEMES + PREMIUM_THEMES) {
            put(theme, (0 until 16).map { "${theme}_$it" })
        }
        for (theme in REWARD_THEMES) {
            put(theme, (0 until 10).map { "${theme}_$it" })
        }
        put("sweets", BEACH_TILES)
        put("vanilla", BEACH_TILES)
    }

    /**
     * Retrieve all tile IDs for a given theme key.
     */
    fun tileIdsFor(theme: String): List<String> {
        return TILE_SETS[theme] ?: BEACH_TILES
    }

    /**
     * Resolve asset relative path for a given tile ID.
     */
    fun tileAssetFor(id: String): String? {
        if (BEACH_TILES.contains(id)) {
            return "tiles/beach/$id.png"
        }

        val allThemes = BASE_THEMES + PREMIUM_THEMES + REWARD_THEMES
        for (theme in allThemes) {
            val list = TILE_SETS[theme] ?: continue
            val index = list.indexOf(id)
            if (index >= 0) {
                val ext = if (BASE_THEMES.contains(theme)) "png" else "webp"
                return "tiles/$theme/$index.$ext"
            }
        }
        return null
    }
}
