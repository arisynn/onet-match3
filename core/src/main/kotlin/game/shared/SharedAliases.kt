package game.shared

typealias RandomGenerator = game.gameplay.shared.RandomGenerator
typealias SeededRandom = game.gameplay.shared.SeededRandom

val BEACH_TILES = game.gameplay.shared.BEACH_TILES
val BASE_THEMES = game.gameplay.shared.BASE_THEMES
val PREMIUM_THEMES = game.gameplay.shared.PREMIUM_THEMES
val REWARD_THEMES = game.gameplay.shared.REWARD_THEMES
val TILE_SETS = game.gameplay.shared.TILE_SETS

fun tileIdsFor(theme: String?): List<String> = game.gameplay.shared.tileIdsFor(theme)
fun tileAssetFor(id: String): String? = game.gameplay.shared.tileAssetFor(id)
fun <T> shuffleList(items: List<T>, rng: RandomGenerator): List<T> = game.gameplay.shared.shuffleList(items, rng)
