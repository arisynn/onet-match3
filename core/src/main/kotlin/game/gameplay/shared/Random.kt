package game.gameplay.shared

/**
 * Functional interface for deterministic or pseudo-random number generation.
 */
fun interface RandomGenerator {
    fun nextDouble(): Double
}

/**
 * Seeded deterministic PRNG implementing FNV-1a 32-bit seed hashing
 * and SplitMix32 32-bit generator. Exact byte/bit parity with JavaScript implementation.
 */
class SeededRandom(seed: Any) : RandomGenerator {
    private var state: Int = 0x811C9DC5.toInt() // 2166136261

    init {
        val str = seed.toString()
        for (char in str) {
            state = (state xor char.code) * 16777619
        }
    }

    override fun nextDouble(): Double {
        state += 0x6D2B79F5.toInt()
        var t = (state xor (state ushr 15)) * (state or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        val unsignedVal = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return unsignedVal.toDouble() / 4294967296.0
    }
}

/**
 * Fisher-Yates shuffle matching JavaScript array shuffle order:
 * for (let i = out.length - 1; i > 0; i--) { const j = Math.floor(rng() * (i+1)); [out[i], out[j]] = [out[j], out[i]]; }
 */
fun <T> shuffleList(items: List<T>, rng: RandomGenerator): List<T> {
    val out = items.toMutableList()
    for (i in out.size - 1 downTo 1) {
        val j = Math.floor(rng.nextDouble() * (i + 1)).toInt()
        val tmp = out[i]
        out[i] = out[j]
        out[j] = tmp
    }
    return out
}

fun <T> MutableList<T>.shuffleInPlace(rng: RandomGenerator) {
    for (i in size - 1 downTo 1) {
        val j = Math.floor(rng.nextDouble() * (i + 1)).toInt()
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}
