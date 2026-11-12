package game.gameplay.shared

val BEACH_TILES: List<String> = listOf(
    "beach_ball", "beach_hat", "camera", "coconut", "crab",
    "ice_cream", "sandal", "seagull", "seashell", "summer_shirt",
    "summer_shorts", "sunglasses", "swim_ring", "tropical_fish", "tropical_juice"
)

val BASE_THEMES: List<String> = listOf("candy", "ocean", "forest", "space")

val PREMIUM_THEMES: List<String> = listOf(
    "sakura", "bakery", "galaxy", "meadow", "fruit",
    "autumn_harvest", "magic_atelier", "retro_arcade", "alpine_winter"
)

val REWARD_THEMES: List<String> = listOf("sakura", "bakery", "galaxy", "meadow", "fruit")
    .map { "reward_$it" }

val TILE_SETS: Map<String, List<String>> = run {
    val map = mutableMapOf<String, List<String>>()
    for (id in BASE_THEMES + PREMIUM_THEMES) {
        map[id] = List(16) { i -> "${id}_$i" }
    }
    for (id in REWARD_THEMES) {
        map[id] = List(10) { i -> "${id}_$i" }
    }
    map["sweets"] = BEACH_TILES
    map["vanilla"] = BEACH_TILES
    map
}

fun tileIdsFor(theme: String?): List<String> {
    val set = if (theme != null) TILE_SETS[theme] else null
    return (set ?: BEACH_TILES).toList()
}

fun tileAssetFor(id: String): String? {
    if (id in BEACH_TILES) {
        return "assets/tiles/beach/$id.png"
    }
    for (theme in BASE_THEMES + PREMIUM_THEMES + REWARD_THEMES) {
        val list = TILE_SETS[theme] ?: continue
        val index = list.indexOf(id)
        if (index >= 0) {
            val ext = if (theme in BASE_THEMES) "png" else "webp"
            return "assets/tiles/$theme/$index.$ext"
        }
    }
    return null
}
