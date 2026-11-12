package game.gameplay.triplematch

import game.gameplay.shared.RandomGenerator
import game.gameplay.shared.shuffleList

/** Level tuning for the new campaign; the original createStacks API stays unchanged. */
data class TripleDifficulty(
    val level: Int,
    val stackCount: Int = 12,
    val stackDepth: Int,
    val tileTypes: Int,
    val seconds: Int,
    val tutorial: Boolean
)

fun getTripleDifficulty(level: Int): TripleDifficulty {
    require(level >= 1) { "Level must be positive." }
    val depth = when {
        level == 1 -> 1
        level < 4 -> 2
        level < 10 -> 3
        level < 25 -> 4
        level < 50 -> 5
        else -> 6
    }
    val types = when {
        level == 1 -> 4
        level < 4 -> 5
        level < 10 -> 6
        level < 25 -> 8
        level < 50 -> 10
        else -> 12
    }
    // More tiles get more total time, but less time per tile as the campaign grows.
    val seconds = depth * 35 + (if (level <= 3) 70 else 50)
    return TripleDifficulty(level, stackDepth = depth, tileTypes = types, seconds = seconds, tutorial = level <= 3)
}

/**
 * A generated campaign level and a constructive winning sequence of stack indices.
 * The solution is QA metadata, not stored in the gameplay state or rewarded as play.
 */
data class GeneratedTripleLevel(
    val stacks: List<List<String>>,
    val solution: List<Int>,
    val difficulty: TripleDifficulty
)

/**
 * Constructs the board backwards from a known winning removal order. Every layer
 * visits each stack once in shuffled order, partitioned into groups of three with
 * the same ID. Per-stack queues are then reversed to match pop-from-end semantics.
 * Following solution therefore clears one trio at a time, never filling the tray.
 * Random wrong choices can still lose: this guarantees solvability, not immunity.
 * Runtime is linear in tile count and does not depend on retry-until-solvable loops.
 */
fun generateTripleLevel(
    tileIds: List<String>,
    level: Int,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): GeneratedTripleLevel {
    require(tileIds.all { it.isNotBlank() } && tileIds.distinct().size == tileIds.size) {
        "Campaign tile IDs must be distinct and nonblank."
    }
    require(tileIds.size >= 6) { "Triple Match needs at least six distinct tile IDs." }
    val requested = getTripleDifficulty(level)
    val difficulty = requested.copy(tileTypes = minOf(requested.tileTypes, tileIds.size))
    val pool = shuffleList(tileIds, rng).take(difficulty.tileTypes)
    val groups = difficulty.stackDepth * 4
    val groupIds = mutableListOf<String>()
    while (groupIds.size < groups) groupIds.addAll(shuffleList(pool, rng))
    val removals = List(difficulty.stackCount) { mutableListOf<String>() }
    val solution = ArrayList<Int>(difficulty.stackCount * difficulty.stackDepth)
    var group = 0
    repeat(difficulty.stackDepth) {
        val order = shuffleList((0 until difficulty.stackCount).toList(), rng)
        order.chunked(3).forEach { triplet ->
            val id = groupIds[group++]
            triplet.forEach { stack ->
                removals[stack].add(id)
                solution.add(stack)
            }
        }
    }
    return GeneratedTripleLevel(removals.map { it.asReversed().toList() }, solution.toList(), difficulty)
}

fun createTripleLevelRound(
    tileIds: List<String>,
    level: Int,
    rng: RandomGenerator = RandomGenerator { Math.random() }
): TripleRoundState {
    val generated = generateTripleLevel(tileIds, level, rng)
    return createTripleRound(tileIds, seconds = generated.difficulty.seconds, stacks = generated.stacks)
}
